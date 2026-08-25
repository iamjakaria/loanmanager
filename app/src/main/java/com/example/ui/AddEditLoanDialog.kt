package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LoanBorderGray
import com.example.ui.theme.LoanCardBorder
import com.example.ui.theme.LoanDeepPurple
import com.example.ui.theme.LoanInputBorder
import com.example.ui.theme.LoanNamePurple
import com.example.ui.theme.LoanPrimaryHover
import com.example.ui.theme.LoanPrimaryLight
import com.example.ui.theme.LoanPrimaryPurple
import com.example.ui.theme.LoanTextMain
import com.example.ui.theme.LoanTextMuted
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanDialog(
    state: AddEditDialogState,
    uniqueBorrowerNames: List<String>,
    onStateChange: ((AddEditDialogState) -> AddEditDialogState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var statusMenuExpanded by remember { mutableStateOf(false) }

    // Autocomplete dropdown matching
    val filteredNames = remember(state.name, uniqueBorrowerNames) {
        val trimmed = state.name.trim()
        if (trimmed.isEmpty()) {
            emptyList()
        } else {
            uniqueBorrowerNames.filter { it.contains(trimmed, ignoreCase = true) && !it.equals(trimmed, ignoreCase = true) }
        }
    }
    var showAutocomplete by remember { mutableStateOf(true) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .testTag("add_edit_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, LoanCardBorder),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (state.isEdit) "Edit Loan Entry" else "Add New Loan",
                    color = LoanPrimaryHover,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 1. Borrower Name Field with Autocomplete
                Text(
                    text = "Name",
                    color = LoanDeepPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { newName ->
                            showAutocomplete = true
                            onStateChange { it.copy(name = newName, nameError = null) }
                        },
                        placeholder = { Text("Enter borrower name", color = Color(0xFF9CA3AF), fontSize = 14.sp) },
                        singleLine = true,
                        isError = state.nameError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAF5FF),
                            focusedBorderColor = LoanPrimaryPurple,
                            unfocusedBorderColor = LoanInputBorder,
                            focusedTextColor = LoanTextMain,
                            unfocusedTextColor = LoanTextMain
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_name_input")
                    )

                    // Autocomplete Suggestions List
                    if (showAutocomplete && filteredNames.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LoanInputBorder),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 56.dp)
                                .heightIn(max = 130.dp)
                        ) {
                            LazyColumn {
                                items(filteredNames) { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onStateChange { it.copy(name = suggestion, nameError = null) }
                                                showAutocomplete = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            color = LoanTextMain,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.nameError != null) {
                    Text(
                        text = state.nameError,
                        color = Color(0xFFE11D48),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Date Field
                Text(
                    text = "Date",
                    color = LoanDeepPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = state.date,
                    onValueChange = { newDate ->
                        onStateChange { it.copy(date = newDate) }
                    },
                    readOnly = true,
                    placeholder = { Text("YYYY-MM-DD", color = Color(0xFF9CA3AF), fontSize = 14.sp) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (state.date.isNotEmpty()) {
                                    val parts = state.date.split("-")
                                    if (parts.size == 3) {
                                        cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
                                        cal.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
                                        cal.set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: 1)
                                    }
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val pickedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                        onStateChange { it.copy(date = pickedDate) }
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.testTag("dialog_date_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick Date",
                                tint = LoanPrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAF5FF),
                        focusedBorderColor = LoanPrimaryPurple,
                        unfocusedBorderColor = LoanInputBorder,
                        focusedTextColor = LoanTextMain,
                        unfocusedTextColor = LoanTextMain
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val pickedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    onStateChange { it.copy(date = pickedDate) }
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .testTag("dialog_date_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Amount Field
                Text(
                    text = "Amount (৳)",
                    color = LoanDeepPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { newAmount ->
                        onStateChange { it.copy(amount = newAmount, amountError = null) }
                    },
                    placeholder = { Text("0.00", color = Color(0xFF9CA3AF), fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = state.amountError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAF5FF),
                        focusedBorderColor = LoanPrimaryPurple,
                        unfocusedBorderColor = LoanInputBorder,
                        focusedTextColor = LoanTextMain,
                        unfocusedTextColor = LoanTextMain
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_amount_input")
                )
                if (state.amountError != null) {
                    Text(
                        text = state.amountError,
                        color = Color(0xFFE11D48),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Status Dropdown
                Text(
                    text = "Status",
                    color = LoanDeepPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (state.status == "Repaid") "Repaid" else "Not Paid",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { statusMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select status",
                                    tint = LoanPrimaryPurple
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAF5FF),
                            focusedBorderColor = LoanPrimaryPurple,
                            unfocusedBorderColor = LoanInputBorder,
                            focusedTextColor = LoanTextMain,
                            unfocusedTextColor = LoanTextMain
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { statusMenuExpanded = true }
                            .testTag("dialog_status_selector")
                    )

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Repaid", color = LoanTextMain, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                val today = LoanViewModel.getTodayFormatted()
                                onStateChange {
                                    it.copy(
                                        status = "Repaid",
                                        repaidDate = if (it.repaidDate.isEmpty()) today else it.repaidDate
                                    )
                                }
                                statusMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Not Paid", color = LoanTextMain, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                onStateChange { it.copy(status = "Not-Paid", repaidDate = "") }
                                statusMenuExpanded = false
                            }
                        )
                    }
                }

                // 5. Repayment Date Field (Conditional on "Repaid")
                if (state.status == "Repaid") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Repayment Date",
                        color = LoanDeepPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.repaidDate,
                        onValueChange = { newRepaidDate ->
                            onStateChange { it.copy(repaidDate = newRepaidDate) }
                        },
                        readOnly = true,
                        placeholder = { Text("YYYY-MM-DD", color = Color(0xFF9CA3AF), fontSize = 14.sp) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    if (state.repaidDate.isNotEmpty()) {
                                        val parts = state.repaidDate.split("-")
                                        if (parts.size == 3) {
                                            cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
                                            cal.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
                                            cal.set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: 1)
                                        }
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val pickedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                            onStateChange { it.copy(repaidDate = pickedDate) }
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.testTag("dialog_repaid_date_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Repayment Date",
                                    tint = LoanPrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAF5FF),
                            focusedBorderColor = LoanPrimaryPurple,
                            unfocusedBorderColor = LoanInputBorder,
                            focusedTextColor = LoanTextMain,
                            unfocusedTextColor = LoanTextMain
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val pickedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                        onStateChange { it.copy(repaidDate = pickedDate) }
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .testTag("dialog_repaid_date_input")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Cancel and Save Entry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color(0xFFF3F4F6),
                            contentColor = LoanTextMuted
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("dialog_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .shadow(4.dp, shape = RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(LoanPrimaryLight, LoanPrimaryPurple)
                                )
                            )
                            .clickable { onSave() }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .testTag("dialog_save_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.isEdit) "Update Entry" else "Save Entry",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
