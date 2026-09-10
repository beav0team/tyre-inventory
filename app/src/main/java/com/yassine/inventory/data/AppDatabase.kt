package com.yassine.inventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Item::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE items ADD COLUMN brand TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE items ADD COLUMN diameter TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE items ADD COLUMN shape TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE items ADD COLUMN weight REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE items ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS items")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL DEFAULT '',
                        sku TEXT NOT NULL DEFAULT '',
                        rimDiameter INTEGER NOT NULL DEFAULT 0,
                        width INTEGER NOT NULL DEFAULT 0,
                        profile INTEGER NOT NULL DEFAULT 0,
                        brand TEXT NOT NULL DEFAULT '',
                        model TEXT NOT NULL DEFAULT '',
                        season TEXT NOT NULL DEFAULT '',
                        loadIndex TEXT NOT NULL DEFAULT '',
                        speedIndex TEXT NOT NULL DEFAULT '',
                        quantity INTEGER NOT NULL DEFAULT 0,
                        minQuantity INTEGER NOT NULL DEFAULT 0,
                        price REAL NOT NULL DEFAULT 0.0,
                        notes TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT '',
                        subCategory TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        suspend fun recreate(instance: AppDatabase) {
            val db = instance
            val driver = db.openHelper.writableDatabase
            driver.execSQL("DROP TABLE IF EXISTS items")
            driver.execSQL(
                """CREATE TABLE IF NOT EXISTS items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL DEFAULT '',
                    sku TEXT NOT NULL DEFAULT '',
                    rimDiameter INTEGER NOT NULL DEFAULT 0,
                    width INTEGER NOT NULL DEFAULT 0,
                    profile INTEGER NOT NULL DEFAULT 0,
                    brand TEXT NOT NULL DEFAULT '',
                    model TEXT NOT NULL DEFAULT '',
                    season TEXT NOT NULL DEFAULT '',
                    loadIndex TEXT NOT NULL DEFAULT '',
                    speedIndex TEXT NOT NULL DEFAULT '',
                    quantity INTEGER NOT NULL DEFAULT 0,
                    minQuantity INTEGER NOT NULL DEFAULT 0,
                    price REAL NOT NULL DEFAULT 0.0,
                    notes TEXT NOT NULL DEFAULT '',
                    category TEXT NOT NULL DEFAULT '',
                    subCategory TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0
                )"""
            )
        }

        fun closeAndReset() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
