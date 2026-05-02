package com.shalltear.shallnotspend.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.shalltear.shallnotspend.model.AppPreferences
import com.shalltear.shallnotspend.model.CurrencyType
import com.shalltear.shallnotspend.model.DataRepository
import com.shalltear.shallnotspend.model.ImportPreview
import com.shalltear.shallnotspend.model.MonthArchive
import com.shalltear.shallnotspend.model.ThemeMode
import com.shalltear.shallnotspend.model.ThemePalette
import com.shalltear.shallnotspend.model.TransactionType
import com.shalltear.shallnotspend.ui.components.TransactionItem
import com.shalltear.shallnotspend.ui.util.formatCurrency
import com.shalltear.shallnotspend.ui.util.formatSignedCurrency
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private enum class ProfilePage {
    HOME,
    SETTINGS,
    THEME,
    CURRENCY,
    DATA_MANAGEMENT,
    HISTORY
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    var page by remember { mutableStateOf(ProfilePage.HOME) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var profileName by remember(AppPreferences.profileName) { mutableStateOf(AppPreferences.profileName) }
    var profileBio by remember(AppPreferences.profileBio) { mutableStateOf(AppPreferences.profileBio) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
    val scaffoldModifier = modifier.padding(scaffoldPadding)
    when (page) {
        ProfilePage.HOME -> {
            LazyColumn(
                modifier = scaffoldModifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // ── Top bar ──────────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 48.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    profileName = AppPreferences.profileName
                                    profileBio = AppPreferences.profileBio
                                    isEditingProfile = true
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_input_add),
                                    contentDescription = "Edit profile",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // ── Avatar + name ─────────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Avatar circle
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (AppPreferences.profileName.ifBlank { "Y" }).take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AppPreferences.profileName,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = AppPreferences.profileBio,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                // ── Edit profile inline (when editing) ───────────────────────
                if (isEditingProfile) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Edit Profile", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { profileName = it },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = profileBio,
                                    onValueChange = { profileBio = it },
                                    label = { Text("Bio") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 3
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { isEditingProfile = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Cancel") }
                                    Button(
                                        onClick = {
                                            AppPreferences.updateProfile(
                                                name = profileName.trim().ifBlank { "Your Name" },
                                                bio = profileBio.trim().ifBlank { "Tell something about yourself." }
                                            )
                                            isEditingProfile = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Save") }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── Menu rows ─────────────────────────────────────────────────
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {

                            ProfileMenuRow(
                                title = "Monthly History",
                                subtitle = "View archived records",
                                iconLabel = "H",
                                iconBgColor = Color(0xFFFFB74D),
                                trailingContent = { MenuChevron() },
                                onClick = { page = ProfilePage.HISTORY }
                            )

                            MenuDivider()

                            ProfileMenuRow(
                                title = "App Settings",
                                subtitle = "Theme, currency & more",
                                iconLabel = "S",
                                iconBgColor = MaterialTheme.colorScheme.primary,
                                trailingContent = { MenuChevron() },
                                onClick = { page = ProfilePage.SETTINGS }
                            )

                            MenuDivider()

                            ProfileMenuRow(
                                title = "Data Management",
                                subtitle = "Backup & restore",
                                iconLabel = "D",
                                iconBgColor = Color(0xFF4CAF50),
                                trailingContent = { MenuChevron() },
                                onClick = { page = ProfilePage.DATA_MANAGEMENT }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        ProfilePage.SETTINGS -> {
            Column(
                modifier = scaffoldModifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page = ProfilePage.HOME }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_revert),
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsOptionCard(
                    title = "Theme",
                    subtitle = "Change app color palette",
                    onClick = { page = ProfilePage.THEME }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingsOptionCard(
                    title = "Currency",
                    subtitle = "Choose currency type",
                    onClick = { page = ProfilePage.CURRENCY }
                )

                Spacer(modifier = Modifier.height(24.dp))

                var showClearConfirm by remember { mutableStateOf(false) }

                if (showClearConfirm) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showClearConfirm = false },
                        title = { Text("Clear All Data") },
                        text = { Text("This will permanently delete all accounts, transactions, and history. You will have a few seconds to undo.") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    val snapshot = DataRepository.snapshotForUndo()
                                    DataRepository.clearAllData()
                                    AppPreferences.clearFinancialPreferences()
                                    showClearConfirm = false
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "All data cleared",
                                            actionLabel = "Undo",
                                            duration = androidx.compose.material3.SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            DataRepository.restoreFromSnapshot(snapshot)
                                        }
                                    }
                                }
                            ) { Text("Clear Everything", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                        }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth().clickable { showClearConfirm = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear All Data", color = MaterialTheme.colorScheme.error, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Permanently delete everything", color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_delete),
                            contentDescription = "Clear all data",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        ProfilePage.THEME -> {
            Column(
                modifier = scaffoldModifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page = ProfilePage.SETTINGS }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_revert),
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Theme", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Appearance",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        ThemeModeSelectionCard(
                            selectedMode = AppPreferences.selectedThemeMode,
                            onSelect = { AppPreferences.setThemeMode(it) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Pick your palette",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        ThemePalette.entries.forEach { palette ->
                            ThemeSelectionCard(
                                palette = palette,
                                selected = AppPreferences.selectedTheme == palette,
                                onClick = { AppPreferences.setTheme(palette) }
                            )
                        }
                    }
                }
            }
        }

        ProfilePage.HISTORY -> {
            MonthlyHistoryFullPage(
                modifier = scaffoldModifier,
                onBack = { page = ProfilePage.HOME }
            )
        }

        ProfilePage.CURRENCY -> {
            Column(
                modifier = scaffoldModifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page = ProfilePage.SETTINGS }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_revert),
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Currency", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Pick your currency",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        CurrencyType.entries.forEach { currency ->
                            CurrencySelectionCard(
                                currency = currency,
                                selected = AppPreferences.selectedCurrency == currency,
                                onClick = { AppPreferences.setCurrency(currency) }
                            )
                        }
                    }
                }
            }
        }

        ProfilePage.DATA_MANAGEMENT -> {
            DataManagementPage(modifier = scaffoldModifier, onBack = { page = ProfilePage.SETTINGS })
        }
    }
    } // end Scaffold
}

@Composable
private fun DataManagementPage(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<ImportPreview?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    OutputStreamWriter(stream).use { writer -> writer.write(DataRepository.exportToJson()) }
                }
                statusMessage = "Exported successfully!"
            } catch (e: Exception) {
                statusMessage = "Export failed: ${e.message}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val fileSizeBytes = context.contentResolver.openFileDescriptor(it, "r")?.use { fd -> fd.statSize } ?: 0L
                if (fileSizeBytes > 5 * 1024 * 1024) {
                    statusMessage = "Import failed: file is too large (max 5 MB)."
                    return@let
                }
                val content = context.contentResolver.openInputStream(it)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: ""
                if (content.isNotBlank()) {
                    pendingImportJson = content
                    importPreview = DataRepository.previewImport(content)
                    previewError = null
                } else {
                    statusMessage = "Import failed: file is empty."
                }
            } catch (e: Exception) {
                statusMessage = "Import failed: ${e.message}"
            }
        }
    }

    if (importPreview != null && pendingImportJson != null) {
        val preview = importPreview!!
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { importPreview = null; pendingImportJson = null },
            title = { Text("Import Preview") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (preview.exportedAt != null) {
                        Text(
                            "Backup from: ${preview.exportedAt.format(fmt)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (preview.newAccountNames.isNotEmpty()) {
                        Text(
                            "New accounts (${preview.newAccountNames.size}): ${preview.newAccountNames.joinToString(", ")}",
                            fontSize = 13.sp
                        )
                    }
                    if (preview.reusedAccountNames.isNotEmpty()) {
                        Text(
                            "Existing accounts reused (${preview.reusedAccountNames.size}): ${preview.reusedAccountNames.joinToString(", ")}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Transactions to add: ${preview.newTransactionCount}", fontSize = 13.sp)
                    if (preview.skippedTransactionCount > 0) {
                        Text(
                            "Duplicates skipped: ${preview.skippedTransactionCount}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (preview.newArchiveCount > 0) {
                        Text("Archive months to add: ${preview.newArchiveCount}", fontSize = 13.sp)
                    }
                    if (!previewError.isNullOrBlank()) {
                        Text(previewError!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    try {
                        DataRepository.importFromJson(pendingImportJson!!)
                        statusMessage = "Imported successfully!"
                    } catch (e: Exception) {
                        statusMessage = "Import failed: ${e.message}"
                    }
                    importPreview = null
                    pendingImportJson = null
                    previewError = null
                }) { Text("Import") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    importPreview = null
                    pendingImportJson = null
                    previewError = null
                }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_revert),
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Data Management", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Backup", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Export all your accounts, transactions, and archives to a JSON file you can save anywhere.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Button(
                    onClick = { statusMessage = null; exportLauncher.launch("shallnotspend_backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Export Backup", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Restore", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "A preview will show what will be added before anything is changed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                OutlinedButton(
                    onClick = { statusMessage = null; importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Import Backup", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage!!,
                    color = if (statusMessage!!.contains("failed", ignoreCase = true)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSelectionCard(
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ThemeMode.SYSTEM to "System",
                ThemeMode.LIGHT to "Light",
                ThemeMode.DARK to "Dark"
            ).forEach { (mode, label) ->
                val selected = selectedMode == mode
                Button(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (name, subtitle, colors) = when (palette) {
        ThemePalette.NEON_MINT -> Triple(
            "Mint Pulse",
            "Fresh neon greens with energetic contrast",
            listOf(Color(0xFF00FF7F), Color(0xFFFF5252), Color(0xFF9BFFB8))
        )
        ThemePalette.SUNSET_CORAL -> Triple(
            "Sunset Coral",
            "Warm coral glow with vivid pink accents",
            listOf(Color(0xFFFF8A5B), Color(0xFFFF4F7B), Color(0xFFFFC371))
        )
        ThemePalette.OCEAN_BLUE -> Triple(
            "Ocean Drift",
            "Cool cyan-blue tones with soft highlights",
            listOf(Color(0xFF43C6FF), Color(0xFF5B8CFF), Color(0xFF8AE3FF))
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            if (selected) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "Active",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencySelectionCard(
    currency: CurrencyType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (symbol, name, sample) = when (currency) {
        CurrencyType.USD -> Triple("$", "US Dollar", "$1,234.56")
        CurrencyType.EUR -> Triple("€", "Euro", "€1,234.56")
        CurrencyType.GBP -> Triple("£", "British Pound", "£1,234.56")
        CurrencyType.INR -> Triple("₹", "Indian Rupee", "₹1,234.56")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(symbol, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(sample, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            if (selected) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "Active",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    title: String,
    subtitle: String,
    iconLabel: String,
    iconBgColor: Color,
    trailingContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Colored circle icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconLabel,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        trailingContent()
    }
}

@Composable
private fun MenuChevron() {
    Text(
        text = ">",
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        modifier = Modifier.padding(start = 78.dp, end = 20.dp)
    )
}

@Composable
private fun SettingsOptionCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Icon(
                painter = painterResource(android.R.drawable.ic_media_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthlyHistoryFullPage(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val archives = DataRepository.monthArchives.sortedByDescending { it.archivedAt }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_revert),
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Monthly History", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${archives.size} archived month${if (archives.size != 1) "s" else ""}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (archives.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No history yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(archives, key = { it.id }) { archive ->
                    MonthArchiveCard(archive = archive)
                }
            }
        }
    }
}

@Composable
fun MonthArchiveCard(archive: MonthArchive) {
    var expanded by remember { mutableStateOf(false) }

    val income = archive.transactions.filter {
        it.type in listOf(TransactionType.INCOME, TransactionType.TRANSFER, TransactionType.LEND_RETURN, TransactionType.BORROW)
    }.sumOf { it.amount }
    val expense = archive.transactions.filter {
        it.type in listOf(TransactionType.EXPENSE, TransactionType.LEND, TransactionType.BORROW_RETURN)
    }.sumOf { it.amount }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = archive.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = archive.accountName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatSignedCurrency(income, "+"),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatSignedCurrency(expense, "-"),
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        painter = painterResource(
                            if (expanded) android.R.drawable.arrow_up_float
                            else android.R.drawable.arrow_down_float
                        ),
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Closing balance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = formatCurrency(archive.closingBalance),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    archive.transactions.sortedByDescending { it.date }.forEach { tx ->
                        TransactionItem(transaction = tx)
                    }
                }
            }
        }
    }
}
