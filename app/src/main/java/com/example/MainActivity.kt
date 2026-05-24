package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MoneyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize modern StateFlow ViewModel
                val viewModel: MoneyViewModel = viewModel()
                
                // Collect states reactively
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val loginError by viewModel.loginError.collectAsState()
                val transactions by viewModel.allTransactions.collectAsState()
                val isSyncing by viewModel.isSyncing.collectAsState()
                val syncMessage by viewModel.syncMessage.collectAsState()

                // Compile-safe, high-performance State Route engine
                var currentRoute by remember { mutableStateOf("splash") }
                var transactionToEdit by remember { mutableStateOf<com.example.data.TransactionEntity?>(null) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(targetState = currentRoute, label = "routing_crossfade") { route ->
                        when (route) {
                            "splash" -> SplashScreen(
                                isLoggedIn = isLoggedIn,
                                onNavigateToLogin = { currentRoute = "login" },
                                onNavigateToMain = { currentRoute = "main" }
                            )

                            "login" -> LoginScreen(
                                isLoading = isLoading,
                                errorMessage = loginError,
                                onLoginClick = { user, pass ->
                                    viewModel.login(user, pass)
                                },
                                onLoginSuccess = { currentRoute = "main" },
                                isLoggedIn = isLoggedIn
                            )

                            "main" -> MainLayout(
                                transactions = transactions,
                                username = "fs", // default profile holder
                                onSaveTransaction = { id, cat, amt, date, day, com ->
                                    viewModel.saveTransaction(id = id, category = cat, amount = amt, date = date, day = day, comment = com) {
                                        transactionToEdit = null
                                    }
                                },
                                onLogout = {
                                    viewModel.logout()
                                    currentRoute = "login"
                                },
                                onSyncWithSheets = {
                                    viewModel.syncWithGoogleSheets()
                                },
                                onResetData = {
                                    viewModel.resetData()
                                },
                                isSyncing = isSyncing,
                                syncMessage = syncMessage,
                                onClearSyncMsg = {
                                    viewModel.clearSyncMessage()
                                },
                                onDeleteTransaction = { tx ->
                                    viewModel.deleteTransaction(tx)
                                },
                                onViewAllClick = {
                                    currentRoute = "view_all"
                                },
                                transactionToEdit = transactionToEdit,
                                onEditClick = { tx ->
                                    transactionToEdit = tx
                                },
                                onClearEdit = {
                                    transactionToEdit = null
                                }
                            )

                            "view_all" -> ViewAllScreen(
                                transactions = transactions,
                                onBackClick = {
                                    currentRoute = "main"
                                },
                                onEditClick = { tx ->
                                    transactionToEdit = tx
                                    currentRoute = "main"
                                },
                                onDeleteClick = { tx ->
                                    viewModel.deleteTransaction(tx)
                                },
                                isRefreshing = isSyncing,
                                onRefresh = {
                                    viewModel.syncWithGoogleSheets()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
