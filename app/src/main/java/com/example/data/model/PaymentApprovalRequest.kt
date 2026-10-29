package com.example.data.model

data class PaymentApprovalRequest(
    val id: String,
    val userId: Long = 0L,
    val userName: String,
    val userPhone: String,
    val planType: String, // "PRO" (₹99) or "PREMIUM" (₹299)
    val amount: Int, // 99 or 299
    val utrNumber: String,
    val generatedOtp: String, // 6 digits for PRO, 12 digits for PREMIUM
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // "PENDING", "APPROVED", "REJECTED"
)
