package com.shalltear.shallnotspend.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shallnotspend.model.AppPreferences
import com.shalltear.shallnotspend.model.DataRepository
import com.shalltear.shallnotspend.model.TransactionType
import com.shalltear.shallnotspend.ui.util.formatCurrency
import com.shalltear.shallnotspend.ui.util.formatSignedCurrency

@Composable
fun LendBorrowScreen(modifier: Modifier = Modifier) {
    var selectedPerson by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedPerson != null) {
        selectedPerson = null
    }
    
    AnimatedContent(
        targetState = selectedPerson,
        transitionSpec = {
            slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
        },
        label = "LendBorrow Transition"
    ) { person ->
        if (person == null) {
            DebtDashboard(
                modifier = modifier,
                onPersonClick = { selectedPerson = it }
            )
        } else {
            PersonDebtHistoryScreen(
                person = person,
                modifier = modifier,
                onBack = { selectedPerson = null }
            )
        }
    }
}

@Composable
fun DebtDashboard(modifier: Modifier = Modifier, onPersonClick: (String) -> Unit) {
    val allTransactions = DataRepository.transactions
    val debtTransactions = allTransactions.filter { 
        it.type in listOf(TransactionType.LEND, TransactionType.BORROW, TransactionType.LEND_RETURN, TransactionType.BORROW_RETURN) 
    }

    // Group by person
    val groupedByPerson = debtTransactions.filter { it.person.isNotBlank() }.groupBy { it.person }

    val personBalances = groupedByPerson.mapValues { (_, txs) ->
        val owesMe = txs.filter { it.type == TransactionType.LEND }.sumOf { it.amount } - txs.filter { it.type == TransactionType.LEND_RETURN }.sumOf { it.amount }
        val iOweThem = txs.filter { it.type == TransactionType.BORROW }.sumOf { it.amount } - txs.filter { it.type == TransactionType.BORROW_RETURN }.sumOf { it.amount }
        owesMe - iOweThem
    }

    val excludedPeople = AppPreferences.excludedDebtPeople
    val includedPersonBalances = personBalances.filterKeys { it !in excludedPeople }

    val totalOwedToMe = includedPersonBalances.values.filter { it > 0 }.sum()
    val totalIOwe = Math.abs(includedPersonBalances.values.filter { it < 0 }.sum())
    val netBalance = totalOwedToMe - totalIOwe

    var personUnderManagement by remember { mutableStateOf<String?>(null) }

    if (personUnderManagement != null) {
        AlertDialog(
            onDismissRequest = { personUnderManagement = null },
            title = { Text(personUnderManagement!!) },
            text = {
                Text(
                    if (AppPreferences.excludedDebtPeople.contains(personUnderManagement!!)) {
                        "This debt is excluded from total debt."
                    } else {
                        "This debt is included in total debt."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppPreferences.toggleDebtPersonExclusion(personUnderManagement!!)
                        personUnderManagement = null
                    }
                ) {
                    Text(
                        if (AppPreferences.excludedDebtPeople.contains(personUnderManagement!!)) {
                            "Include in Total Debt"
                        } else {
                            "Exclude from Total Debt"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { personUnderManagement = null }) { Text("Cancel") }
            }
        )
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Net Balance Header
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Net Debt Balance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                val color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                val prefix = if (netBalance >= 0) "+" else "-"
                Text(
                    text = formatSignedCurrency(Math.abs(netBalance), prefix),
                    color = color,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            com.shalltear.shallnotspend.ui.components.SummaryCard(
                title = "Owed to Me",
                amount = totalOwedToMe,
                color = MaterialTheme.colorScheme.primary,
                iconId = android.R.drawable.ic_menu_send,
                modifier = Modifier.weight(1f)
            )
            com.shalltear.shallnotspend.ui.components.SummaryCard(
                title = "I Owe",
                amount = totalIOwe,
                color = MaterialTheme.colorScheme.secondary,
                iconId = android.R.drawable.ic_menu_revert,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "People",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // People List
        if (personBalances.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active lend or borrow records.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(personBalances.entries.toList()) { (person, balance) ->
                    if (Math.abs(balance) > 0.01) { // Only show active debts
                        PersonDebtCard(
                            name = person,
                            balance = balance,
                            isExcluded = excludedPeople.contains(person),
                            onClick = { onPersonClick(person) },
                            onLongClick = { personUnderManagement = person }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PersonDebtCard(name: String, balance: Double, isExcluded: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isExcluded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_myplaces), // Using a generic person icon replacement
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val isOwedToMe = balance > 0
                val color = if (isOwedToMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                val statusText = if (isOwedToMe) "Owes me" else "I owe"
                
                Text(
                    text = formatCurrency(Math.abs(balance)),
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                if (isExcluded) {
                    Text(
                        text = "Excluded",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDebtHistoryScreen(person: String, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val transactions = DataRepository.transactions
        .filter { it.person == person && it.type in listOf(TransactionType.LEND, TransactionType.BORROW, TransactionType.LEND_RETURN, TransactionType.BORROW_RETURN) }
        .sortedByDescending { it.date }
    
    val owesMe = transactions.filter { it.type == TransactionType.LEND }.sumOf { it.amount } - transactions.filter { it.type == TransactionType.LEND_RETURN }.sumOf { it.amount }
    val iOweThem = transactions.filter { it.type == TransactionType.BORROW }.sumOf { it.amount } - transactions.filter { it.type == TransactionType.BORROW_RETURN }.sumOf { it.amount }
    val netBalance = owesMe - iOweThem

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(android.R.drawable.ic_menu_revert),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = person,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Balance card
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                val status = if (netBalance > 0) "Owes you" else if (netBalance < 0) "You owe" else "Settled"
                Text(text = status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatCurrency(Math.abs(netBalance)),
                    color = color,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Transaction History",
            modifier = Modifier.padding(horizontal = 24.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(transactions) { tx ->
                    com.shalltear.shallnotspend.ui.components.TransactionItem(transaction = tx)
                }
            }
        }
    }
}
