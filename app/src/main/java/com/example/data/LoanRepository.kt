package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class LoanRepository(private val loanDao: LoanDao) {
    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()

    suspend fun insertLoan(loan: LoanEntity): Long = loanDao.insertLoan(loan)

    suspend fun updateLoan(loan: LoanEntity) = loanDao.updateLoan(loan)

    suspend fun deleteLoan(loan: LoanEntity) = loanDao.deleteLoan(loan)

    suspend fun deleteLoanById(id: Long) = loanDao.deleteLoanById(id)

    suspend fun exportToJson(): String {
        val loans = loanDao.getAllLoansList()
        val jsonArray = JSONArray()
        for (loan in loans) {
            val obj = JSONObject().apply {
                put("name", loan.name)
                put("date", loan.date)
                put("amount", loan.amount)
                put("status", loan.status)
                put("repaidDate", loan.repaidDate)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Result<Int> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val importedList = mutableListOf<LoanEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                val date = obj.optString("date", "")
                val amount = obj.optDouble("amount", 0.0)
                val status = obj.optString("status", "Not-Paid")
                val repaidDate = obj.optString("repaidDate", "")

                importedList.add(
                    LoanEntity(
                        name = name,
                        date = date,
                        amount = amount,
                        status = status,
                        repaidDate = repaidDate
                    )
                )
            }
            if (importedList.isEmpty()) {
                Result.failure(Exception("No valid loan records found in backup file."))
            } else {
                loanDao.clearAllLoans()
                loanDao.insertLoans(importedList)
                Result.success(importedList.size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
