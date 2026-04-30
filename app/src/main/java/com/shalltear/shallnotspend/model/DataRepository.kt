package com.shalltear.shallnotspend.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    override fun serialize(src: LocalDateTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {
        return LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

object DataRepository {
    private const val PREFS_NAME = "shallnotspend_data"
    private const val KEY_ACCOUNTS = "accounts"
    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_ARCHIVES = "month_archives"

    private lateinit var prefs: SharedPreferences
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    val accounts = mutableStateListOf<Account>()
    val transactions = mutableStateListOf<Transaction>()
    val monthArchives = mutableStateListOf<MonthArchive>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()
        clearLegacySeedDataIfPresent()
    }

    private fun loadData() {
        accounts.clear()
        val accountsJson = prefs.getString(KEY_ACCOUNTS, null)
        if (accountsJson != null) {
            val type = object : TypeToken<List<Account>>() {}.type
            val loadedAccounts: List<Account> = gson.fromJson(accountsJson, type)
            accounts.addAll(loadedAccounts)
        } else {
            // First run: start with empty state and persist it.
            saveAccounts()
        }

        transactions.clear()
        val txJson = prefs.getString(KEY_TRANSACTIONS, null)
        if (txJson != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            val loadedTxs: List<Transaction> = gson.fromJson(txJson, type)
            transactions.addAll(loadedTxs)
        } else {
            // First run: start with empty state and persist it.
            saveTransactions()
        }

        monthArchives.clear()
        val archivesJson = prefs.getString(KEY_ARCHIVES, null)
        if (archivesJson != null) {
            val type = object : TypeToken<List<MonthArchive>>() {}.type
            val loadedArchives: List<MonthArchive> = gson.fromJson(archivesJson, type)
            monthArchives.addAll(loadedArchives)
        }
    }

    private fun clearLegacySeedDataIfPresent() {
        val isLegacyAccounts = accounts.size == 2 &&
            accounts.any { it.id == "1" && it.name == "Salary" && it.type == AccountType.MONTHLY_REFRESH && it.initialBalance == 0.0 } &&
            accounts.any { it.id == "2" && it.name == "Savings" && it.type == AccountType.REGULAR && it.initialBalance == 2000.0 }

        val isLegacyTransactions = transactions.size == 4 &&
            transactions.any { it.title == "Monthly Salary" && it.type == TransactionType.INCOME && it.accountId == "1" && it.amount == 5400.0 } &&
            transactions.any { it.title == "Groceries" && it.type == TransactionType.EXPENSE && it.accountId == "1" && it.amount == 120.50 } &&
            transactions.any { it.title == "Electricity Bill" && it.type == TransactionType.EXPENSE && it.accountId == "1" && it.amount == 85.00 } &&
            transactions.any { it.title == "Lunch" && it.type == TransactionType.LEND && it.accountId == "1" && it.amount == 25.00 && it.person == "Alice" }

        if (isLegacyAccounts && isLegacyTransactions) {
            accounts.clear()
            transactions.clear()
            saveAccounts()
            saveTransactions()
        }
    }

    fun saveAccounts() {
        prefs.edit().putString(KEY_ACCOUNTS, gson.toJson(accounts.toList())).apply()
    }

    fun saveTransactions() {
        prefs.edit().putString(KEY_TRANSACTIONS, gson.toJson(transactions.toList())).apply()
    }

    fun saveArchives() {
        prefs.edit().putString(KEY_ARCHIVES, gson.toJson(monthArchives.toList())).apply()
    }

    fun getTransactionsForAccount(accountId: String): List<Transaction> {
        return transactions.filter { it.accountId == accountId }
    }

    fun getMonthlyRefreshAmountForAccount(accountId: String): Double {
        val account = accounts.find { it.id == accountId } ?: return 0.0
        if (account.monthlyRefreshAmount > 0.0) {
            return account.monthlyRefreshAmount
        }

        val accountTransactions = transactions
            .filter { it.accountId == accountId }
            .sortedByDescending { it.date }

        val currentAmount = accountTransactions.firstOrNull {
            it.type == TransactionType.INCOME &&
                it.amount > 0.0 &&
                (
                    it.title == "Monthly Credit" ||
                        it.title == "Existing Amount" ||
                        it.title == "Monthly Refresh" ||
                        it.category == "Initial" ||
                        it.category == "Monthly Credit" ||
                        it.category == "Monthly Refresh"
                    )
        }?.amount

        val archivedAmount = monthArchives
            .filter { it.accountId == accountId }
            .sortedByDescending { it.archivedAt }
            .asSequence()
            .flatMap { archive -> archive.transactions.asSequence().sortedByDescending { it.date } }
            .firstOrNull {
                it.type == TransactionType.INCOME &&
                    it.amount > 0.0 &&
                    (
                        it.title == "Monthly Credit" ||
                            it.title == "Existing Amount" ||
                            it.title == "Monthly Refresh" ||
                            it.category == "Initial" ||
                            it.category == "Monthly Credit" ||
                            it.category == "Monthly Refresh"
                        )
            }?.amount

        val resolvedAmount = currentAmount ?: archivedAmount ?: 0.0
        if (resolvedAmount > 0.0) {
            updateAccount(account.copy(monthlyRefreshAmount = resolvedAmount))
        }
        return resolvedAmount
    }

    fun archiveMonthForAccount(
        accountId: String,
        closingBalance: Double,
        appendedTransactions: List<Transaction> = emptyList(),
        endedAt: LocalDateTime = LocalDateTime.now()
    ) {
        val account = accounts.find { it.id == accountId } ?: return
        val txsToArchive = transactions.filter { it.accountId == accountId }
        val archiveTransactions = (txsToArchive + appendedTransactions).sortedBy { it.date }
        if (archiveTransactions.isEmpty()) return

        val periodStart = archiveTransactions.minByOrNull { it.date }?.date ?: endedAt
        val label = buildString {
            append(periodStart.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())))
            append(" - ")
            append(endedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())))
        }

        monthArchives.add(
            MonthArchive(
                accountId = accountId,
                accountName = account.name,
                label = label,
                archivedAt = endedAt,
                transactions = archiveTransactions,
                closingBalance = closingBalance,
                periodStart = periodStart,
                periodEnd = endedAt
            )
        )
        transactions.removeAll { it.accountId == accountId }
        saveArchives()
        saveTransactions()
    }

    fun addAccount(account: Account) {
        accounts.add(account)
        saveAccounts()
    }

    fun updateAccount(account: Account) {
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index != -1) {
            accounts[index] = account
            saveAccounts()
        }
    }

    fun deleteAccount(accountId: String) {
        accounts.removeAll { it.id == accountId }
        transactions.removeAll { it.accountId == accountId }
        monthArchives.removeAll { it.accountId == accountId }
        saveAccounts()
        saveTransactions()
        saveArchives()
    }

    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
        saveTransactions()
    }

    fun updateTransaction(transaction: Transaction) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            saveTransactions()
        }
    }

    fun deleteTransaction(transactionId: String) {
        transactions.removeAll { it.id == transactionId }
        saveTransactions()
    }

    fun getBalanceForAccount(accountId: String): Double {
        val account = accounts.find { it.id == accountId } ?: return 0.0
        val txs = transactions.filter { it.accountId == accountId }
        val income = txs.filter { it.type in listOf(TransactionType.INCOME, TransactionType.TRANSFER, TransactionType.LEND_RETURN, TransactionType.BORROW) }.sumOf { it.amount }
        val expense = txs.filter { it.type in listOf(TransactionType.EXPENSE, TransactionType.LEND, TransactionType.BORROW_RETURN) }.sumOf { it.amount }
        return account.initialBalance + income - expense
    }
}
