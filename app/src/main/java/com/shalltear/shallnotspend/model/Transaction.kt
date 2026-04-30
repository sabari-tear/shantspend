package com.shalltear.shallnotspend.model

import java.time.LocalDateTime
import java.util.UUID

enum class AccountType {
    REGULAR, MONTHLY_REFRESH
}

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val initialBalance: Double = 0.0
)

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER, LEND, BORROW, LEND_RETURN, BORROW_RETURN
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val date: LocalDateTime,
    val category: String,
    val iconId: Int,
    val accountId: String,
    var isReturned: Boolean = false,
    val person: String = ""
)

data class MonthArchive(
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val accountName: String,
    val label: String,
    val archivedAt: LocalDateTime,
    val transactions: List<Transaction>,
    val closingBalance: Double
)
