package com.example.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GoogleSheetsApi {

    @GET("macros/s/AKfycbyXDl5pUhceA7335nO7GBL0l2yEpDv4CVjoIbpUNcKbwxhhRMh3T09_rHF1bjXcwD1S/exec")
    suspend fun getTransactions(): Response<ResponseBody>

    @POST("macros/s/AKfycbyXDl5pUhceA7335nO7GBL0l2yEpDv4CVjoIbpUNcKbwxhhRMh3T09_rHF1bjXcwD1S/exec")
    @FormUrlEncoded
    suspend fun uploadTransactionForm(
        @Field("caterogious") categoryOld: String,
        @Field("category") categoryNew: String,
        @Field("date") date: String,
        @Field("day") day: String,
        @Field("amount") amount: Double,
        @Field("comment") comment: String
    ): Response<ResponseBody>

    @POST("macros/s/AKfycbyXDl5pUhceA7335nO7GBL0l2yEpDv4CVjoIbpUNcKbwxhhRMh3T09_rHF1bjXcwD1S/exec")
    suspend fun uploadTransactionJson(
        @Body payload: TransactionDto
    ): Response<ResponseBody>

    // Query parameters POST - highly robust in Google Apps Scripts environments
    @POST("macros/s/AKfycbyXDl5pUhceA7335nO7GBL0l2yEpDv4CVjoIbpUNcKbwxhhRMh3T09_rHF1bjXcwD1S/exec")
    suspend fun uploadTransactionQuery(
        @Query("caterogious") categoryOld: String,
        @Query("category") categoryNew: String,
        @Query("date") date: String,
        @Query("day") day: String,
        @Query("amount") amount: Double,
        @Query("comment") comment: String
    ): Response<ResponseBody>
}
