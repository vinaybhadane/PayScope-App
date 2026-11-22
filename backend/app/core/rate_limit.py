"""IP keyed limits. Use shared Redis storage when running multiple workers/hosts."""
from slowapi import Limiter
from slowapi.util import get_remote_address
from limits import parse
from starlette.requests import Request
from starlette.responses import JSONResponse
from starlette.concurrency import run_in_threadpool
from app.core.config import settings

limiter = Limiter(key_func=get_remote_address, storage_uri=settings.RATE_LIMIT_STORAGE_URI)


class UploadRateLimitMiddleware:
    """Enforce limits before multipart parsing, including lazily included routers.

    The stock SlowAPI route lookup does not traverse FastAPI's included-router
    wrappers. Calling the shared storage-backed limiter here also counts malformed
    uploads, so invalid bodies cannot bypass quotas or fill the upload spool.
    """
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            return await self.app(scope, receive, send)
        request = Request(scope)
        path = scope["path"].rstrip("/")
        prefix = settings.API_V1_STR
        rates = {f"{prefix}/auth/guest": "5/minute", f"{prefix}/imports/preview": "10/minute", f"{prefix}/imports": "10/minute",
                 f"{prefix}/insights/statement-comparison/explain": "5/minute",
                 f"{prefix}/workspace/graph-review": "5/minute"}
        body_exceeded = False
        if scope["method"] == "POST" and path in rates:
            allowed = await run_in_threadpool(limiter.limiter.hit, parse(rates[path]), get_remote_address(request), path)
            if not allowed:
                return await JSONResponse(status_code=429, content={"detail": "Rate limit exceeded. Please retry in one minute."},
                                          headers={"Retry-After": "60", "Cache-Control": "no-store"})(scope, receive, send)
            if path.startswith(f"{prefix}/imports"):
                from app.ingestion.file_validator import MAX_FILE_SIZE_BYTES
                from starlette.formparsers import MultiPartException
                max_body = MAX_FILE_SIZE_BYTES + 64 * 1024  # Bounded multipart overhead.
                try:
                    declared_size = int(request.headers.get("content-length", "0"))
                except ValueError:
                    return await JSONResponse(status_code=400, content={"detail": "Invalid Content-Length."})(scope, receive, send)
                if declared_size < 0 or declared_size > max_body:
                    return await JSONResponse(status_code=413, content={"detail": "File exceeds the 10 MB upload limit."})(scope, receive, send)
                original_receive = receive
                received = 0
                async def limited_receive():
                    nonlocal received, body_exceeded
                    message = await original_receive()
                    received += len(message.get("body", b""))
                    if received > max_body:
                        body_exceeded = True
                        # The multipart parser closes its temporary files for this
                        # exception type. A generic HTTPException bypasses cleanup.
                        raise MultiPartException("File exceeds the 10 MB upload limit.")
                    return message
                receive = limited_receive

        async def private_response(message):
            if message["type"] == "http.response.start" and body_exceeded:
                message["status"] = 413
            if message["type"] == "http.response.start" and path.startswith(prefix) and not path.startswith(f"{prefix}/demo/sample-files"):
                from starlette.datastructures import MutableHeaders
                MutableHeaders(scope=message)["Cache-Control"] = "no-store"
            await send(message)
        await self.app(scope, receive, private_response)
