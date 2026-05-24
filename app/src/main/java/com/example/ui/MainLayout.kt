package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.theme.EmeraldPrimary

@Composable
fun MainLayout(
    transactions: List<TransactionEntity>,
    username: String,
    onSaveTransaction: (id: Long, category: String, amount: Double, date: String, day: String, comment: String) -> Unit,
    onLogout: () -> Unit,
    onSyncWithSheets: () -> Unit,
    onResetData: () -> Unit,
    isSyncing: Boolean,
    syncMessage: String?,
    onClearSyncMsg: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onViewAllClick: () -> Unit,
    transactionToEdit: TransactionEntity? = null,
    onEditClick: (TransactionEntity) -> Unit = {},
    onClearEdit: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(0) }

    LaunchedEffect(transactionToEdit) {
        if (transactionToEdit != null) {
            activeTab = 1
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                // Home tab
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = {
                        onClearEdit()
                        activeTab = 0
                    },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 0) Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = Color(0xFFE2F4EB)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )

                // Add Transaction tab
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = {
                        if (activeTab == 1) {
                            onClearEdit()
                        }
                        activeTab = 1
                    },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 1) Icons.Default.AddCircle else Icons.Outlined.AddCircleOutline,
                            contentDescription = "Add"
                        )
                    },
                    label = { Text("Add", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = Color(0xFFE2F4EB)
                    ),
                    modifier = Modifier.testTag("nav_add")
                )

                // Reports tab
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = {
                        onClearEdit()
                        activeTab = 2
                    },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 2) Icons.Default.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Reports"
                        )
                    },
                    label = { Text("Reports", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = Color(0xFFE2F4EB)
                    ),
                    modifier = Modifier.testTag("nav_reports")
                )

                // Profile tab
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = {
                        onClearEdit()
                        activeTab = 3
                    },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 3) Icons.Default.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = Color(0xFFE2F4EB)
                    ),
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> HomeScreen(
                    transactions = transactions,
                    username = username,
                    onAddExpenseClick = {
                        onClearEdit()
                        activeTab = 1
                    },
                    onReportsClick = { activeTab = 2 },
                    onViewAllClick = onViewAllClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteTransaction,
                    isRefreshing = isSyncing,
                    onRefresh = onSyncWithSheets
                )

                1 -> AddScreen(
                    transactionToEdit = transactionToEdit,
                    onSaveClick = { id, cat, amt, date, day, com ->
                        onSaveTransaction(id, cat, amt, date, day, com)
                        // Route back to Dashboard on save
                        activeTab = 0
                    }
                )

                2 -> ReportsScreen(
                    transactions = transactions,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteTransaction
                )

                3 -> ProfileScreen(
                    username = username,
                    onLogoutClick = onLogout,
                    onSyncClick = onSyncWithSheets,
                    onResetClick = onResetData,
                    isSyncing = isSyncing,
                    syncMessage = syncMessage,
                    onClearSyncMsg = onClearSyncMsg
                )
            }
        }
    }
}
