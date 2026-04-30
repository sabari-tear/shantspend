package com.shalltear.shallnotspend.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shallnotspend.model.DataRepository
import com.shalltear.shallnotspend.model.Transaction
import com.shalltear.shallnotspend.model.TransactionType
import com.shalltear.shallnotspend.ui.components.TransactionItem
import com.shalltear.shallnotspend.ui.components.SummaryCard
import com.shalltear.shallnotspend.ui.components.MonthEndDialog
import com.shalltear.shallnotspend.ui.util.formatCurrency
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onEditAccount: (com.shalltear.shallnotspend.model.Account) -> Unit,
    onDeleteAccount: (com.shalltear.shallnotspend.model.Account) -> Unit
) {
    val account = DataRepository.accounts.find { it.id == accountId } ?: return
    val transactions = DataRepository.transactions.filter { it.accountId == accountId }
    val balance = DataRepository.getBalanceForAccount(accountId)
    val income = transactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.TRANSFER }.sumOf { it.amount }
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    var showMonthEndDialog by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var transactionUnderManagement by remember { mutableStateOf<Transaction?>(null) }
    var transactionPendingDeletion by remember { mutableStateOf<Transaction?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    val transferTargets = DataRepository.accounts.filter { it.id != accountId }

    if (showMonthEndDialog) {
        MonthEndDialog(
            salaryBalance = balance,
            transferTargets = transferTargets,
            onDismiss = { showMonthEndDialog = false },
            onConfirm = { action, targetAccountId ->
                val monthEndAt = LocalDateTime.now()
                val isTransfer = action == "TRANSFER" && targetAccountId != null
                val transferOutTransaction = if (action == "TRANSFER" && targetAccountId != null && balance > 0.0) {
                    Transaction(
                        title = "Transfer to ${DataRepository.accounts.find { it.id == targetAccountId }?.name ?: "Selected Account"}",
                        amount = balance,
                        type = TransactionType.EXPENSE,
                        date = monthEndAt,
                        category = "Transfer",
                        iconId = android.R.drawable.ic_menu_send,
                        accountId = accountId
                    )
                } else {
                    null
                }

                val nextMonthRefreshAmount = DataRepository.getMonthlyRefreshAmountForAccount(accountId)
                val carryForwardAmount = if (isTransfer) 0.0 else balance

                DataRepository.archiveMonthForAccount(
                    accountId = accountId,
                    closingBalance = balance,
                    appendedTransactions = listOfNotNull(transferOutTransaction),
                    endedAt = monthEndAt
                )

                if (action == "TRANSFER" && targetAccountId != null) {
                    DataRepository.addTransaction(
                        Transaction(
                            title = "Rollover from ${account.name}",
                            amount = balance,
                            type = TransactionType.INCOME,
                            date = monthEndAt,
                            category = "Transfer",
                            iconId = android.R.drawable.ic_menu_revert,
                            accountId = targetAccountId
                        )
                    )
                }

                DataRepository.addTransaction(
                    Transaction(
                        title = "Monthly Refresh",
                        amount = carryForwardAmount,
                        type = TransactionType.INCOME,
                        date = monthEndAt,
                        category = "Monthly Refresh",
                        iconId = android.R.drawable.ic_menu_my_calendar,
                        accountId = accountId
                    )
                )

                DataRepository.addTransaction(
                    Transaction(
                        title = "Monthly Credit",
                        amount = nextMonthRefreshAmount,
                        type = TransactionType.INCOME,
                        date = monthEndAt,
                        category = "Monthly Credit",
                        iconId = android.R.drawable.ic_input_add,
                        accountId = accountId
                    )
                )

                showMonthEndDialog = false
            }
        )
    }

    if (transactionUnderManagement != null) {
        ModalBottomSheet(
            onDismissRequest = { transactionUnderManagement = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TransactionOptionsContent(
                transaction = transactionUnderManagement!!,
                onEdit = {
                    transactionUnderManagement = transactionUnderManagement!!.copy()
                },
                onDelete = {
                    transactionPendingDeletion = transactionUnderManagement
                    transactionUnderManagement = null
                },
                onDismiss = { transactionUnderManagement = null }
            )
        }
    }

    if (transactionPendingDeletion != null) {
        AlertDialog(
            onDismissRequest = { transactionPendingDeletion = null },
            title = { Text("Delete Entry") },
            text = { Text("Delete ${transactionPendingDeletion!!.title}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        DataRepository.deleteTransaction(transactionPendingDeletion!!.id)
                        transactionPendingDeletion = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { transactionPendingDeletion = null }) { Text("Cancel") }
            }
        )
    }

    val editingTransaction = transactionUnderManagement?.takeIf {
        it.type == TransactionType.INCOME || it.type == TransactionType.EXPENSE
    }

    if (editingTransaction != null && (editingTransaction.type == TransactionType.INCOME || editingTransaction.type == TransactionType.EXPENSE) && false) {
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_revert),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = account.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box {
                IconButton(onClick = { showAccountMenu = true }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_more),
                        contentDescription = "Account options",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                DropdownMenu(
                    expanded = showAccountMenu,
                    onDismissRequest = { showAccountMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Account") },
                        onClick = {
                            showAccountMenu = false
                            onEditAccount(account)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showAccountMenu = false
                            onDeleteAccount(account)
                        }
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // Balance
            AnimatedContent(
                targetState = balance,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith slideOutVertically { height -> -height } + fadeOut()
                }, label = "balanceAnimation"
            ) { targetBalance ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Available Balance",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatCurrency(targetBalance),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "Income",
                    amount = income,
                    color = MaterialTheme.colorScheme.primary,
                    iconId = android.R.drawable.arrow_up_float,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Expense",
                    amount = expense,
                    color = MaterialTheme.colorScheme.secondary,
                    iconId = android.R.drawable.arrow_down_float,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (account.type == com.shalltear.shallnotspend.model.AccountType.MONTHLY_REFRESH) {
                    TextButton(onClick = { showMonthEndDialog = true }) {
                        Text("Month End", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            itemsIndexed(transactions.reversed()) { index, transaction ->
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(tween(delayMillis = 100 + (index * 50)))
                ) {
                    TransactionItem(
                        transaction = transaction,
                        onLongClick = if (transaction.type == TransactionType.INCOME || transaction.type == TransactionType.EXPENSE) {
                            { transactionUnderManagement = transaction }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionOptionsContent(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showEditSheet by remember { mutableStateOf(false) }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showEditSheet = false
                onDismiss()
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            com.shalltear.shallnotspend.AddTransactionContent(
                accountId = transaction.accountId,
                existingTransaction = transaction,
                onDismiss = {
                    showEditSheet = false
                    onDismiss()
                }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(transaction.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Choose an action", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = {
                    onEdit()
                    showEditSheet = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Edit Entry")
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Entry")
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}
