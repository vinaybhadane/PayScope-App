package com.payscope.app.domain.model

/** Completion means a committed import was verified and its dashboard read succeeded. */
suspend fun completeStatementImport(
    upload: suspend () -> String,
    confirmSaved: suspend (String) -> Unit,
    refreshDashboard: suspend () -> Unit,
    onStage: suspend (UploadStage) -> Unit
) {
    onStage(UploadStage.SAVING)
    val importId = upload()
    onStage(UploadStage.PREPARING)
    confirmSaved(importId)
    refreshDashboard()
    onStage(UploadStage.COMPLETED)
}
