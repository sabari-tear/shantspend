package com.shalltear.shallnotspend.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shallnotspend.model.DataRepository
import com.shalltear.shallnotspend.model.Transaction
import com.shalltear.shallnotspend.model.TransactionType
import com.shalltear.shallnotspend.ui.util.formatSignedCurrency
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(transaction: Transaction, onLongClick: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                val isSecretIncome = transaction.type == TransactionType.INCOME && transaction.category == "SECRET_INCOME"
                val iconTint = when (transaction.type) {
                    TransactionType.INCOME -> if (isSecretIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    TransactionType.LEND_RETURN, TransactionType.BORROW -> MaterialTheme.colorScheme.primary
                    TransactionType.EXPENSE, TransactionType.LEND, TransactionType.BORROW_RETURN -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = transaction.iconId),
                        contentDescription = when (transaction.type) {
                            TransactionType.INCOME -> if (isSecretIncome) "Secret income" else "Income"
                            TransactionType.EXPENSE -> "Expense"
                            TransactionType.LEND -> "Lent money"
                            TransactionType.BORROW -> "Borrowed money"
                            TransactionType.LEND_RETURN -> "Lend returned"
                            TransactionType.BORROW_RETURN -> "Borrow paid back"
                            TransactionType.TRANSFER -> "Transfer"
                            else -> "Transaction"
                        },
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val titlePrefix = when (transaction.type) {
                        TransactionType.LEND -> "Lent to "
                        TransactionType.BORROW -> "Borrowed from "
                        TransactionType.LEND_RETURN -> "Returned by "
                        TransactionType.BORROW_RETURN -> "Paid back "
                        else -> ""
                    }
                    val displayTitle = if (transaction.type in listOf(TransactionType.LEND, TransactionType.BORROW, TransactionType.LEND_RETURN, TransactionType.BORROW_RETURN) && transaction.person.isNotBlank() && transaction.person != transaction.title) {
                        "${transaction.person} (${transaction.title})"
                    } else {
                        transaction.title
                    }
                    Text(
                        text = "$titlePrefix$displayTitle",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm")),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Amount
                val isPositive = transaction.type in listOf(TransactionType.INCOME, TransactionType.TRANSFER, TransactionType.LEND_RETURN, TransactionType.BORROW)
                val amountColor = if (isPositive) {
                    if (isSecretIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
                val sign = if (isPositive) "+" else "-"

                Text(
                    text = formatSignedCurrency(transaction.amount, sign),
                    color = amountColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Action Buttons for Lend/Borrow
            if (!transaction.isReturned && (transaction.type == TransactionType.LEND || transaction.type == TransactionType.BORROW)) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // Mark original as returned via immutable copy
                        DataRepository.updateTransaction(transaction.copy(isReturned = true))

                        // Create return transaction
                        val returnType = if (transaction.type == TransactionType.LEND) TransactionType.LEND_RETURN else TransactionType.BORROW_RETURN
                        val returnIcon = if (transaction.type == TransactionType.LEND) android.R.drawable.ic_menu_revert else android.R.drawable.ic_menu_send
                        
                        DataRepository.addTransaction(
                            Transaction(
                                title = transaction.title,
                                amount = transaction.amount,
                                type = returnType,
                                date = LocalDateTime.now(),
                                category = returnType.name,
                                iconId = returnIcon,
                                accountId = transaction.accountId,
                                person = transaction.person
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (transaction.type == TransactionType.LEND) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        contentColor = if (transaction.type == TransactionType.LEND) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(if (transaction.type == TransactionType.LEND) "Mark as Returned" else "Pay Back")
                }
            } else if (transaction.isReturned && (transaction.type == TransactionType.LEND || transaction.type == TransactionType.BORROW)) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Settled",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
