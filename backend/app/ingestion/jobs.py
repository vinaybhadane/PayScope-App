"""Bounded OCR task queue isolated from the ASGI event loop and shared thread pool.

Jobs are ephemeral: clients retry uploads after a process restart. Run one API
worker per instance for this local queue, or replace it with a shared broker.
"""
from concurrent.futures import ThreadPoolExecutor
from contextvars import copy_context
from dataclasses import dataclass
import logging
import threading
import time
import uuid
from fastapi import HTTPException

logger = logging.getLogger(__name__)


@dataclass
class ImportJob:
    owner: uuid.UUID
    status: str = "queued"
    result: dict | None = None
    error: str | None = None
    finished_at: float | None = None


class ImportJobQueue:
    def __init__(self, workers=1, capacity=4, retained_results=128):
        self.executor = ThreadPoolExecutor(max_workers=workers, thread_name_prefix="payscope-ocr")
        self.slots = threading.BoundedSemaphore(capacity)
        self.lock = threading.Lock()
        self.jobs = {}
        self.retained_results = retained_results

    def submit(self, owner, operation):
        if not self.slots.acquire(blocking=False):
            raise HTTPException(429, "OCR queue is full. Retry after current uploads finish.", headers={"Retry-After": "30"})
        job_id = str(uuid.uuid4())
        job = ImportJob(owner)
        with self.lock:
            completed = [(key, value.finished_at) for key, value in self.jobs.items() if value.finished_at is not None]
            for key, finished in sorted(completed, key=lambda item: item[1]):
                if time.monotonic() - finished > 900 or len(self.jobs) >= self.retained_results:
                    self.jobs.pop(key, None)
            self.jobs[job_id] = job

        def run():
            try:
                with self.lock:
                    job.status = "running"
                result = operation()
                with self.lock:
                    job.result = result
                    job.status = "completed"
            except Exception as exc:
                logger.warning("Background import job failed: job=%s type=%s", job_id, type(exc).__name__)
                with self.lock:
                    job.error = str(exc) if isinstance(exc, ValueError) else "The import could not be processed. Please retry."
                    job.status = "failed"
            finally:
                with self.lock:
                    job.finished_at = time.monotonic()
                self.slots.release()

        try:
            self.executor.submit(copy_context().run, run)
        except Exception:
            with self.lock:
                self.jobs.pop(job_id, None)
            self.slots.release()
            raise
        return {"job_id": job_id, "status": "queued"}

    def get(self, job_id, owner):
        with self.lock:
            job = self.jobs.get(str(job_id))
            if job is None or job.owner != owner or (job.finished_at is not None and time.monotonic() - job.finished_at > 900):
                raise HTTPException(404, "Import job not found or expired.")
            return {"job_id": str(job_id), "status": job.status, "result": job.result, "error": job.error}


import_jobs = ImportJobQueue()
