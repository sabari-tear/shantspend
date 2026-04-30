package com.shalltear.shallnotspend.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shallnotspend.model.DataRepository
import com.shalltear.shallnotspend.model.TransactionType
import com.shalltear.shallnotspend.ui.components.TransactionItem
import com.shalltear.shallnotspend.ui.components.SummaryCard
import com.shalltear.shallnotspend.ui.components.MonthEndDialog
import java.time.LocalDateTime

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
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    val transferTargets = DataRepository.accounts.filter { it.id != accountId }

    if (showMonthEndDialog) {
        MonthEndDialog(
            salaryBalance = balance,
            transferTargets = transferTargets,
            onDismiss = { showMonthEndDialog = false },
            onConfirm = { action, targetAccountId ->
                // Archive current month's transactions before clearing
                DataRepository.archiveMonthForAccount(accountId, balance)

                if (action == "TRANSFER" && targetAccountId != null) {
                    val targetAccount = DataRepository.accounts.find { it.id == targetAccountId }
                    DataRepository.addTransaction(
                        com.shalltear.shallnotspend.model.Transaction(
                            title = "Rollover from ${account.name}",
                            amount = balance,
                            type = TransactionType.INCOME,
                            date = LocalDateTime.now(),
                            category = "Transfer",
                            iconId = android.R.drawable.ic_menu_revert,
                            accountId = targetAccountId
                        )
                    )
                }
                showMonthEndDialog = false
            }
        )
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
                        text = "$${String.format("%.2f", targetBalance)}",
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
                    TransactionItem(transaction = transaction)
                }
            }
        }
    }
}
