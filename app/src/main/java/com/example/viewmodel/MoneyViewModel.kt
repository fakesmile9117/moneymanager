package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MoneyViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MoneyViewModel"
    private val prefs = application.getSharedPreferences("money_manager_prefs", Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database.transactionDao())

    // Login state
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Sync State
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Database state
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    init {
        // Start with a clean local database so only the user's synced entries are shown
        // Automatically fetch fresh data from Google Sheets on start to never rely on stale mobile cache
        syncWithGoogleSheets()
    }

    private suspend fun prePopulateDemoData() {
        Log.d(TAG, "Pre-populating database with classic screenshot transaction entries...")
        val demoItems = listOf(
            TransactionEntity(
                category = "Food & Dining",
                date = "2026-05-22",
                day = "Friday",
                amount = -120.50,
                comment = "Corporate team meet",
                isSynced = false
            ),
            TransactionEntity(
                category = "Salary",
                date = "2026-05-20",
                day = "Wednesday",
                amount = 2800.00,
                comment = "UI Design completion",
                isSynced = false
            ),
            TransactionEntity(
                category = "Groceries",
                date = "2026-05-18",
                day = "Monday",
                amount = -84.20,
                comment = "Weekly supply",
                isSynced = false
            ),
            TransactionEntity(
                category = "Living/Rent",
                date = "2026-05-15",
                day = "Friday",
                amount = -1200.00,
                comment = "Monthly apartment fee",
                isSynced = false
            ),
            TransactionEntity(
                category = "Transport",
                date = "2026-05-12",
                day = "Tuesday",
                amount = -12.50,
                comment = "Uber ride to office",
                isSynced = false
            ),
            TransactionEntity(
                category = "Shopping",
                date = "2026-05-10",
                day = "Sunday",
                amount = -150.00,
                comment = "New keyboard",
                isSynced = false
            )
        )

        for (item in demoItems) {
            repository.insertTransaction(item)
        }
    }

    // Login logic
    fun login(passUsername: String, passSecret: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            
            // Expected login credentials: Username "fs" and Password "mugu9117"
            if (passUsername.trim() == "fs" && passSecret == "mugu9117") {
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
            } else {
                _loginError.value = "Invalid username or password"
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    // Transaction logic
    fun saveTransaction(
        id: Long = 0,
        category: String,
        amount: Double,
        date: String,
        day: String,
        comment: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val transaction = TransactionEntity(
                id = id,
                category = category,
                date = date,
                day = day,
                amount = amount,
                comment = comment,
                isSynced = false
            )
            repository.insertTransaction(transaction)
            _isLoading.value = false
            onSuccess()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.clearAllLocal()
        }
    }

    // Search operations
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    // Synchronization
    fun syncWithGoogleSheets() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Connecting to Google Sheet..."
            
            // First push any pending unsynced changes
            repository.syncUnsyncedTransactions()
            
            // Then attempt to fetch sheets list
            val success = repository.fetchFromGoogleSheets()
            
            _isSyncing.value = false
            if (success) {
                _syncMessage.value = "Synced successfully with Google Sheets!"
            } else {
                _syncMessage.value = "Sync completed. (Check connection or URL script status)"
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Convenience computed values helper
    fun getSummary(transactions: List<TransactionEntity>): SummaryData {
        var income = 0.0
        var expense = 0.0
        for (item in transactions) {
            if (item.amount > 0) {
                income += item.amount
            } else {
                expense += kotlin.math.abs(item.amount)
            }
        }
        val balance = income - expense
        return SummaryData(
            totalIncome = income,
            totalExpense = expense,
            remainingBalance = balance
        )
    }
}

data class SummaryData(
    val totalIncome: Double,
    val totalExpense: Double,
    val remainingBalance: Double
)
