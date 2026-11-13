package com.example.chaskirider.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chaskirider.feature.bank.data.local.BankCacheDao
import com.example.chaskirider.feature.bank.data.local.BankInfoEntity
import com.example.chaskirider.feature.chat.data.local.ChatDraftDao
import com.example.chaskirider.feature.chat.data.local.ChatDraftEntity
import com.example.chaskirider.feature.orders.data.local.CustomerContactDao
import com.example.chaskirider.feature.orders.data.local.CustomerContactEntity
import com.example.chaskirider.feature.orders.data.local.DemoOrderDao
import com.example.chaskirider.feature.orders.data.local.DemoOrderEntity
import com.example.chaskirider.feature.orders.data.local.OrderActionEntity
import com.example.chaskirider.feature.orders.data.local.OrderRecordEntity
import com.example.chaskirider.feature.support.data.local.SupportDraftDao
import com.example.chaskirider.feature.support.data.local.SupportDraftEntity
import com.example.chaskirider.feature.user.data.local.DocumentCaptureEntity
import com.example.chaskirider.feature.user.data.local.RegistrationDraftDao
import com.example.chaskirider.feature.user.data.local.RegistrationDraftEntity
import com.example.chaskirider.feature.vehicle.data.local.VehicleCacheDao
import com.example.chaskirider.feature.vehicle.data.local.VehicleCacheEntity
import java.io.File

@Database(
    entities = [
        RegistrationDraftEntity::class, DocumentCaptureEntity::class,
        DemoOrderEntity::class, OrderRecordEntity::class, OrderActionEntity::class,
        CustomerContactEntity::class,
        ChatDraftEntity::class,
        SupportDraftEntity::class,
        BankInfoEntity::class,
        VehicleCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ChaskiDatabase : RoomDatabase() {
    abstract fun registrationDrafts(): RegistrationDraftDao
    abstract fun orders(): DemoOrderDao
    abstract fun contacts(): CustomerContactDao
    abstract fun chatDrafts(): ChatDraftDao
    abstract fun supportDrafts(): SupportDraftDao
    abstract fun bankCache(): BankCacheDao
    abstract fun vehicleCache(): VehicleCacheDao
}

/** Base única de la app; en el primer arranque absorbe los datos de las bases por feature. */
fun openChaskiDatabase(
    context: Context,
    name: String = "chaski.db",
    importLegacy: Boolean = name == "chaski.db",
): ChaskiDatabase =
    Room.databaseBuilder(context.applicationContext, ChaskiDatabase::class.java, name)
        .enableMultiInstanceInvalidation()
        .run { if (importLegacy) addCallback(LegacyDatabaseImport(context.applicationContext)) else this }
        .build()

private class LegacyDatabaseImport(private val context: Context) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        synchronized(LOCK) {
            import(context, db, "registration.db", listOf("registration_drafts", "document_captures"))
            import(context, db, "demo_orders.db", listOf("demo_orders", "order_records", "order_actions"))
            import(context, db, "customer_contact.db", listOf("customer_contact"))
            import(context, db, "chat_drafts.db", listOf("chat_drafts"))
            import(context, db, "support_drafts.db", listOf("support_drafts"))
            import(context, db, "bank_cache.db", listOf("bank_cache"))
            import(context, db, "vehicle_cache.db", listOf("vehicle_cache"))
        }
    }

    private fun import(context: Context, db: SupportSQLiteDatabase, legacyName: String, tables: List<String>) {
        val file = context.getDatabasePath(legacyName)
        if (!file.exists()) return
        try {
            db.execSQL("ATTACH DATABASE ? AS legacy", arrayOf(file.absolutePath))
        } catch (error: Exception) {
            Log.w("ChaskiDatabase", "No se pudo abrir la base antigua $legacyName", error)
            return
        }
        var imported = true
        try {
            for (table in tables) {
                if (!exists(db, table)) continue
                db.beginTransaction()
                try {
                    db.execSQL("INSERT OR REPLACE INTO main.$table SELECT * FROM legacy.$table")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        } catch (error: Exception) {
            imported = false
            Log.w("ChaskiDatabase", "No se pudieron importar los datos de $legacyName", error)
        } finally {
            runCatching { db.execSQL("DETACH DATABASE legacy") }
        }
        if (imported) delete(file)
    }

    private fun exists(db: SupportSQLiteDatabase, table: String): Boolean =
        db.query("SELECT name FROM legacy.sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table))
            .use { it.moveToFirst() }

    private fun delete(file: File) {
        listOf(file, File(file.path + "-wal"), File(file.path + "-shm"), File(file.path + "-journal"))
            .forEach { runCatching { it.delete() } }
    }

    private companion object {
        val LOCK = Any()
    }
}
