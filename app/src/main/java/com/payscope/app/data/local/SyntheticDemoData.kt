package com.payscope.app.data.local

import com.payscope.app.domain.model.CancellationPlaybook
import com.payscope.app.domain.model.CategorySpend
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.DateRangeFilter
import com.payscope.app.domain.model.FinancialSummary
import com.payscope.app.domain.model.HistoricalDebit
import com.payscope.app.domain.model.MerchantComparison
import com.payscope.app.domain.model.PriceHikeAlert
import com.payscope.app.domain.model.SavedSavingsPlan
import com.payscope.app.domain.model.SpendTrendPoint
import com.payscope.app.domain.model.StatementPreviewData
import com.payscope.app.domain.model.TransactionPreviewItem
import com.payscope.app.domain.model.UpcomingPayment

object SyntheticDemoData {

    fun getSummary(): FinancialSummary = FinancialSummary(
        monthlyRecurringSpend = 12450.0,
        annualizedSpend = 149400.0,
        activeSubscriptionsCount = 14,
        potentialSavingsAnnual = 34200.0,
        priceHikesCount = 2,
        totalHistoricalSpend12M = 142800.0,
        upcomingBillsCount = 3,
        upcomingBillsTotal = 2917.0,
        currencySymbol = "₹"
    )

    fun getCommitments(): List<CommitmentItem> = listOf(
        CommitmentItem(
            id = "demo-sub-1",
            name = "Netflix Premium",
            merchantName = "Netflix India",
            amount = 799.0,
            currency = "INR",
            cadence = "Monthly",
            normalizedMonthlyAmount = 799.0,
            nextBillingDate = "Nov 28, 2026",
            category = "Streaming",
            status = "Confirmed",
            confidenceScore = 98,
            logoKey = "netflix",
            paymentRail = "UPI AutoPay",
            hasPriceHike = true,
            previousAmount = 649.0,
            priceHikePercentage = 23.1,
            reasonExplanation = "Charged ₹799 on 3 consecutive monthly cycles (approx 30 days interval)."
        ),
        CommitmentItem(
            id = "demo-sub-2",
            name = "ChatGPT Plus",
            merchantName = "OpenAI",
            amount = 1999.0,
            currency = "INR",
            cadence = "Monthly",
            normalizedMonthlyAmount = 1999.0,
            nextBillingDate = "Dec 02, 2026",
            category = "AI / Tech",
            status = "Confirmed",
            confidenceScore = 95,
            logoKey = "openai",
            paymentRail = "Credit Card Recurring",
            hasPriceHike = false,
            reasonExplanation = "Regular monthly standing instruction on credit card ending in 4102."
        ),
        CommitmentItem(
            id = "demo-sub-3",
            name = "Spotify Individual",
            merchantName = "Spotify India",
            amount = 119.0,
            currency = "INR",
            cadence = "Monthly",
            normalizedMonthlyAmount = 119.0,
            nextBillingDate = "Dec 05, 2026",
            category = "Music",
            status = "Confirmed",
            confidenceScore = 99,
            logoKey = "spotify",
            paymentRail = "UPI AutoPay",
            hasPriceHike = false,
            reasonExplanation = "Consistent ₹119 recurring mandate via Google Pay UPI AutoPay."
        ),
        CommitmentItem(
            id = "demo-sub-4",
            name = "Amazon Prime Annual",
            merchantName = "Amazon Prime",
            amount = 1499.0,
            currency = "INR",
            cadence = "Yearly",
            normalizedMonthlyAmount = 124.9,
            nextBillingDate = "Jan 15, 2027",
            category = "Shopping",
            status = "Confirmed",
            confidenceScore = 97,
            logoKey = "amazon",
            paymentRail = "Credit Card Recurring",
            hasPriceHike = false,
            reasonExplanation = "Annual recurring subscription charge observed in Jan statement."
        ),
        CommitmentItem(
            id = "demo-sub-5",
            name = "YouTube Premium Family",
            merchantName = "Google YouTube",
            amount = 299.0,
            currency = "INR",
            cadence = "Monthly",
            normalizedMonthlyAmount = 299.0,
            nextBillingDate = "Dec 10, 2026",
            category = "Streaming",
            status = "Confirmed",
            confidenceScore = 96,
            logoKey = "youtube",
            paymentRail = "Google Play Store",
            hasPriceHike = true,
            previousAmount = 189.0,
            priceHikePercentage = 58.2,
            reasonExplanation = "Family plan rate changed from ₹189 to ₹299 across recent cycles."
        ),
        CommitmentItem(
            id = "demo-sub-6",
            name = "DevStack Cloud Instance",
            merchantName = "DevStack Cloud",
            amount = 550.0,
            currency = "INR",
            cadence = "Monthly",
            normalizedMonthlyAmount = 550.0,
            nextBillingDate = "Dec 14, 2026",
            category = "Cloud & SaaS",
            status = "Needs Review",
            confidenceScore = 88,
            logoKey = "devstack",
            paymentRail = "Direct Website",
            hasPriceHike = false,
            reasonExplanation = "Variable usage charges detected over the last two months. Review suggested."
        ),
        CommitmentItem(
            id = "demo-sub-7",
            name = "Zenith Fitness Membership",
            merchantName = "Zenith Gym",
            amount = 1500.0,
            currency = "INR",
            cadence = "Monthly",
            normalizedMonthlyAmount = 1500.0,
            nextBillingDate = "Dec 18, 2026",
            category = "Fitness",
            status = "Potential",
            confidenceScore = 92,
            logoKey = "zenith",
            paymentRail = "NACH / e-Mandate",
            hasPriceHike = false,
            reasonExplanation = "Physical gym recurring monthly standing instruction mandate."
        )
    )

    fun getSpendTrend(filter: DateRangeFilter = DateRangeFilter.LAST_6_MONTHS): List<SpendTrendPoint> {
        return when (filter) {
            DateRangeFilter.LAST_30_DAYS -> listOf(
                SpendTrendPoint("W1", 2800.0),
                SpendTrendPoint("W2", 3100.0),
                SpendTrendPoint("W3", 3400.0),
                SpendTrendPoint("W4", 3150.0)
            )
            DateRangeFilter.LAST_3_MONTHS -> listOf(
                SpendTrendPoint("Sep", 12100.0),
                SpendTrendPoint("Oct", 12450.0),
                SpendTrendPoint("Nov (Est)", 12450.0, isProjected = true)
            )
            DateRangeFilter.LAST_12_MONTHS, DateRangeFilter.THIS_YEAR -> listOf(
                SpendTrendPoint("Jan", 9800.0),
                SpendTrendPoint("Feb", 9950.0),
                SpendTrendPoint("Mar", 10200.0),
                SpendTrendPoint("Apr", 10400.0),
                SpendTrendPoint("May", 10600.0),
                SpendTrendPoint("Jun", 10800.0),
                SpendTrendPoint("Jul", 11200.0),
                SpendTrendPoint("Aug", 11900.0),
                SpendTrendPoint("Sep", 12100.0),
                SpendTrendPoint("Oct", 12450.0),
                SpendTrendPoint("Nov (Est)", 12450.0, isProjected = true),
                SpendTrendPoint("Dec (Est)", 12450.0, isProjected = true)
            )
            else -> listOf(
                SpendTrendPoint("Jun", 10800.0),
                SpendTrendPoint("Jul", 11200.0),
                SpendTrendPoint("Aug", 11900.0),
                SpendTrendPoint("Sep", 12100.0),
                SpendTrendPoint("Oct", 12450.0),
                SpendTrendPoint("Nov (Est)", 12450.0, isProjected = true)
            )
        }
    }

    fun getCategories(): List<CategorySpend> = listOf(
        CategorySpend("Streaming & Media", 3800.0, 31, 0xFFFF5500),
        CategorySpend("AI & Cloud Software", 4500.0, 36, 0xFF00E0E0),
        CategorySpend("Fitness & Health", 1500.0, 12, 0xFF14B8A6),
        CategorySpend("Shopping & Delivery", 1499.0, 12, 0xFFF59E0B),
        CategorySpend("Other Mandates", 1151.0, 9, 0xFF8E9CAE)
    )

    fun getTopMerchants(): List<MerchantComparison> = listOf(
        MerchantComparison("OpenAI (ChatGPT)", 23988.0, 1999.0, "openai", "AI / Tech", 1),
        MerchantComparison("Zenith Gym Fitness", 18000.0, 1500.0, "zenith", "Fitness", 2),
        MerchantComparison("Netflix India", 9588.0, 799.0, "netflix", "Streaming", 3),
        MerchantComparison("DevStack Cloud", 6600.0, 550.0, "devstack", "Cloud & SaaS", 4),
        MerchantComparison("YouTube Premium", 3588.0, 299.0, "youtube", "Streaming", 5)
    )

    fun getUpcoming(): List<UpcomingPayment> = listOf(
        UpcomingPayment("up-1", "Netflix India", 799.0, "Nov 28", 12, "Monthly", "netflix"),
        UpcomingPayment("up-2", "OpenAI ChatGPT", 1999.0, "Dec 02", 16, "Monthly", "openai"),
        UpcomingPayment("up-3", "Spotify India", 119.0, "Dec 05", 19, "Monthly", "spotify")
    )

    fun getPriceHikes(): List<PriceHikeAlert> = listOf(
        PriceHikeAlert(
            id = "hike-1",
            merchantName = "YouTube Premium Family",
            oldAmount = 189.0,
            newAmount = 299.0,
            percentageChange = 58.2,
            effectiveDate = "Oct 2026",
            logoKey = "youtube"
        ),
        PriceHikeAlert(
            id = "hike-2",
            merchantName = "Netflix India",
            oldAmount = 649.0,
            newAmount = 799.0,
            percentageChange = 23.1,
            effectiveDate = "Sep 2026",
            logoKey = "netflix"
        )
    )

    fun getHistoricalDebits(commitmentId: String): List<HistoricalDebit> = listOf(
        HistoricalDebit("tx-1", "28 Oct 2026", 799.0, "UPI/50392819/NETFLIX ENTERTAINMENT/MUMBAI"),
        HistoricalDebit("tx-2", "28 Sep 2026", 799.0, "UPI/49201948/NETFLIX ENTERTAINMENT/MUMBAI"),
        HistoricalDebit("tx-3", "28 Aug 2026", 649.0, "UPI/48194022/NETFLIX ENTERTAINMENT/MUMBAI"),
        HistoricalDebit("tx-4", "28 Jul 2026", 649.0, "UPI/47103940/NETFLIX ENTERTAINMENT/MUMBAI")
    )

    fun getCancellationPlaybook(commitmentId: String, merchant: String, rail: String): CancellationPlaybook {
        return when (rail.lowercase()) {
            "upi autopay" -> CancellationPlaybook(
                commitmentId = commitmentId,
                merchantName = merchant,
                paymentMechanism = "UPI AutoPay",
                steps = listOf(
                    "Open your UPI app (Google Pay, PhonePe, Paytm, or BHIM).",
                    "Tap your Profile Icon in the top right corner.",
                    "Select 'Autopay' or 'Mandates'.",
                    "Locate '$merchant' under Active Mandates.",
                    "Tap 'Pause Mandate' or 'Cancel Mandate'.",
                    "Authenticate with your UPI PIN to revoke the auto-debit permission."
                ),
                warningNote = "Revoking a UPI mandate prevents future bank debits. If you have an active annual contract with $merchant, remember to also notify their customer support."
            )
            "credit card recurring" -> CancellationPlaybook(
                commitmentId = commitmentId,
                merchantName = merchant,
                paymentMechanism = "Credit Card Recurring Instruction",
                steps = listOf(
                    "Open your Bank's Mobile Banking App (HDFC, ICICI, SBI, Axis, Amex).",
                    "Go to 'Cards' > 'Card Controls' or 'Manage Standing Instructions'.",
                    "Locate the recurring token or merchant mandate for '$merchant'.",
                    "Select 'Revoke Token' or 'Disable Recurring Charges'.",
                    "Authenticate using your net banking MPIN or OTP."
                ),
                warningNote = "Under RBI tokenization guidelines, revoking your card token ensures the merchant cannot initiate recurring card debits."
            )
            "nach / e-mandate" -> CancellationPlaybook(
                commitmentId = commitmentId,
                merchantName = merchant,
                paymentMechanism = "NACH / e-Mandate",
                steps = listOf(
                    "Contact your issuing bank via phone banking or internet banking.",
                    "Request cancellation of NACH mandate registered for '$merchant'.",
                    "Provide your UMRN (Unique Mandate Reference Number).",
                    "Send a written cancellation notice to $merchant billing department."
                ),
                warningNote = "NACH mandate cancellations require 3 to 7 business days to process with the clearing house."
            )
            "google play store" -> CancellationPlaybook(
                commitmentId = commitmentId,
                merchantName = merchant,
                paymentMechanism = "Google Play Store Subscription",
                steps = listOf(
                    "Open the Google Play Store app.",
                    "Tap your Profile icon > 'Payments & subscriptions' > 'Subscriptions'.",
                    "Select '$merchant'.",
                    "Tap 'Cancel subscription' at the bottom.",
                    "Follow the on-screen prompts to confirm."
                ),
                destinationUrl = "https://play.google.com/store/account/subscriptions"
            )
            else -> CancellationPlaybook(
                commitmentId = commitmentId,
                merchantName = merchant,
                paymentMechanism = "Direct Merchant Website",
                steps = listOf(
                    "Sign in to your $merchant account via web browser.",
                    "Go to Account Settings > Billing & Subscriptions.",
                    "Click 'Manage Plan' or 'Cancel Membership'.",
                    "Complete the cancellation steps, skipping any retention discount prompts.",
                    "Save or screenshot your cancellation confirmation reference."
                )
            )
        }
    }

    fun getSamplePreview(): StatementPreviewData = StatementPreviewData(
        fileName = "HDFC_Bank_Oct2026_Statement.pdf",
        fileType = "PDF",
        totalRows = 48,
        debitCount = 38,
        totalDebits = 46850.0,
        creditCount = 10,
        totalCredits = 85000.0,
        sampleTransactions = listOf(
            TransactionPreviewItem("28-10-2026", "UPI/50392819/NETFLIX ENTERTAINMENT/MUMBAI", 799.0, true, "Netflix India"),
            TransactionPreviewItem("27-10-2026", "POS 402910 CHATGPT SUBSCRIPTION SF US", 1999.0, true, "OpenAI"),
            TransactionPreviewItem("25-10-2026", "ACH DR SWIGGY ONE MEMBERSHIP BLR", 299.0, true, "Swiggy One"),
            TransactionPreviewItem("20-10-2026", "SALARY CREDIT TECHCORP PRIVATE LIMITED", 85000.0, false, "Salary"),
            TransactionPreviewItem("18-10-2026", "NACH ZENITH FITNESS GYM MUMBAI", 1500.0, true, "Zenith Gym"),
            TransactionPreviewItem("15-10-2026", "UPI/49201948/SPOTIFY INDIA/MUMBAI", 119.0, true, "Spotify India")
        )
    )

    fun getSavedPlans(): List<SavedSavingsPlan> = listOf(
        SavedSavingsPlan(
            id = "plan-1",
            name = "Q1 2027 Budget Clean-up",
            selectedCommitmentIds = listOf("demo-sub-1", "demo-sub-6", "demo-sub-7"),
            annualSavings = 34200.0,
            projectedAnnualCost = 115200.0,
            createdAt = "15 Nov 2026"
        )
    )
}
