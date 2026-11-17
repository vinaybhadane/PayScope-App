package com.payscope.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.payscope.app.data.api.*
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class SelectedFile(val uri: Uri, val name: String, val sizeBytes: Long, val extension: String)

@Singleton
class StatementRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: PayScopeApiService,
    private val dataStoreManager: DataStoreManager,
    private val dashboardRepository: DashboardRepository
) {
    fun validateFile(uri: Uri): Result<SelectedFile> {
        val contentResolver = context.contentResolver
        var name = "statement"
        var size: Long = 0

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: "statement"
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        val extension = name.substringAfterLast('.', "").lowercase()
        val allowedExtensions = setOf("pdf", "csv", "xlsx", "json")

        if (extension !in allowedExtensions) {
            return Result.failure(
                IllegalArgumentException("Unsupported format (.$extension). PayScope supports PDF, CSV, XLSX, and JSON statements.")
            )
        }

        val maxSizeBytes = 10 * 1024 * 1024 // 10 MB
        if (size > maxSizeBytes) {
            return Result.failure(
                IllegalArgumentException("File size exceeds 10 MB limit (${size / (1024 * 1024)} MB).")
            )
        }

        return Result.success(
            SelectedFile(
                uri = uri,
                name = name,
                sizeBytes = size,
                extension = extension
            )
        )
    }


    private suspend fun auth(): String = "Bearer ${dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")}"

    private suspend fun part(file: SelectedFile): MultipartBody.Part = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(file.uri)?.use { input ->
            val buffer = ByteArray(8192)
            val output = java.io.ByteArrayOutputStream()
            while (output.size() <= 10 * 1024 * 1024) {
                val count = input.read(buffer)
                if (count == -1) break
                output.write(buffer, 0, count)
            }
            output.toByteArray() }
            ?: error("Cannot open this file. Select it again.")
        require(bytes.isNotEmpty()) { "The selected file is empty." }
        require(bytes.size <= 10 * 1024 * 1024) { "The statement exceeds the 10 MB limit." }
        val mime = when (file.extension) {
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/json"
        }
        MultipartBody.Part.createFormData("file", file.name, bytes.toRequestBody(mime.toMediaType()))
    }
    private fun confirmed() = "true".toRequestBody("text/plain".toMediaType())

    suspend fun getStatementPreview(file: SelectedFile): StatementPreviewData {
        val b = apiService.previewStatement(auth(), part(file), confirmed()).requireBody()
        require(b.invalidCount == 0) { "${b.invalidCount} statement rows could not be read reliably. Correct the source file or export CSV/XLSX and retry." }
        require(b.previewRows.isNotEmpty()) { "No valid transactions were found in this statement." }
        return StatementPreviewData(b.fileInfo?.filename ?: file.name,
            (b.fileInfo?.fileType ?: file.extension).uppercase(), b.totalRows, b.debitCount, b.totalDebits,
            b.creditCount, b.totalCredits, b.previewRows.map { row ->
                val desc = (row["descriptor"] ?: row["description"]).toString()
                TransactionPreviewItem(row["date"].toString(), desc,
                    (row["amount"] as? Number)?.toDouble() ?: 0.0,
                    row["direction"].toString().equals("debit", true),
                    row["suggested_merchant"]?.toString() ?: desc, row["currency"]?.toString() ?: "INR")
            }, currency = b.currencies.singleOrNull() ?: "Mixed")
    }

    fun processStatement(file: SelectedFile): Flow<UploadStage> = flow {
        emit(UploadStage.READING)
        val filePart = part(file)
        val token = auth()
        completeStatementImport(
            upload = {
                val result = apiService.uploadStatement(token, filePart, confirmed()).requireBody()
                check(result.status != "failed") { result.errorSummary ?: "No transactions could be imported. Review your file and retry." }
                result.importId ?: error("The server did not return a saved import ID.")
            },
            confirmSaved = { id ->
                val saved = apiService.getImportStatus(token, id).requireBody()
                check(saved.id == id && saved.status in listOf("completed", "partially_completed") &&
                    saved.acceptedCount + saved.duplicateCount > 0) {
                    saved.error ?: "The import has not finished saving. Please retry."
                }
            },
            refreshDashboard = {
                val snapshot = dashboardRepository.refreshStrict()
                check(snapshot.importedTransactionsCount > 0) { "Your statement is saved, but its transactions could not be loaded. Please retry the refresh." }
                dataStoreManager.setHasUploadedStatements(true)
            },
            onStage = { emit(it) }
        )
    }

    fun processSampleStatement(): Flow<UploadStage> = flow {
        emit(UploadStage.READING)
        // Samples always live in a separate signed guest workspace.
        val guest = apiService.createGuestSession().requireBody()
        val token = guest.accessToken ?: error("The server did not create a sample session.")
        emit(UploadStage.DETECTING)
        apiService.seedDemo("Bearer $token").requireSuccess()
        val workspaceId = apiService.getCurrentUser("Bearer $token").requireBody().id ?: error("Could not load sample workspace.")
        dataStoreManager.saveSession(token, workspaceId, "guest@payscope.demo", "Sample workspace", true, true)
        dashboardRepository.refreshStrict()
        emit(UploadStage.COMPLETED)
    }

    suspend fun retryAnalysis() {
        val result = apiService.retryAnalysis(auth()).requireBody()
        check(result["analysis_status"] == "completed") { result["message"]?.toString() ?: "Gemini analysis is unavailable. Please retry." }
        dashboardRepository.refreshStrict()
    }

    suspend fun getImportHistory(): List<BankStatementItem> = apiService.getImports(auth()).requireBody().items.map {
        BankStatementItem(it.id, it.filename, it.fileType.uppercase(), it.acceptedCount, it.createdAt.take(10))
    }

    suspend fun deleteWorkspaceData(): Result<Unit> = try {
        apiService.deleteWorkspaceData(auth()).requireSuccess()
        dataStoreManager.setHasUploadedStatements(false)
        dashboardRepository.clearUserCommitments()
        Result.success(Unit)
    } catch (e: Exception) { if (e is CancellationException) throw e; Result.failure(e) }
}
