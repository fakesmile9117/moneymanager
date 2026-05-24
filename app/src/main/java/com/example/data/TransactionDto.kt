package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransactionDto(
    @Json(name = "caterogious") val caterogious: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "date") val date: String,
    @Json(name = "day") val day: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "comment") val comment: String
)

@JsonClass(generateAdapter = true)
data class SheetResponseDto(
    val status: String? = null,
    val message: String? = null,
    val data: List<TransactionDto>? = null
)
