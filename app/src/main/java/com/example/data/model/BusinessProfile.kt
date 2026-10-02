package com.example.data.model

data class BusinessProfile(
    val businessName: String = "BillGen AI Shop",
    val businessTagline: String = "Quality • Trust • Service",
    val businessAddress: String = "Connaught Place, New Delhi, India",
    val businessPhone: String = "+91 98765 43210",
    val businessEmail: String = "billing@billgen.ai",
    val businessWebsite: String = "www.billgen.ai",
    val businessGstin: String = "07AAAAA0000A1Z5",
    val upiId: String = "billgen@upi",
    val bankDetails: String = "HDFC Bank • A/C 50200012345678 • IFSC HDFC0001234",
    val logoUri: String = "",
    val defaultGstRate: Double = 18.0,
    val defaultCurrency: String = "INR",
    val defaultTheme: String = "orange",
    val defaultNotes: String = "Thank you for your business!",
    val defaultTerms: String = "Goods once sold cannot be returned unless defective.",
    val invoicePrefix: String = "INV",
    val nextInvoiceNumber: Int = 1
)
