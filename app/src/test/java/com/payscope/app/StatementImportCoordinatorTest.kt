package com.payscope.app

import com.payscope.app.domain.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class StatementImportCoordinatorTest {
    @Test fun waitsForDatabaseConfirmationAndFreshDashboard() = runBlocking {
        val saved = CompletableDeferred<Unit>()
        val fresh = CompletableDeferred<Unit>()
        val stages = mutableListOf<UploadStage>()
        val job = launch {
            completeStatementImport(upload = { "import-123" },
                confirmSaved = { id -> assertEquals("import-123", id); saved.await() },
                refreshDashboard = { fresh.await() }, onStage = { stages.add(it) })
        }
        yield()
        assertFalse(job.isCompleted)
        assertFalse(stages.contains(UploadStage.COMPLETED))
        saved.complete(Unit)
        yield()
        assertFalse(job.isCompleted)
        assertFalse(stages.contains(UploadStage.COMPLETED))
        fresh.complete(Unit)
        job.join()
        assertEquals(UploadStage.COMPLETED, stages.last())
    }

    @Test fun failedPersistenceNeverNavigatesToSuccess() = runBlocking {
        val stages = mutableListOf<UploadStage>()
        var refreshed = false
        val result = runCatching { completeStatementImport(upload = { "import-123" },
            confirmSaved = { error("not committed") }, refreshDashboard = { refreshed = true },
            onStage = { stages.add(it) }) }
        assertTrue(result.isFailure)
        assertFalse(refreshed)
        assertFalse(stages.contains(UploadStage.COMPLETED))
    }

    @Test fun failedDashboardReadNeverNavigatesToSuccess() = runBlocking {
        val stages = mutableListOf<UploadStage>()
        val result = runCatching { completeStatementImport(upload = { "import-123" },
            confirmSaved = {}, refreshDashboard = { error("connection lost") }, onStage = { stages.add(it) }) }
        assertTrue(result.isFailure)
        assertFalse(stages.contains(UploadStage.COMPLETED))
    }
}
