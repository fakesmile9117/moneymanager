package com.example.data

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class TransactionRepository(private val transactionDao: TransactionDao) {

    private val TAG = "TransactionRepository"

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Configured OkHttpClient with generous timeouts to allow Google Apps Script execution time
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://script.google.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GoogleSheetsApi::class.java)

    /**
     * Inserts a transaction locally and triggers background sync.
     */
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            val localId = transactionDao.insertTransaction(transaction)
            // Trigger sync in background
            syncUnsyncedTransactions()
            localId
        }
    }

    /**
     * Deletes a transaction locally.
     */
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransaction(transaction)
        }
    }

    /**
     * Perform local Database cleaning.
     */
    suspend fun clearAllLocal() {
        withContext(Dispatchers.IO) {
            transactionDao.deleteAll()
        }
    }

    /**
     * Syncs any locally saved transactions that have not yet been sent to Google Sheets.
     */
    suspend fun syncUnsyncedTransactions() {
        withContext(Dispatchers.IO) {
            try {
                val unsyncedList = transactionDao.getUnsyncedTransactions()
                if (unsyncedList.isEmpty()) {
                    Log.d(TAG, "No unsynced transactions found.")
                    return@withContext
                }

                Log.d(TAG, "Syncing ${unsyncedList.size} transaction entries with Google Sheets...")
                for (item in unsyncedList) {
                    // Try uploading using query-parameter strategy which is 100% redirect-safe
                    val response = api.uploadTransactionQuery(
                        categoryOld = item.category,
                        categoryNew = item.category,
                        date = item.date,
                        day = item.day,
                        amount = item.amount,
                        comment = item.comment
                    )

                    if (response.isSuccessful) {
                        Log.d(TAG, "Transaction successfully synced to Google Sheets: ID ${item.id}")
                        transactionDao.updateSyncStatus(item.id, true)
                    } else {
                        Log.e(TAG, "Failed sync for transaction ID ${item.id}: Code ${response.code()}")
                        // Check if fallback upload method is needed
                        val fallbackResponse = api.uploadTransactionJson(
                            TransactionDto(
                                caterogious = item.category,
                                category = item.category,
                                date = item.date,
                                day = item.day,
                                amount = item.amount,
                                comment = item.comment
                            )
                        )
                        if (fallbackResponse.isSuccessful) {
                            Log.d(TAG, "Fallback sync successful for ID ${item.id}")
                            transactionDao.updateSyncStatus(item.id, true)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception logging unsynced items: ${e.message}", e)
            }
        }
    }

    /**
     * Fetches current transactions list from Google spreadsheet.
     * Parses the response and updates local Room table if appropriate.
     */
    suspend fun fetchFromGoogleSheets(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching data list from Google Sheets...")
                val response = api.getTransactions()
                if (response.isSuccessful) {
                    val rawBodyString = response.body()?.string()
                    Log.d(TAG, "Fetch body: $rawBodyString")
                    if (!rawBodyString.isNullOrBlank()) {
                        tryToParseAndMerge(rawBodyString)
                        return@withContext true
                    }
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from Google sheets: ${e.message}", e)
                false
            }
        }
    }

    private suspend fun tryToParseAndMerge(rawJson: String) {
        // Many Apps Script instances return raw array of columns/rows directly, or wrapped in a standard JSON
        // Let's implement robust parsing that handles both!
        try {
            // Option A: Try to parse as a wrapped response containing list of transactions DTOs
            val wrapperAdapter = moshi.adapter(SheetResponseDto::class.java)
            val wrapper = wrapperAdapter.fromJson(rawJson)
            val dtoList = wrapper?.data ?: run {
                // Option B: Try to parse directly as a list of TransactionDto
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, TransactionDto::class.java)
                val listAdapter = moshi.adapter<List<TransactionDto>>(listType)
                listAdapter.fromJson(rawJson)
            }

            if (dtoList != null && dtoList.isNotEmpty()) {
                Log.d(TAG, "Successfully parsed ${dtoList.size} records from Sheets. Merging into database...")
                
                // Pure sheets data focus: delete old synced records to prevent stale/duplicate cache entries
                transactionDao.deleteSyncedTransactions()
                
                // Keep existing unsynced transactions, but we can insert synced ones
                for (dto in dtoList) {
                    val resolvedCategory = (dto.category?.takeIf { it.isNotBlank() } 
                        ?: dto.caterogious?.takeIf { it.isNotBlank() } 
                        ?: "Others")
                    val cleanAmount = dto.amount
                    
                    // Check if it already exists to avoid duplication
                    val entity = TransactionEntity(
                        category = resolvedCategory,
                        date = dto.date,
                        day = dto.day,
                        amount = cleanAmount,
                        comment = dto.comment,
                        isSynced = true // Pulled from spreadsheet, so it is synced
                    )
                    transactionDao.insertTransaction(entity)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sheet JSON content: ${e.message}", e)
        }
    }
}
