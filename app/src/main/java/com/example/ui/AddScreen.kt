package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    transactionToEdit: com.example.data.TransactionEntity? = null,
    onSaveClick: (id: Long, category: String, amount: Double, date: String, day: String, comment: String) -> Unit
) {
    val context = LocalContext.current
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDay = SimpleDateFormat("EEEE", Locale.getDefault())

    // Active state models
    var amountValue by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var noteValue by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var isExpenseState by remember { mutableStateOf(true) } // Expense vs Income option
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(transactionToEdit) {
        if (transactionToEdit != null) {
            amountValue = kotlin.math.abs(transactionToEdit.amount).toString()
            selectedCategory = transactionToEdit.category
            noteValue = transactionToEdit.comment
            isExpenseState = transactionToEdit.amount < 0
            val cal = Calendar.getInstance()
            try {
                val parsed = sdfDate.parse(transactionToEdit.date)
                if (parsed != null) cal.time = parsed
            } catch (e: Exception) {
                // fallback
            }
            selectedDate = cal
        } else {
            amountValue = ""
            selectedCategory = "Food & Dining"
            noteValue = ""
            selectedDate = Calendar.getInstance()
            isExpenseState = true
        }
    }

    val categories = listOf(
        "Food & Dining",
        "Salary",
        "Groceries",
        "Living/Rent",
        "Transport",
        "Shopping",
        "Medical",
        "Others"
    )

    var currentCategoryExpanded by remember { mutableStateOf(false) }

    // Helper functions for quick date chips
    fun selectQuickDay(calendarOffset: Int) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, calendarOffset)
        selectedDate = cal
    }

    fun selectLastFriday() {
        val cal = Calendar.getInstance()
        // Retreat days back until we strike Friday
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        selectedDate = cal
    }

    // DatePicker setup
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val tempCal = Calendar.getInstance()
            tempCal.set(year, month, dayOfMonth)
            selectedDate = tempCal
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("add_transaction_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App bar Row matching design
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transactionToEdit != null) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (transactionToEdit != null) "Edit Transaction" else "Add Transaction",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Submit Button in top-bar
            IconButton(
                onClick = {
                    val num = amountValue.toDoubleOrNull() ?: 0.0
                    if (num > 0) {
                        val processedAmount = if (isExpenseState) -num else num
                        val stringDate = sdfDate.format(selectedDate.time)
                        val stringDay = sdfDay.format(selectedDate.time)
                        onSaveClick(transactionToEdit?.id ?: 0L, selectedCategory, processedAmount, stringDate, stringDay, noteValue)
                        amountValue = ""
                        noteValue = ""
                    } else {
                        android.widget.Toast.makeText(context, "Please enter a valid amount", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("top_submit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (transactionToEdit != null) "Submit Update" else "Submit Transaction",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Toggle selector: Expense vs Income
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { isExpenseState = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpenseState) EmeraldPrimary else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Text(
                        "Expense",
                        color = if (isExpenseState) Color.White else EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { isExpenseState = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isExpenseState) EmeraldPrimary else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Text(
                        "Income",
                        color = if (!isExpenseState) Color.White else EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Card displaying current amount typed
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Amount",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "₹",
                        color = EmeraldPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Big input amount digits
                    TextField(
                        value = amountValue,
                        onValueChange = { amountValue = it },
                        placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("amount_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (isExpenseState) Color(0xFFC0392B) else EmeraldPrimary,
                            unfocusedTextColor = if (isExpenseState) Color(0xFFC0392B) else EmeraldPrimary
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpenseState) Color(0xFFC0392B) else EmeraldPrimary
                        )
                    )
                }
            }
        }

        // Category dropdown card Selection
        Column {
            Text(
                "Category",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { currentCategoryExpanded = true }
                    .padding(16.dp)
                    .testTag("category_dropdown")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = selectedCategory,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Icon(
                        imageVector = if (currentCategoryExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = currentCategoryExpanded,
                    onDismissRequest = { currentCategoryExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedCategory = cat
                                currentCategoryExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Date selector
        Column {
            Text(
                "Date",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { datePickerDialog.show() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${sdfDate.format(selectedDate.time)} (${sdfDay.format(selectedDate.time)})",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = "Edit calendar",
                        tint = Color.Gray
                    )
                }
            }
        }

        // Quick Day selection row matching Image 3
        Column {
            Text(
                "Quick Day Selection",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Yesterday Chip
                Button(
                    onClick = { selectQuickDay(-1) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = borderForSelectedChip(false),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Yesterday", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Today Chip
                Button(
                    onClick = { selectQuickDay(0) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Today", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Last Friday Chip
                Button(
                    onClick = { selectLastFriday() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = borderForSelectedChip(false),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Last Friday", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Note area
        Column {
            Text(
                "Note (Optional)",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = noteValue,
                onValueChange = { noteValue = it },
                placeholder = { Text("What was this for?", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = EmeraldPrimary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .testTag("note_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Submit Transaction Button
        Button(
            onClick = {
                val num = amountValue.toDoubleOrNull() ?: 0.0
                if (num > 0) {
                    // Sign amount depending on Expense toggle
                    val processedAmount = if (isExpenseState) -num else num
                    val stringDate = sdfDate.format(selectedDate.time)
                    val stringDay = sdfDay.format(selectedDate.time)
                    onSaveClick(transactionToEdit?.id ?: 0L, selectedCategory, processedAmount, stringDate, stringDay, noteValue)
                    // Reset field values
                    amountValue = ""
                    noteValue = ""
                } else {
                    android.widget.Toast.makeText(context, "Please enter a valid amount", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_transaction_button"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(27.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (transactionToEdit != null) "Submit Update" else "Submit Transaction",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun borderForSelectedChip(selected: Boolean): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = if (selected) EmeraldPrimary else Color(0xFFCFD8DC)
    )
}
