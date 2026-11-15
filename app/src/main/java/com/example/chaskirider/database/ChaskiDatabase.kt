package com.example.chaskirider.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chaskirider.feature.bank.data.local.BankAccountDao
import com.example.chaskirider.feature.bank.data.local.BankAccountEntity
import com.example.chaskirider.feature.chat.data.local.ChatMessageDao
import com.example.chaskirider.feature.chat.data.local.ChatMessageEntity
import com.example.chaskirider.feature.documents.data.local.DocumentDao
import com.example.chaskirider.feature.documents.data.local.DocumentEntity
import com.example.chaskirider.feature.earnings.data.local.ConnectionSessionEntity
import com.example.chaskirider.feature.earnings.data.local.EarningsDao
import com.example.chaskirider.feature.earnings.data.local.SettlementEntity
import com.example.chaskirider.feature.notifications.data.local.NotificationDao
import com.example.chaskirider.feature.notifications.data.local.NotificationEntity
import com.example.chaskirider.feature.orders.data.local.OrderContactDao
import com.example.chaskirider.feature.orders.data.local.OrderContactEntity
import com.example.chaskirider.feature.orders.data.local.OrderEntity
import com.example.chaskirider.feature.orders.data.local.OrderProblemEntity
import com.example.chaskirider.feature.orders.data.local.OrdersDao
import com.example.chaskirider.feature.rider.data.local.RiderDao
import com.example.chaskirider.feature.rider.data.local.RiderEntity
import com.example.chaskirider.feature.support.data.local.SupportDao
import com.example.chaskirider.feature.support.data.local.SupportMessageEntity
import com.example.chaskirider.feature.support.data.local.SupportTicketEntity
import java.io.File

@Database(
    entities = [
        RiderEntity::class,
        BankAccountEntity::class,
        DocumentEntity::class,
        OrderEntity::class, OrderProblemEntity::class,
        OrderContactEntity::class,
        ChatMessageEntity::class,
        SupportTicketEntity::class, SupportMessageEntity::class,
        SettlementEntity::class, ConnectionSessionEntity::class,
        NotificationEntity::class
    ],
    version = 11,
    exportSchema = true
)
abstract class ChaskiDatabase : RoomDatabase() {
    abstract fun riders(): RiderDao
    abstract fun banks(): BankAccountDao
    abstract fun documents(): DocumentDao
    abstract fun orders(): OrdersDao
    abstract fun orderContacts(): OrderContactDao
    abstract fun chatMessages(): ChatMessageDao
    abstract fun support(): SupportDao
    abstract fun earnings(): EarningsDao
    abstract fun notifications(): NotificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE registration_drafts RENAME TO rider_onboarding_drafts")
                db.execSQL("ALTER TABLE document_captures RENAME TO pending_document_uploads")
                db.execSQL("ALTER TABLE demo_orders RENAME TO order_session_states")
                db.execSQL("ALTER TABLE order_records RENAME TO orders")
                db.execSQL("ALTER TABLE order_actions RENAME TO pending_order_actions")
                db.execSQL("ALTER TABLE customer_contact RENAME TO order_contact_states")
                db.execSQL("ALTER TABLE chat_drafts RENAME TO pending_chat_messages")
                db.execSQL("ALTER TABLE support_drafts RENAME TO support_ticket_drafts")
                db.execSQL("DROP TABLE IF EXISTS bank_cache")
                db.execSQL("DROP TABLE IF EXISTS vehicle_cache")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS riders (uid TEXT NOT NULL, name TEXT NOT NULL, lastName TEXT NOT NULL, dni TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL, profilePhotoPath TEXT NOT NULL, vehicleType TEXT, status TEXT NOT NULL, rejectionReason TEXT NOT NULL, submissionVersion INTEGER NOT NULL, termsAccepted INTEGER NOT NULL, isAvailable INTEGER NOT NULL, isEnabled INTEGER NOT NULL, PRIMARY KEY(uid))")
                db.execSQL("DROP TABLE IF EXISTS rider_onboarding_drafts")
                db.execSQL("DROP TABLE IF EXISTS pending_document_uploads")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS bank_accounts (riderId TEXT NOT NULL, bankName TEXT NOT NULL, holderName TEXT NOT NULL, accountNumber TEXT NOT NULL, cci TEXT NOT NULL, statementDocumentUrl TEXT NOT NULL, PRIMARY KEY(riderId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS documents (riderId TEXT NOT NULL, type TEXT NOT NULL, storagePath TEXT NOT NULL, status TEXT NOT NULL, reviewReason TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(riderId, type))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS order_contacts (riderId TEXT NOT NULL, orderId TEXT NOT NULL, arrivedAt INTEGER NOT NULL, firstCallAt INTEGER, firstCallElapsed INTEGER, boot INTEGER, secondCallAt INTEGER, reminderSeen INTEGER NOT NULL, phone TEXT, serverRegistered INTEGER NOT NULL, PRIMARY KEY(riderId, orderId))")
                db.execSQL("DROP TABLE IF EXISTS order_contact_states")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_order_actions RENAME TO order_actions")
                db.execSQL("ALTER TABLE pending_chat_messages RENAME TO chat_messages")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS orders")
                db.execSQL("CREATE TABLE IF NOT EXISTS orders (riderId TEXT NOT NULL, orderId TEXT NOT NULL, status TEXT NOT NULL, merchantName TEXT NOT NULL, pickupLatitude REAL NOT NULL, pickupLongitude REAL NOT NULL, destinationLatitude REAL NOT NULL, destinationLongitude REAL NOT NULL, acceptedAt INTEGER, arrivedAt INTEGER, pickedUpAt INTEGER, deliveredAt INTEGER, cancelledAt INTEGER, waitStartedAt INTEGER, evidencePhotoUrl TEXT, deliveryNote TEXT, paidByCustomer INTEGER NOT NULL, earningsApproachMeters INTEGER, earningsDeliveryMeters INTEGER, earningsChaskiCents INTEGER, earningsTipCents INTEGER, earningsTotalCents INTEGER, earningsPricingVersion TEXT, earningsDistanceSource TEXT, completedAt INTEGER, completionReason TEXT, revision INTEGER NOT NULL, pending INTEGER NOT NULL, PRIMARY KEY(riderId, orderId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS order_problems (id TEXT NOT NULL, riderId TEXT NOT NULL, orderId TEXT NOT NULL, category TEXT NOT NULL, detail TEXT NOT NULL, occurredAt INTEGER NOT NULL, pickedUp INTEGER NOT NULL, cancelled INTEGER NOT NULL, orderStatus TEXT, PRIMARY KEY(id))")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS order_session_states")
                db.execSQL("DROP TABLE IF EXISTS order_actions")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS chat_messages")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (id TEXT NOT NULL, riderId TEXT NOT NULL, orderId TEXT NOT NULL, channel TEXT NOT NULL, senderRole TEXT NOT NULL, text TEXT NOT NULL, createdAt INTEGER NOT NULL, status TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS support_tickets (id TEXT NOT NULL, riderId TEXT NOT NULL, topic TEXT NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL, needsHuman INTEGER NOT NULL, orderId TEXT, orderMerchantName TEXT, orderPickupAddress TEXT, orderDeliveryAddress TEXT, orderPickedUp INTEGER, orderProblemId TEXT, orderStatus TEXT, orderProblemCategory TEXT, locationLatitude REAL, locationLongitude REAL, locationCapturedAt INTEGER, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS support_messages (id TEXT NOT NULL, riderId TEXT NOT NULL, ticketId TEXT NOT NULL, sender TEXT NOT NULL, body TEXT NOT NULL, createdAt INTEGER NOT NULL, pending INTEGER NOT NULL, mode TEXT, PRIMARY KEY(id))")
                db.execSQL("DROP TABLE IF EXISTS support_ticket_drafts")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS settlements (id TEXT NOT NULL, riderId TEXT NOT NULL, periodLabel TEXT NOT NULL, amountCents INTEGER NOT NULL, status TEXT NOT NULL, paidAt INTEGER, orderIdsJson TEXT NOT NULL, periodStart INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS connection_sessions (riderId TEXT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER, PRIMARY KEY(riderId, startedAt))")
                db.execSQL("CREATE TABLE IF NOT EXISTS notifications (id TEXT NOT NULL, riderId TEXT, title TEXT NOT NULL, body TEXT NOT NULL, createdAt INTEGER NOT NULL, read INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
    }
}

/** Base única de la app; en el primer arranque absorbe los datos de las bases por feature. */
fun openChaskiDatabase(
    context: Context,
    name: String = "chaski.db",
    importLegacy: Boolean = name == "chaski.db",
): ChaskiDatabase =
    Room.databaseBuilder(context.applicationContext, ChaskiDatabase::class.java, name)
        .addMigrations(ChaskiDatabase.MIGRATION_1_2, ChaskiDatabase.MIGRATION_2_3, ChaskiDatabase.MIGRATION_3_4, ChaskiDatabase.MIGRATION_4_5, ChaskiDatabase.MIGRATION_5_6, ChaskiDatabase.MIGRATION_6_7, ChaskiDatabase.MIGRATION_7_8, ChaskiDatabase.MIGRATION_8_9, ChaskiDatabase.MIGRATION_9_10, ChaskiDatabase.MIGRATION_10_11)
        .enableMultiInstanceInvalidation()
        .run { if (importLegacy) addCallback(LegacyDatabaseCleanup(context.applicationContext)) else this }
        .build()

private class LegacyDatabaseCleanup(private val context: Context) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        synchronized(LOCK) {
            deleteIfExists(context, "support_drafts.db")
            deleteIfExists(context, "registration.db")
            deleteIfExists(context, "customer_contact.db")
            deleteIfExists(context, "demo_orders.db")
            deleteIfExists(context, "chat_drafts.db")
            deleteIfExists(context, "bank_cache.db")
            deleteIfExists(context, "vehicle_cache.db")
        }
    }

    private fun deleteIfExists(context: Context, name: String) {
        val file = context.getDatabasePath(name)
        if (file.exists()) delete(file)
    }

    private fun delete(file: File) {
        listOf(file, File(file.path + "-wal"), File(file.path + "-shm"), File(file.path + "-journal"))
            .forEach { runCatching { it.delete() } }
    }

    private companion object {
        val LOCK = Any()
    }
}
