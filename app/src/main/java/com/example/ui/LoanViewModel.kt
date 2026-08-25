package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LoanEntity
import com.example.data.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LoanItemDisplay(
    val loan: LoanEntity,
    val isFirstInGroup: Boolean,
    val groupSubtotal: Double
)

data class BorrowerLoanGroup(
    val borrowerName: String,
    val subtotal: Double,
    val items: List<LoanItemDisplay>
)

data class AddEditDialogState(
    val isEdit: Boolean = false,
    val loanId: Long = 0,
    val name: String = "",
    val date: String = "",
    val amount: String = "",
    val status: String = "Repaid",
    val repaidDate: String = "",
    val nameError: String? = null,
    val amountError: String? = null
)

data class LoanUiState(
    val loans: List<LoanEntity> = emptyList(),
    val groupedLoans: List<BorrowerLoanGroup> = emptyList(),
    val grandTotal: Double = 0.0,
    val uniqueBorrowerNames: List<String> = emptyList(),
    val badgeViewStates: Map<Long, Boolean> = emptyMap(), // loanId -> true means show repayment date, false means show "Repaid"
    val dialogState: AddEditDialogState? = null,
    val deleteConfirmationLoan: LoanEntity? = null,
    val userMessage: String? = null,
    val isSuccessMessage: Boolean = true
)

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LoanRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = LoanRepository(database.loanDao())
    }

    private val _badgeViewStates = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    private val _dialogState = MutableStateFlow<AddEditDialogState?>(null)
    private val _deleteConfirmationLoan = MutableStateFlow<LoanEntity?>(null)
    private val _userMessage = MutableStateFlow<Pair<String?, Boolean>>(null to true)

    val uiState: StateFlow<LoanUiState> = combine(
        repository.allLoans,
        _badgeViewStates,
        _dialogState,
        _deleteConfirmationLoan,
        _userMessage
    ) { loans, badgeStates, dialogState, deleteLoan, messagePair ->

        // Group loans by borrower name (trimmed, case-insensitive sorting matching HTML)
        val groupedMap = loans.groupBy { it.name.trim() }
        val sortedNames = groupedMap.keys.sortedWith(String.CASE_INSENSITIVE_ORDER)

        var grandTotal = 0.0
        val groupedList = mutableListOf<BorrowerLoanGroup>()

        for (name in sortedNames) {
            val groupLoans = groupedMap[name] ?: emptyList()
            val subtotal = groupLoans.sumOf { it.amount }
            grandTotal += subtotal

            val itemDisplays = groupLoans.mapIndexed { index, loan ->
                LoanItemDisplay(
                    loan = loan,
                    isFirstInGroup = (index == 0),
                    groupSubtotal = subtotal
                )
            }

            groupedList.add(
                BorrowerLoanGroup(
                    borrowerName = name,
                    subtotal = subtotal,
                    items = itemDisplays
                )
            )
        }

        val uniqueNames = loans.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

        LoanUiState(
            loans = loans,
            groupedLoans = groupedList,
            grandTotal = grandTotal,
            uniqueBorrowerNames = uniqueNames,
            badgeViewStates = badgeStates,
            dialogState = dialogState,
            deleteConfirmationLoan = deleteLoan,
            userMessage = messagePair.first,
            isSuccessMessage = messagePair.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoanUiState()
    )

    fun toggleBadgeView(loanId: Long) {
        val current = _badgeViewStates.value
        val isCurrentlyShowingDate = current[loanId] ?: false
        _badgeViewStates.value = current + (loanId to !isCurrentlyShowingDate)
    }

    fun openAddModal() {
        val today = getTodayFormatted()
        _dialogState.value = AddEditDialogState(
            isEdit = false,
            name = "",
            date = today,
            amount = "",
            status = "Repaid",
            repaidDate = today
        )
    }

    fun openEditModal(loan: LoanEntity) {
        _dialogState.value = AddEditDialogState(
            isEdit = true,
            loanId = loan.id,
            name = loan.name,
            date = loan.date,
            amount = if (loan.amount % 1.0 == 0.0) loan.amount.toLong().toString() else loan.amount.toString(),
            status = loan.status,
            repaidDate = loan.repaidDate.ifEmpty { loan.date }
        )
    }

    fun updateDialogState(transform: (AddEditDialogState) -> AddEditDialogState) {
        _dialogState.value?.let { current ->
            _dialogState.value = transform(current)
        }
    }

    fun closeDialog() {
        _dialogState.value = null
    }

    fun saveDialogEntry() {
        val current = _dialogState.value ?: return

        val trimmedName = current.name.trim()
        if (trimmedName.isEmpty()) {
            _dialogState.value = current.copy(nameError = "Name is required")
            return
        }

        val amountVal = current.amount.toDoubleOrNull()
        if (amountVal == null || amountVal < 0) {
            _dialogState.value = current.copy(amountError = "Please enter a valid amount")
            return
        }

        val dateVal = current.date.ifEmpty { getTodayFormatted() }
        val repaidDateVal = if (current.status == "Repaid") {
            current.repaidDate.ifEmpty { dateVal }
        } else {
            ""
        }

        viewModelScope.launch {
            if (current.isEdit) {
                val updatedLoan = LoanEntity(
                    id = current.loanId,
                    name = trimmedName,
                    date = dateVal,
                    amount = amountVal,
                    status = current.status,
                    repaidDate = repaidDateVal
                )
                repository.updateLoan(updatedLoan)
                _userMessage.value = "Loan entry updated successfully" to true
            } else {
                val newLoan = LoanEntity(
                    name = trimmedName,
                    date = dateVal,
                    amount = amountVal,
                    status = current.status,
                    repaidDate = repaidDateVal
                )
                repository.insertLoan(newLoan)
                _userMessage.value = "New loan added successfully" to true
            }
            _dialogState.value = null
        }
    }

    fun promptDelete(loan: LoanEntity) {
        _deleteConfirmationLoan.value = loan
    }

    fun dismissDeletePrompt() {
        _deleteConfirmationLoan.value = null
    }

    fun confirmDelete() {
        val loan = _deleteConfirmationLoan.value ?: return
        viewModelScope.launch {
            repository.deleteLoan(loan)
            _badgeViewStates.value = _badgeViewStates.value - loan.id
            _deleteConfirmationLoan.value = null
            _userMessage.value = "Loan record deleted" to true
        }
    }

    suspend fun getExportJsonString(): String {
        return repository.exportToJson()
    }

    fun importFromJsonString(jsonString: String) {
        viewModelScope.launch {
            val result = repository.importFromJson(jsonString)
            if (result.isSuccess) {
                _badgeViewStates.value = emptyMap()
                _userMessage.value = "Data imported successfully! (${result.getOrNull()} records)" to true
            } else {
                _userMessage.value = "Error importing backup: ${result.exceptionOrNull()?.message ?: "Invalid JSON format"}" to false
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null to true
    }

    companion object {
        fun getTodayFormatted(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun formatDateToDDMMYYYY(dateString: String): String {
            if (dateString.isEmpty()) return ""
            val parts = dateString.split("-")
            return if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}"
            } else {
                dateString
            }
        }

        fun formatCurrency(amount: Double): String {
            val formatter = DecimalFormat("#,##0.00")
            return "৳" + formatter.format(amount)
        }
    }
}
