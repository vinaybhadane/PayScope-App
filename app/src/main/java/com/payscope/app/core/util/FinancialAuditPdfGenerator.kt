package com.payscope.app.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.FinancialSummary
import com.payscope.app.domain.model.PriceHikeAlert
import com.payscope.app.domain.model.WorkspaceActivity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object FinancialAuditPdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 points (8.27 x 11.69 inches @ 72 dpi)
    private const val PAGE_HEIGHT = 842

    fun generateAuditPdf(
        context: Context,
        workspaceId: String,
        userName: String,
        userEmail: String,
        summary: FinancialSummary,
        commitments: List<CommitmentItem>,
        activity: WorkspaceActivity,
        priceHikes: List<PriceHikeAlert>
    ): File {
        val document = PdfDocument()

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(20, 30, 45)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val boldPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(255, 85, 0) // PayScope Primary Orange
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val subheaderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val boxPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        val boxBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val inrFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }

        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))
        val maskedWorkspace = if (workspaceId.length > 8) workspaceId.take(8) + "..." else workspaceId
        val maskedEmail = if (userEmail.contains("@")) {
            val parts = userEmail.split("@")
            val namePart = parts[0]
            val maskedName = if (namePart.length > 2) namePart.take(2) + "***" else namePart
            "$maskedName@${parts[1]}"
        } else userEmail

        // --- PAGE 1 ---
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = 40f

        // Top Accent Bar
        val accentPaint = Paint().apply { color = Color.rgb(255, 85, 0) }
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 4f, accentPaint)
        y += 24f

        // Brand Title
        canvas.drawText("PAYSCOPE FINANCIAL INTELLIGENCE", 36f, y, headerPaint)
        y += 15f
        canvas.drawText("Verified Financial Audit & Recurring Payment Intelligence Report", 36f, y, subheaderPaint)
        y += 20f

        // Metadata Box
        val metaRect = RectF(36f, y, PAGE_WIDTH - 36f, y + 44f)
        canvas.drawRoundRect(metaRect, 6f, 6f, boxPaint)
        canvas.drawRoundRect(metaRect, 6f, 6f, boxBorderPaint)

        canvas.drawText("Audit Scope: Workspace ($maskedWorkspace)", 48f, y + 16f, textPaint)
        canvas.drawText("Authorized User: $userName ($maskedEmail)", 48f, y + 32f, textPaint)
        canvas.drawText("Generated: $nowStr", 340f, y + 16f, textPaint)
        canvas.drawText("Currency: INR (Rs.)", 340f, y + 32f, textPaint)
        y += 62f

        // Section 1: Executive Summary
        canvas.drawText("1. EXECUTIVE FINANCIAL COMMITMENT SUMMARY", 36f, y, boldPaint)
        y += 12f

        // 3 Metric Cards
        val cardWidth = (PAGE_WIDTH - 72f - 20f) / 3f
        val cardHeight = 60f

        // Card 1: Monthly Spend
        var cx = 36f
        canvas.drawRoundRect(RectF(cx, y, cx + cardWidth, y + cardHeight), 6f, 6f, boxPaint)
        canvas.drawRoundRect(RectF(cx, y, cx + cardWidth, y + cardHeight), 6f, 6f, boxBorderPaint)
        canvas.drawText("Monthly Recurring", cx + 12f, y + 20f, subheaderPaint)
        val monthlyStr = inrFormatter.format(summary.monthlyRecurringSpend.toLong())
        canvas.drawText(monthlyStr, cx + 12f, y + 42f, boldPaint)

        // Card 2: Annual Spend
        cx += cardWidth + 10f
        canvas.drawRoundRect(RectF(cx, y, cx + cardWidth, y + cardHeight), 6f, 6f, boxPaint)
        canvas.drawRoundRect(RectF(cx, y, cx + cardWidth, y + cardHeight), 6f, 6f, boxBorderPaint)
        canvas.drawText("Annualized Commitment", cx + 12f, y + 20f, subheaderPaint)
        val annualStr = inrFormatter.format(summary.annualizedSpend.toLong())
        canvas.drawText(annualStr, cx + 12f, y + 42f, boldPaint)

        // Card 3: Potential Savings
        cx += cardWidth + 10f
        canvas.drawRoundRect(RectF(cx, y, cx + cardWidth, y + cardHeight), 6f, 6f, boxPaint)
        canvas.drawRoundRect(RectF(cx, y, cx + cardWidth, y + cardHeight), 6f, 6f, boxBorderPaint)
        canvas.drawText("Potential Annual Savings", cx + 12f, y + 20f, subheaderPaint)
        val savingsStr = inrFormatter.format(summary.potentialSavingsAnnual.toLong())
        val savingsPaint = Paint(boldPaint).apply { color = Color.rgb(13, 148, 136) } // Teal
        canvas.drawText(savingsStr, cx + 12f, y + 42f, savingsPaint)

        y += cardHeight + 24f

        // Section 2: Statement Activity
        canvas.drawText("2. STATEMENT INGESTION & CASH FLOW OVERVIEW", 36f, y, boldPaint)
        y += 12f

        val stmtRect = RectF(36f, y, PAGE_WIDTH - 36f, y + 48f)
        canvas.drawRoundRect(stmtRect, 6f, 6f, boxPaint)
        canvas.drawRoundRect(stmtRect, 6f, 6f, boxBorderPaint)

        val totalDebits = activity.totals.firstOrNull()?.debits ?: 0.0
        val totalCredits = activity.totals.firstOrNull()?.credits ?: 0.0

        canvas.drawText("Processed Statements: ${activity.statementCount}", 48f, y + 18f, textPaint)
        canvas.drawText("Saved Transactions: ${activity.transactionCount}", 48f, y + 34f, textPaint)
        canvas.drawText("Total Money Out (Debits): ${inrFormatter.format(totalDebits.toLong())}", 260f, y + 18f, textPaint)
        canvas.drawText("Total Money In (Credits): ${inrFormatter.format(totalCredits.toLong())}", 260f, y + 34f, textPaint)

        y += 68f

        // Section 3: Subscriptions Table
        canvas.drawText("3. DETECTED RECURRING COMMITMENTS & SUBSCRIPTIONS (${commitments.size})", 36f, y, boldPaint)
        y += 14f

        // Table Header
        val colService = 36f
        val colCadence = 230f
        val colAmount = 310f
        val colRail = 400f
        val colStatus = 490f

        val thPaint = Paint().apply {
            color = Color.rgb(230, 235, 245)
            style = Paint.Style.FILL
        }
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 18f, thPaint)
        canvas.drawText("SERVICE / MERCHANT", colService + 6f, y + 12f, subheaderPaint)
        canvas.drawText("CADENCE", colCadence, y + 12f, subheaderPaint)
        canvas.drawText("AMOUNT", colAmount, y + 12f, subheaderPaint)
        canvas.drawText("PAYMENT RAIL", colRail, y + 12f, subheaderPaint)
        canvas.drawText("STATUS", colStatus, y + 12f, subheaderPaint)
        y += 20f

        val rowPaint = Paint().apply {
            color = Color.rgb(250, 252, 255)
            style = Paint.Style.FILL
        }

        // Draw commitments rows
        var rowCount = 0
        for (item in commitments) {
            if (y > PAGE_HEIGHT - 60f) {
                // Page overflow: finish page and start new page
                canvas.drawText("PayScope Financial Audit • Page $pageNumber • Confidential", 36f, PAGE_HEIGHT - 24f, subheaderPaint)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f

                // Repeat table header on new page
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 18f, thPaint)
                canvas.drawText("SERVICE / MERCHANT", colService + 6f, y + 12f, subheaderPaint)
                canvas.drawText("CADENCE", colCadence, y + 12f, subheaderPaint)
                canvas.drawText("AMOUNT", colAmount, y + 12f, subheaderPaint)
                canvas.drawText("PAYMENT RAIL", colRail, y + 12f, subheaderPaint)
                canvas.drawText("STATUS", colStatus, y + 12f, subheaderPaint)
                y += 22f
            }

            if (rowCount % 2 == 1) {
                canvas.drawRect(36f, y - 2f, PAGE_WIDTH - 36f, y + 14f, rowPaint)
            }

            val serviceName = if (item.name.length > 28) item.name.take(28) + ".." else item.name
            canvas.drawText(serviceName, colService + 6f, y + 10f, textPaint)
            canvas.drawText(item.cadence, colCadence, y + 10f, textPaint)
            canvas.drawText(inrFormatter.format(item.amount.toLong()), colAmount, y + 10f, textPaint)
            canvas.drawText(item.paymentRail.take(14), colRail, y + 10f, textPaint)
            canvas.drawText(item.status, colStatus, y + 10f, textPaint)

            y += 16f
            rowCount++
        }

        // Section 4: Price Hikes (if any)
        if (priceHikes.isNotEmpty()) {
            y += 16f
            if (y > PAGE_HEIGHT - 100f) {
                canvas.drawText("PayScope Financial Audit • Page $pageNumber • Confidential", 36f, PAGE_HEIGHT - 24f, subheaderPaint)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            canvas.drawText("4. PRICE HIKE & ANOMALY ALERTS (${priceHikes.size})", 36f, y, boldPaint)
            y += 14f

            for (hike in priceHikes) {
                val hikeText = "${hike.merchantName}: ${inrFormatter.format(hike.oldAmount.toLong())} -> ${inrFormatter.format(hike.newAmount.toLong())} (+${hike.percentageChange.toInt()}%) | Extra Annualized: ${inrFormatter.format(hike.annualizedImpact.toLong())}"
                canvas.drawText("• $hikeText", 48f, y + 10f, textPaint)
                y += 16f
            }
        }

        // Compliance & Methodology Footer
        y += 20f
        if (y <= PAGE_HEIGHT - 60f) {
            val notePaint = Paint(subheaderPaint).apply { textSize = 8f }
            canvas.drawText("Audit Methodology: Deduplicated extraction from validated statements with multi-cycle payment confirmation.", 36f, y, notePaint)
            canvas.drawText("Tenant workspace data is strictly isolated. Document generated locally on-device.", 36f, y + 11f, notePaint)
        }

        // Footer on final page
        canvas.drawText("PayScope Financial Audit • Page $pageNumber • Confidential", 36f, PAGE_HEIGHT - 24f, subheaderPaint)
        document.finishPage(page)

        // Save PDF to Documents/Downloads folder
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "payscope-financial-audit-${System.currentTimeMillis()}.pdf")
        val outStream = FileOutputStream(outputFile)
        document.writeTo(outStream)
        outStream.flush()
        outStream.close()
        document.close()

        return outputFile
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Financial Audit PDF"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Audit PDF saved to: ${file.name}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}
