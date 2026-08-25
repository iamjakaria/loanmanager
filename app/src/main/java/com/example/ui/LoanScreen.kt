package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LoanEntity
import com.example.ui.theme.LoanBgGradientEnd
import com.example.ui.theme.LoanBgGradientStart
import com.example.ui.theme.LoanBorderGray
import com.example.ui.theme.LoanCardBorder
import com.example.ui.theme.LoanDeepPurple
import com.example.ui.theme.LoanDeleteBtn
import com.example.ui.theme.LoanDriveBlueEnd
import com.example.ui.theme.LoanDriveBlueStart
import com.example.ui.theme.LoanEditBtn
import com.example.ui.theme.LoanHeaderBgEnd
import com.example.ui.theme.LoanHeaderBgStart
import com.example.ui.theme.LoanNamePurple
import com.example.ui.theme.LoanPendingBg
import com.example.ui.theme.LoanPendingBorder
import com.example.ui.theme.LoanPendingText
import com.example.ui.theme.LoanPrimaryHover
import com.example.ui.theme.LoanPrimaryLight
import com.example.ui.theme.LoanPrimaryPurple
import com.example.ui.theme.LoanRowHover
import com.example.ui.theme.LoanSuccessBg
import com.example.ui.theme.LoanSuccessBorder
import com.example.ui.theme.LoanSuccessText
import com.example.ui.theme.LoanTextMain
import com.example.ui.theme.LoanTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun LoanScreen(
    viewModel: LoanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Export Document Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonContent = viewModel.getExportJsonString()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonContent.toByteArray())
                        }
                    }
                    snackbarHostState.showSnackbar(
                        message = "Backup exported successfully!",
                        duration = SnackbarDuration.Short
                    )
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Failed to export: ${e.localizedMessage ?: "Unknown error"}",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    // Import Document Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonString = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        }
                    }
                    if (!jsonString.isNullOrEmpty()) {
                        viewModel.importFromJsonString(jsonString)
                    } else {
                        snackbarHostState.showSnackbar("Selected backup file is empty.")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error reading file: ${e.localizedMessage}")
                }
            }
        }
    }

    // React to user messages from ViewModel
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // Background linear gradient: 135deg from #F3E8FF to #E0E7FF
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(LoanBgGradientStart, LoanBgGradientEnd)
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            // Main Modal Card Container ("table-modal")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, LoanCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("loan_manager_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Modal Header
                    LoanHeader(
                        onExport = {
                            exportLauncher.launch("loan_manager_backup.json")
                        },
                        onImport = {
                            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        onAddLoan = {
                            viewModel.openAddModal()
                        }
                    )

                    HorizontalDivider(thickness = 2.dp, color = LoanBorderGray)

                    // 2. Table / List Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (uiState.loans.isEmpty()) {
                            // Empty State
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No loan records found.",
                                    color = Color(0xFF6B7280),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ActionGradientButton(
                                    text = "+ Add Loan",
                                    gradientColors = listOf(LoanPrimaryLight, LoanPrimaryPurple),
                                    onClick = { viewModel.openAddModal() },
                                    testTag = "empty_add_loan_btn"
                                )
                            }
                        } else {
                            LoanTableResponsive(
                                groupedLoans = uiState.groupedLoans,
                                grandTotal = uiState.grandTotal,
                                badgeStates = uiState.badgeViewStates,
                                onToggleBadge = { loanId -> viewModel.toggleBadgeView(loanId) },
                                onEdit = { loan -> viewModel.openEditModal(loan) },
                                onDelete = { loan -> viewModel.promptDelete(loan) }
                            )
                        }
                    }

                    // 3. Modal Footer (Clean finish matching HTML design)
                    HorizontalDivider(thickness = 1.dp, color = LoanBorderGray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LoanHeaderBgStart)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.loans.size} active record(s)",
                            color = LoanTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Offline-Ready • Room DB",
                            color = LoanDeepPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Add / Edit Modal Dialog
        uiState.dialogState?.let { dialogState ->
            AddEditLoanDialog(
                state = dialogState,
                uniqueBorrowerNames = uiState.uniqueBorrowerNames,
                onStateChange = { transform -> viewModel.updateDialogState(transform) },
                onSave = { viewModel.saveDialogEntry() },
                onDismiss = { viewModel.closeDialog() }
            )
        }

        // Delete Confirmation Dialog
        uiState.deleteConfirmationLoan?.let { loanToDelete ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeletePrompt() },
                title = {
                    Text(
                        text = "Delete Loan Record?",
                        color = LoanPrimaryHover,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete the loan record for ${loanToDelete.name} (${LoanViewModel.formatCurrency(loanToDelete.amount)})?",
                        color = LoanTextMain,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDelete() },
                        colors = ButtonDefaults.buttonColors(containerColor = LoanDeleteBtn),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("dialog_confirm_delete_btn")
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissDeletePrompt() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = LoanTextMuted)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("delete_confirm_dialog")
            )
        }
    }
}

@Composable
fun LoanHeader(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onAddLoan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(LoanHeaderBgStart, LoanHeaderBgEnd)
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Loan Manager",
            color = LoanPrimaryHover,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .testTag("app_title")
        )

        // Action Buttons Row (Responsive Wrap)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionGradientButton(
                text = "Export",
                icon = Icons.Default.FileUpload,
                gradientColors = listOf(LoanDriveBlueStart, LoanDriveBlueEnd),
                onClick = onExport,
                testTag = "export_btn"
            )

            Spacer(modifier = Modifier.width(8.dp))

            ActionGradientButton(
                text = "Import",
                icon = Icons.Default.FileDownload,
                gradientColors = listOf(LoanDriveBlueStart, LoanDriveBlueEnd),
                onClick = onImport,
                testTag = "import_btn"
            )

            Spacer(modifier = Modifier.width(8.dp))

            ActionGradientButton(
                text = "+ Add Loan",
                icon = Icons.Default.Add,
                gradientColors = listOf(LoanPrimaryLight, LoanPrimaryPurple),
                onClick = onAddLoan,
                testTag = "add_loan_btn"
            )
        }
    }
}

@Composable
fun ActionGradientButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .shadow(4.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LoanTableResponsive(
    groupedLoans: List<BorrowerLoanGroup>,
    grandTotal: Double,
    badgeStates: Map<Long, Boolean>,
    onToggleBadge: (Long) -> Unit,
    onEdit: (LoanEntity) -> Unit,
    onDelete: (LoanEntity) -> Unit
) {
    val horizontalScrollState = rememberScrollState()

    // Minimum width of 650dp matching HTML's min-width: 650px table
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .widthIn(min = 650.dp)
        ) {
            // Table Header Row (Sticky at top)
            TableHeader()

            HorizontalDivider(thickness = 2.dp, color = LoanCardBorder)

            // Table Data Rows
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                groupedLoans.forEach { group ->
                    items(group.items, key = { it.loan.id }) { itemDisplay ->
                        TableRowItem(
                            itemDisplay = itemDisplay,
                            isBadgeToggled = badgeStates[itemDisplay.loan.id] ?: false,
                            onToggleBadge = { onToggleBadge(itemDisplay.loan.id) },
                            onEdit = { onEdit(itemDisplay.loan) },
                            onDelete = { onDelete(itemDisplay.loan) }
                        )
                        HorizontalDivider(thickness = 1.dp, color = LoanBorderGray)
                    }
                }
            }

            // Table Sticky Grand Total Row (At the bottom)
            TableGrandTotalRow(grandTotal = grandTotal)
        }
    }
}

@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoanHeaderBgStart)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Name",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .width(180.dp)
                .padding(start = 16.dp)
        )
        Text(
            text = "Date",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = "Amount",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = "Status",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = "Actions",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(130.dp)
                .padding(end = 12.dp)
        )
    }
}

@Composable
fun TableRowItem(
    itemDisplay: LoanItemDisplay,
    isBadgeToggled: Boolean,
    onToggleBadge: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val loan = itemDisplay.loan
    val displayDate = LoanViewModel.formatDateToDDMMYYYY(loan.date)
    val displayRepaidDate = LoanViewModel.formatDateToDDMMYYYY(
        if (loan.repaidDate.isNotEmpty()) loan.repaidDate else loan.date
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag("loan_row_${loan.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name Column (with subtotal if first in group)
        Column(
            modifier = Modifier
                .width(180.dp)
                .padding(start = 16.dp)
        ) {
            if (itemDisplay.isFirstInGroup) {
                Text(
                    text = loan.name,
                    color = LoanNamePurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Total: ${LoanViewModel.formatCurrency(itemDisplay.groupSubtotal)}",
                    color = LoanPrimaryPurple,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Date Column
        Text(
            text = displayDate,
            color = LoanTextMain,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(100.dp)
        )

        // Amount Column
        Text(
            text = LoanViewModel.formatCurrency(loan.amount),
            color = LoanTextMain,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(130.dp)
        )

        // Status Badge Column
        Box(
            modifier = Modifier.width(110.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loan.status == "Not-Paid") {
                // Not Paid Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LoanPendingBg)
                        .border(1.dp, LoanPendingBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("badge_not_paid_${loan.id}")
                ) {
                    Text(
                        text = "Not Paid",
                        color = LoanPendingText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            } else {
                // Repaid Badge (Clickable to toggle repayment date!)
                val badgeText = if (isBadgeToggled) displayRepaidDate else "Repaid"
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LoanSuccessBg)
                        .border(1.dp, LoanSuccessBorder, CircleShape)
                        .clickable { onToggleBadge() }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("badge_repaid_${loan.id}")
                ) {
                    Text(
                        text = badgeText,
                        color = LoanSuccessText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Actions Column
        Row(
            modifier = Modifier
                .width(130.dp)
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LoanEditBtn)
                    .clickable { onEdit() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_edit_${loan.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Edit",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Delete Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LoanDeleteBtn)
                    .clickable { onDelete() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_delete_${loan.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Delete",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun TableGrandTotalRow(grandTotal: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoanHeaderBgStart)
            .border(BorderStroke(2.dp, LoanCardBorder))
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .testTag("table_total_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Total",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .width(180.dp)
                .padding(start = 16.dp)
        )
        Text(
            text = "-",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = LoanViewModel.formatCurrency(grandTotal),
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = "-",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = "-",
            color = LoanDeepPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(130.dp)
                .padding(end = 12.dp)
        )
    }
}
