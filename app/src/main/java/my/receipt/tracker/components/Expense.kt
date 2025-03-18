package my.receipt.tracker.components

import java.util.UUID

// Expense Data Model
data class Expense(
    var description: String,
    var location: String,
    var usAmount: Double,
    var ausAumount: Double,
    var date: String,
    var receiptImagePath: String?,
    var screenshotImagePath: String?,
    val id: String = UUID.randomUUID().toString()
)