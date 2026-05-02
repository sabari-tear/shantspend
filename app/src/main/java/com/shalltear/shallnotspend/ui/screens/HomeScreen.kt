package com.shalltear.shallnotspend.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shallnotspend.model.Account
import com.shalltear.shallnotspend.model.AppPreferences
import com.shalltear.shallnotspend.model.DataRepository
import com.shalltear.shallnotspend.model.TransactionType
import com.shalltear.shallnotspend.ui.util.formatCurrency

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onAccountClick: (String) -> Unit,
    onAccountLongClick: (Account) -> Unit
) {
    val accounts = DataRepository.accounts
    val totalWealth = accounts.filter { !it.excludeFromWealth }.sumOf { DataRepository.getBalanceForAccount(it.id) }

    val allTxs = DataRepository.transactions
    val groupedByPerson = allTxs.filter {
        it.type in listOf(TransactionType.LEND, TransactionType.BORROW, TransactionType.LEND_RETURN, TransactionType.BORROW_RETURN) && it.person.isNotBlank()
    }.groupBy { it.person }
    val personBalances = groupedByPerson.mapValues { (_, txs) ->
        val owesMe = txs.filter { it.type == TransactionType.LEND }.sumOf { it.amount } - txs.filter { it.type == TransactionType.LEND_RETURN }.sumOf { it.amount }
        val iOwe = txs.filter { it.type == TransactionType.BORROW }.sumOf { it.amount } - txs.filter { it.type == TransactionType.BORROW_RETURN }.sumOf { it.amount }
        owesMe - iOwe
    }
    val netDebt = personBalances
        .filterKeys { it !in AppPreferences.excludedDebtPeople }
        .values
        .sum()

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Total Wealth Header
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Total Wealth",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatCurrency(totalWealth),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (netDebt != 0.0) {
                        val debtColor = if (netDebt > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                        val debtSign = if (netDebt > 0) "+" else "-"
                        Text(
                            text = "$debtSign${formatCurrency(Math.abs(netDebt))}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = debtColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Your Accounts",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No accounts yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tap + to add your first account", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        } else {
            // Accounts List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(accounts) { account ->
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        ) + fadeIn()
                    ) {
                        AccountCard(
                            account = account,
                            balance = DataRepository.getBalanceForAccount(account.id),
                            onClick = { onAccountClick(account.id) },
                            onLongClick = { onAccountLongClick(account) }
                        )
                    }
                }
            }
        } // end else
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AccountCard(account: Account, balance: Double, onClick: () -> Unit, onLongClick: () -> Unit) {
    val scale = remember { Animatable(0.95f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics { contentDescription = "${account.name} account, balance ${formatCurrency(balance)}" },
        shape = RoundedCornerShape(20.dp),
        color = if (account.excludeFromWealth) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_myplaces),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = account.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = account.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    if (account.excludeFromWealth) {
                        Text(
                            text = "Excluded from total",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            Text(
                text = formatCurrency(balance),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
