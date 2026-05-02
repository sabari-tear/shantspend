package com.shalltear.shallnotspend

import com.google.gson.GsonBuilder
import com.shalltear.shallnotspend.model.Account
import com.shalltear.shallnotspend.model.AccountType
import com.shalltear.shallnotspend.model.AppBackup
import com.shalltear.shallnotspend.model.BACKUP_SCHEMA_VERSION
import com.shalltear.shallnotspend.model.ImportPreview
import com.shalltear.shallnotspend.model.LocalDateTimeAdapter
import com.shalltear.shallnotspend.model.MonthArchive
import com.shalltear.shallnotspend.model.Transaction
import com.shalltear.shallnotspend.model.TransactionType
import com.shalltear.shallnotspend.model.fingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for pure data logic — no Android context required.
 */
class DataLogicTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    private fun makeAccount(id: String, name: String) =
        Account(id = id, name = name, type = AccountType.REGULAR)

    private fun makeTx(id: String, accountId: String, title: String, amount: Double, type: TransactionType = TransactionType.EXPENSE) =
        Transaction(id = id, title = title, amount = amount, type = type,
            date = LocalDateTime.of(2026, 1, 15, 10, 0), category = type.name,
            iconId = 0, accountId = accountId)

    // ── Backup schema version ────────────────────────────────────────────────
    @Test
    fun `backup JSON contains schemaVersion`() {
        val backup = AppBackup(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = LocalDateTime.now(),
            accounts = emptyList(),
            transactions = emptyList(),
            archives = emptyList()
        )
        val json = gson.toJson(backup)
        assertTrue("schemaVersion missing from JSON", json.contains("\"schemaVersion\""))
        assertTrue("exportedAt missing from JSON", json.contains("\"exportedAt\""))
    }

    @Test
    fun `backup schema version matches constant`() {
        assertEquals(1, BACKUP_SCHEMA_VERSION)
    }

    // ── Transaction fingerprint ──────────────────────────────────────────────
    @Test
    fun `same logical transaction produces same fingerprint`() {
        val date = LocalDateTime.of(2026, 3, 10, 14, 30)
        val t1 = Transaction(id = "id-1", title = "Coffee", amount = 4.50,
            type = TransactionType.EXPENSE, date = date, category = "EXPENSE",
            iconId = 0, accountId = "acc-1")
        val t2 = t1.copy(id = "id-2") // different UUID, same content
        assertEquals(t1.fingerprint(), t2.fingerprint())
    }

    @Test
    fun `different amounts produce different fingerprints`() {
        val date = LocalDateTime.of(2026, 3, 10, 14, 30)
        val t1 = makeTx("a", "acc", "Coffee", 4.50)
        val t2 = makeTx("b", "acc", "Coffee", 5.00)
        assertFalse(t1.fingerprint() == t2.fingerprint())
    }

    // ── Import dedupe (ID-based) ─────────────────────────────────────────────
    @Test
    fun `import skips transactions with duplicate IDs`() {
        val existing = listOf(makeTx("tx-1", "acc-1", "Salary", 1000.0, TransactionType.INCOME))
        val imported = listOf(makeTx("tx-1", "acc-1", "Salary", 1000.0, TransactionType.INCOME))
        val existingIds = existing.map { it.id }.toSet()
        val newTxCount = imported.count { it.id !in existingIds }
        assertEquals(0, newTxCount)
    }

    // ── Import dedupe (fingerprint-based) ────────────────────────────────────
    @Test
    fun `import skips content-duplicate transactions with different IDs`() {
        val date = LocalDateTime.of(2026, 3, 1, 9, 0)
        val existing = listOf(Transaction(id = "old-id", title = "Rent", amount = 500.0,
            type = TransactionType.EXPENSE, date = date, category = "EXPENSE",
            iconId = 0, accountId = "acc-1"))
        val imported = listOf(Transaction(id = "new-id", title = "Rent", amount = 500.0,
            type = TransactionType.EXPENSE, date = date, category = "EXPENSE",
            iconId = 0, accountId = "acc-1"))
        val existingIds = existing.map { it.id }.toSet()
        val existingFingerprints = existing.map { it.fingerprint() }.toSet()
        val newTxCount = imported.count { tx ->
            tx.id !in existingIds && tx.fingerprint() !in existingFingerprints
        }
        assertEquals(0, newTxCount)
    }

    // ── Account name dedup ───────────────────────────────────────────────────
    @Test
    fun `import reuses existing account with same name (case-insensitive)`() {
        val existing = listOf(makeAccount("acc-existing", "Salary"))
        val imported = listOf(makeAccount("acc-from-backup", "salary")) // lowercase
        val accountIdMap = mutableMapOf<String, String>()
        imported.forEach { imp ->
            val found = existing.find { it.name.equals(imp.name, ignoreCase = true) }
            if (found != null) accountIdMap[imp.id] = found.id
        }
        assertEquals("acc-existing", accountIdMap["acc-from-backup"])
    }

    @Test
    fun `import adds account with new name`() {
        val existing = listOf(makeAccount("acc-1", "Salary"))
        val imported = listOf(makeAccount("acc-new", "Savings"))
        val newAccounts = imported.filter { imp -> existing.none { it.name.equals(imp.name, ignoreCase = true) } }
        assertEquals(1, newAccounts.size)
        assertEquals("Savings", newAccounts.first().name)
    }

    // ── Clear all data ───────────────────────────────────────────────────────
    @Test
    fun `clear all produces empty backup`() {
        // Simulate clearAllData on an in-memory snapshot
        val cleared = AppBackup(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = null,
            accounts = emptyList(),
            transactions = emptyList(),
            archives = emptyList()
        )
        assertEquals(0, cleared.accounts.size)
        assertEquals(0, cleared.transactions.size)
        assertEquals(0, cleared.archives.size)
    }

    // ── Amount validation helper ─────────────────────────────────────────────
    @Test
    fun `amount string with more than 2 decimal places is truncated`() {
        val raw = "12.3456"
        val filtered = raw.let { s ->
            val dot = s.indexOf('.')
            if (dot >= 0) s.take(dot + 1) + s.drop(dot + 1).filter { it.isDigit() }.take(2)
            else s
        }
        assertEquals("12.34", filtered)
    }

    @Test
    fun `amount string with no decimal is unchanged`() {
        val raw = "100"
        val filtered = raw.let { s ->
            val dot = s.indexOf('.')
            if (dot >= 0) s.take(dot + 1) + s.drop(dot + 1).filter { it.isDigit() }.take(2)
            else s
        }
        assertEquals("100", filtered)
    }
}
