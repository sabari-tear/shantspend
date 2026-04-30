package com.shalltear.shallnotspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shallnotspend.model.Account
import com.shalltear.shallnotspend.model.AccountType
import com.shalltear.shallnotspend.model.AppPreferences
import com.shalltear.shallnotspend.model.DataRepository
import com.shalltear.shallnotspend.model.TransactionType
import com.shalltear.shallnotspend.ui.screens.AccountDetailScreen
import com.shalltear.shallnotspend.ui.screens.DashboardScreen
import com.shalltear.shallnotspend.ui.theme.ShantSpendTheme
import java.time.LocalDateTime
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataRepository.init(this)
        AppPreferences.init(this)
        enableEdgeToEdge()
        setContent {
            ShantSpendTheme(
                dynamicColor = false,
                palette = AppPreferences.selectedTheme
            ) {
                ShantSpendApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun ShantSpendApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var selectedAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var accountUnderManagement by remember { mutableStateOf<Account?>(null) }
    var accountPendingDeletion by remember { mutableStateOf<Account?>(null) }

    BackHandler(enabled = selectedAccountId != null) {
        selectedAccountId = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (selectedAccountId == null && currentDestination == AppDestinations.HOME) {
                FluidFab(onClick = { showAddAccountSheet = true }, iconId = android.R.drawable.ic_menu_add)
            } else if (selectedAccountId != null && currentDestination == AppDestinations.HOME) {
                FluidFab(onClick = { showAddTransactionSheet = true }, iconId = android.R.drawable.ic_input_add)
            } else if (currentDestination == AppDestinations.FAVORITES) {
                FluidFab(onClick = { showAddTransactionSheet = true }, iconId = android.R.drawable.ic_menu_sort_by_size)
            }
        },
        bottomBar = {
            if (selectedAccountId == null) {
                ShantBottomBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = {
                        currentDestination = it
                        selectedAccountId = null
                    }
                )
            }
        }
    ) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> {
                    if (selectedAccountId == null) {
                        DashboardScreen(
                            modifier = Modifier.padding(innerPadding),
                            onAccountClick = { selectedAccountId = it },
                            onAccountLongClick = { accountUnderManagement = it }
                        )
                    } else {
                        AccountDetailScreen(
                            accountId = selectedAccountId!!,
                            modifier = Modifier.padding(innerPadding),
                            onBack = { selectedAccountId = null },
                            onEditAccount = {
                                accountUnderManagement = it
                                showAddAccountSheet = true
                            },
                            onDeleteAccount = {
                                accountPendingDeletion = it
                            }
                        )
                    }
                }
                AppDestinations.FAVORITES -> {
                    com.shalltear.shallnotspend.ui.screens.LendBorrowScreen(modifier = Modifier.padding(innerPadding))
                }
                AppDestinations.PROFILE -> com.shalltear.shallnotspend.ui.screens.ProfileScreen(modifier = Modifier.padding(innerPadding))
            }
            
            if (showAddTransactionSheet && currentDestination == AppDestinations.HOME && selectedAccountId != null) {
                ModalBottomSheet(
                    onDismissRequest = { showAddTransactionSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AddTransactionContent(
                        accountId = selectedAccountId!!,
                        onDismiss = { showAddTransactionSheet = false }
                    )
                }
            } else if (showAddTransactionSheet && currentDestination == AppDestinations.FAVORITES) {
                ModalBottomSheet(
                    onDismissRequest = { showAddTransactionSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AddDebtContent(
                        onDismiss = { showAddTransactionSheet = false }
                    )
                }
            }

            if (showAddAccountSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showAddAccountSheet = false
                        accountUnderManagement = null
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AddAccountContent(
                        existingAccount = accountUnderManagement,
                        onDismiss = {
                            showAddAccountSheet = false
                            accountUnderManagement = null
                        }
                    )
                }
            }

            if (accountUnderManagement != null && !showAddAccountSheet) {
                ModalBottomSheet(
                    onDismissRequest = { accountUnderManagement = null },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AccountOptionsContent(
                        account = accountUnderManagement!!,
                        onEdit = { showAddAccountSheet = true },
                        onDelete = {
                            accountPendingDeletion = accountUnderManagement
                            accountUnderManagement = null
                        },
                        onDismiss = { accountUnderManagement = null }
                    )
                }
            }

            if (accountPendingDeletion != null) {
                AlertDialog(
                    onDismissRequest = { accountPendingDeletion = null },
                    title = { Text("Delete Account") },
                    text = { Text("Delete ${accountPendingDeletion!!.name} and all its transactions and archived history?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (selectedAccountId == accountPendingDeletion!!.id) {
                                    selectedAccountId = null
                                }
                                DataRepository.deleteAccount(accountPendingDeletion!!.id)
                                accountPendingDeletion = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { accountPendingDeletion = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
    }
}

@Composable
fun ShantBottomBar(
    currentDestination: AppDestinations,
    onDestinationSelected: (AppDestinations) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppDestinations.entries.forEach { destination ->
                    val selected = destination == currentDestination
                    Surface(
                        onClick = { onDestinationSelected(destination) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) {
                            if (destination == AppDestinations.FAVORITES) MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        color = if (selected) {
                                            if (destination == AppDestinations.FAVORITES) MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(destination.icon),
                                    contentDescription = destination.label,
                                    tint = if (selected) {
                                        if (destination == AppDestinations.FAVORITES) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = destination.label,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtContent(onDismiss: () -> Unit) {
    var personName by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(DataRepository.accounts.firstOrNull()) }
    var isLend by remember { mutableStateOf(true) }
    var personExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    val existingPeople = remember {
        DataRepository.transactions
            .map { it.person }
            .filter { it.isNotBlank() }
            .distinct()
    }
    
    val filteredPeople = existingPeople.filter { it.contains(personName, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Lend / Borrow", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isLend,
                onClick = { isLend = true },
                label = { Text("I Lent Money", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = !isLend,
                onClick = { isLend = false },
                label = { Text("I Borrowed Money", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        ExposedDropdownMenuBox(
            expanded = personExpanded,
            onExpandedChange = { personExpanded = it }
        ) {
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    personExpanded = true
                },
                label = { Text(if (isLend) "Who did you lend to?" else "Who did you borrow from?") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true
            )

            if (filteredPeople.isNotEmpty() && personExpanded) {
                ExposedDropdownMenu(
                    expanded = personExpanded,
                    onDismissRequest = { personExpanded = false }
                ) {
                    filteredPeople.forEach { person ->
                        DropdownMenuItem(
                            text = { Text(person) },
                            onClick = {
                                personName = person
                                personExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount ($)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = accountExpanded,
            onExpandedChange = { accountExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedAccount?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Account") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = accountExpanded,
                onDismissRequest = { accountExpanded = false }
            ) {
                DataRepository.accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            selectedAccount = account
                            accountExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                val expenseAmount = amount.toDoubleOrNull() ?: 0.0
                if (expenseAmount > 0 && personName.isNotBlank() && selectedAccount != null) {
                    val type = if (isLend) TransactionType.LEND else TransactionType.BORROW
                    val icon = if (isLend) android.R.drawable.ic_menu_send else android.R.drawable.ic_menu_revert
                    DataRepository.addTransaction(
                        com.shalltear.shallnotspend.model.Transaction(
                            title = if (reason.isNotBlank()) reason else personName,
                            amount = expenseAmount,
                            type = type,
                            date = LocalDateTime.now(),
                            category = type.name,
                            iconId = icon,
                            accountId = selectedAccount!!.id,
                            person = personName
                        )
                    )
                }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Save Debt", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddAccountContent(existingAccount: Account? = null, onDismiss: () -> Unit) {
    var name by remember(existingAccount) { mutableStateOf(existingAccount?.name ?: "") }
    var selectedType by remember(existingAccount) { mutableStateOf(existingAccount?.type ?: AccountType.REGULAR) }
    var initialBalance by remember { mutableStateOf("") }
    val isEditing = existingAccount != null
    val balanceLabel = if (selectedType == AccountType.MONTHLY_REFRESH) "Starting month balance ($)" else "Initial Balance ($)"
    val helperTitle = if (selectedType == AccountType.MONTHLY_REFRESH) "Monthly Refresh Account" else "Regular Account"
    val helperBody = if (selectedType == AccountType.MONTHLY_REFRESH) {
        "Use this for accounts like salary. At month end, you can archive the current month and optionally roll the remaining amount into another account."
    } else {
        "Use this for long-term balances like savings, cash, or sinking funds. Transactions stay visible until you change them."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (isEditing) "Edit Account" else "Create New Account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Account Name (e.g. Vacation Fund)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (!isEditing) {
            OutlinedTextField(
                value = initialBalance,
                onValueChange = { initialBalance = it },
                label = { Text(balanceLabel) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == AccountType.REGULAR,
                onClick = { selectedType = AccountType.REGULAR },
                label = {
                    Text(
                        "Regular",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedType == AccountType.REGULAR,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                )
            )
            FilterChip(
                selected = selectedType == AccountType.MONTHLY_REFRESH,
                onClick = { selectedType = AccountType.MONTHLY_REFRESH },
                label = {
                    Text(
                        "Monthly",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedType == AccountType.MONTHLY_REFRESH,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (selectedType == AccountType.MONTHLY_REFRESH) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = helperTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = helperBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    if (existingAccount != null) {
                        DataRepository.updateAccount(
                            existingAccount.copy(
                                name = name,
                                type = selectedType
                            )
                        )
                    } else {
                        val parsedInitialBalance = initialBalance.toDoubleOrNull() ?: 0.0
                        val newAccountId = UUID.randomUUID().toString()

                        DataRepository.addAccount(
                            Account(
                                id = newAccountId,
                                name = name,
                                type = selectedType,
                                initialBalance = 0.0
                            )
                        )

                        if (parsedInitialBalance != 0.0) {
                            val txType = if (parsedInitialBalance > 0) TransactionType.INCOME else TransactionType.EXPENSE
                            DataRepository.addTransaction(
                                com.shalltear.shallnotspend.model.Transaction(
                                    title = "Existing Amount",
                                    amount = kotlin.math.abs(parsedInitialBalance),
                                    type = txType,
                                    date = LocalDateTime.now(),
                                    category = "Initial",
                                    iconId = android.R.drawable.ic_menu_info_details,
                                    accountId = newAccountId
                                )
                            )
                        }
                    }
                }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isEditing) "Save Changes" else "Create Account", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AccountOptionsContent(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(account.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Choose an action", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Edit Account")
        }

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Account")
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
fun AddTransactionContent(accountId: String, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    
    val accountName = DataRepository.accounts.find { it.id == accountId }?.name ?: "Account"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add to $accountName", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf(TransactionType.INCOME, TransactionType.EXPENSE)
            val labels = listOf("Income", "Expense")
            types.forEachIndexed { index, type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(labels[index], modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        val titleLabel = if (selectedType == TransactionType.EXPENSE) "What did you buy?" else "Where from?"
        
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(titleLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount ($)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                val expenseAmount = amount.toDoubleOrNull() ?: 0.0
                if (expenseAmount > 0 && title.isNotBlank()) {
                    val icon = if (selectedType == TransactionType.INCOME) android.R.drawable.ic_menu_save else android.R.drawable.ic_menu_edit
                    DataRepository.addTransaction(
                        com.shalltear.shallnotspend.model.Transaction(
                            title = title,
                            amount = expenseAmount,
                            type = selectedType,
                            date = LocalDateTime.now(),
                            category = selectedType.name,
                            iconId = icon,
                            accountId = accountId
                        )
                    )
                }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Save Transaction", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun FluidFab(onClick: () -> Unit, iconId: Int) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isPressed) 90f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabRotation"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = interactionSource
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = "Add",
            modifier = Modifier.rotate(rotation)
        )
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", android.R.drawable.ic_menu_myplaces),
    FAVORITES("Debts", android.R.drawable.ic_menu_sort_by_size),
    PROFILE("Profile", android.R.drawable.ic_menu_info_details),
}