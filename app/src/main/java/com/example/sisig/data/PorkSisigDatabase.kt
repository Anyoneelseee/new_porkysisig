package com.example.sisig.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Order::class, OrderItem::class, Product::class, Stock::class, AllOrder::class, ProductStock::class],
    version = 5, // Increment version from 4 to 5
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val productStockDao: ProductStockDao
    abstract val orderDao: OrderDao
    abstract val orderItemDao: OrderItemDao
    abstract val productDao: ProductDao
    abstract val stockDao: StockDao
    abstract val allOrderDao: AllOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS all_orders (
                        orderId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderDetail TEXT NOT NULL,
                        totalAmount REAL NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS all_orders_temp (
                        orderId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderDetail TEXT NOT NULL,
                        totalAmount REAL NOT NULL
                    )
                """)
                database.execSQL("""
                    INSERT INTO all_orders_temp (orderDetail, totalAmount)
                    SELECT orderDetail, totalAmount FROM all_orders
                """)
                database.execSQL("DROP TABLE all_orders")
                database.execSQL("ALTER TABLE all_orders_temp RENAME TO all_orders")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS product_stock (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productName TEXT NOT NULL,
                        quantity INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            ALTER TABLE all_orders ADD COLUMN date TEXT NOT NULL DEFAULT '2023-01-01 00:00:00'
        """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "PorkSisigDatabase"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .allowMainThreadQueries() // For testing only; consider removing in production
                    .fallbackToDestructiveMigration() // Use with caution
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}