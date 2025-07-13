package core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pherus.merchant.center.Database
import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import kotlin.text.toBigDecimalOrNull

/**
 * Database migration utility for handling schema changes
 */
object DatabaseMigration {
    
    private const val CURRENT_VERSION = 2
    private const val VERSION_TABLE = "schema_version"
    
    /**
     * Migrates the database to the latest version
     */
    fun migrateDatabase(driver: JdbcSqliteDriver, database: Database) {
        val currentVersion = getCurrentVersion(driver)
        
        when (currentVersion) {
            0 -> migrateFromV0ToV1(driver, database)
            1 -> migrateFromV1ToV2(driver, database)
            CURRENT_VERSION -> {
                // Database is at version 2, but ensure all tables exist
                ensureAllTablesExist(driver, database)
            }
            else -> {
                throw IllegalStateException("Unknown database version: $currentVersion")
            }
        }
    }
    
    /**
     * Gets the current database version
     */
    private fun getCurrentVersion(driver: JdbcSqliteDriver): Int {
        return try {
            val connection = driver.getConnection()
            val statement = connection.prepareStatement("SELECT version FROM $VERSION_TABLE LIMIT 1")
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        } catch (e: Exception) {
            // Version table doesn't exist, assume version 0
            0
        }
    }
    
    /**
     * Sets the database version
     */
    private fun setVersion(driver: JdbcSqliteDriver, version: Int) {
        try {
            val connection = driver.getConnection()
            val statement = connection.prepareStatement("UPDATE $VERSION_TABLE SET version = ?")
            statement.setInt(1, version)
            statement.executeUpdate()
        } catch (e: Exception) {
            // Version table doesn't exist, create it
            val connection = driver.getConnection()
            connection.createStatement().execute("CREATE TABLE $VERSION_TABLE (version INTEGER NOT NULL)")
            val insertStatement = connection.prepareStatement("INSERT INTO $VERSION_TABLE (version) VALUES (?)")
            insertStatement.setInt(1, version)
            insertStatement.executeUpdate()
        }
    }
    
    /**
     * Ensures all required tables exist, even if database is at version 2
     */
    private fun ensureAllTablesExist(driver: JdbcSqliteDriver, database: Database) {
        println("Ensuring all tables exist...")
        val connection = driver.getConnection()
        
        // Check if SalesProducts table exists
        val salesTableExists = try {
            connection.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='SalesProducts'")
            true
        } catch (e: Exception) {
            false
        }
        
        // Check if Products table exists
        val productsTableExists = try {
            connection.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Products'")
            true
        } catch (e: Exception) {
            false
        }
        
        // Create missing tables
        if (!salesTableExists || !productsTableExists) {
            println("Creating missing tables...")
            Database.Schema.create(driver)
        }
        
        // Always ensure Products table exists specifically (in case Schema.create didn't create it)
        if (!productsTableExists) {
            println("Creating Products table...")
            ensureProductsTableExists(connection)
        }
        
        println("All tables verified")
    }
    
    /**
     * Migration from version 0 to 1 (initial schema)
     */
    private fun migrateFromV0ToV1(driver: JdbcSqliteDriver, database: Database) {
        println("Migrating database from version 0 to 1...")
        
        val connection = driver.getConnection()
        
        // Check if SalesProducts table already exists
        val salesTableExists = try {
            connection.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='SalesProducts'")
            true
        } catch (e: Exception) {
            false
        }
        
        // Check if Products table already exists
        val productsTableExists = try {
            connection.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Products'")
            true
        } catch (e: Exception) {
            false
        }
        
        // Create the initial schema only if tables don't exist
        if (!salesTableExists || !productsTableExists) {
            Database.Schema.create(driver)
        }
        
        setVersion(driver, 1)
        println("Migration to version 1 completed")
    }
    
    /**
     * Migration from version 1 to 2 (improved schema with proper data types)
     */
    private fun migrateFromV1ToV2(driver: JdbcSqliteDriver, database: Database) {
        println("Migrating database from version 1 to 2...")
        
        val connection = driver.getConnection()
        
        // Start transaction
        connection.autoCommit = false
        
        try {
            // Check if we need to migrate SalesProducts table
            val needsSalesMigration = checkIfSalesMigrationNeeded(connection)
            
            if (needsSalesMigration) {
                // Create new SalesProducts table with improved schema
                connection.createStatement().execute("""
                    CREATE TABLE SalesProducts_new (
                      pid INTEGER PRIMARY KEY AUTOINCREMENT,
                      productName TEXT NOT NULL,
                      qty INTEGER NOT NULL,
                      unitPrice TEXT NOT NULL,
                      totalPrice TEXT NOT NULL,
                      time TEXT NOT NULL,
                      created_at TEXT NOT NULL,
                      updated_at TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Copy data from old table to new table with data transformation
                migrateSalesData(connection)
                
                // Drop old table
                connection.createStatement().execute("DROP TABLE SalesProducts")
                
                // Rename new table
                connection.createStatement().execute("ALTER TABLE SalesProducts_new RENAME TO SalesProducts")
                
                // Create indices
                connection.createStatement().execute("CREATE INDEX idx_sales_products_time ON SalesProducts(time)")
                connection.createStatement().execute("CREATE INDEX idx_sales_products_name ON SalesProducts(productName)")
            }
            
            // Ensure Products table exists
            ensureProductsTableExists(connection)
            
            // Commit transaction
            connection.commit()
            
            setVersion(driver, 2)
            println("Migration to version 2 completed successfully")
            
        } catch (e: Exception) {
            // Rollback on error
            connection.rollback()
            println("Migration failed: ${e.message}")
            throw e
        } finally {
            connection.autoCommit = true
        }
    }
    
    /**
     * Check if SalesProducts table needs migration
     */
    private fun checkIfSalesMigrationNeeded(connection: Connection): Boolean {
        return try {
            // Check if the table has the old schema (with 'price' column)
            val resultSet = connection.createStatement().executeQuery("PRAGMA table_info(SalesProducts)")
            var hasPriceColumn = false
            var hasUnitPriceColumn = false
            
            while (resultSet.next()) {
                val columnName = resultSet.getString(2)
                when (columnName) {
                    "price" -> hasPriceColumn = true
                    "unitPrice" -> hasUnitPriceColumn = true
                }
            }
            
            // If it has 'price' column but no 'unitPrice', it needs migration
            hasPriceColumn && !hasUnitPriceColumn
        } catch (e: Exception) {
            // Table doesn't exist or other error, assume no migration needed
            false
        }
    }
    
    /**
     * Ensure Products table exists
     */
    private fun ensureProductsTableExists(connection: Connection) {
        val productsTableExists = try {
            connection.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Products'")
            true
        } catch (e: Exception) {
            false
        }
        
        if (!productsTableExists) {
            // Create Products table
            connection.createStatement().execute("""
                CREATE TABLE Products (
                  productId INTEGER PRIMARY KEY AUTOINCREMENT,
                  productNumber TEXT UNIQUE NOT NULL,
                  productName TEXT NOT NULL,
                  unitPrice TEXT NOT NULL,
                  description TEXT,
                  category TEXT,
                  stockQuantity INTEGER DEFAULT 0,
                  isActive INTEGER DEFAULT 1,
                  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                  updated_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())
            
            // Create indices for Products table
            connection.createStatement().execute("CREATE INDEX idx_products_number ON Products(productNumber)")
            connection.createStatement().execute("CREATE INDEX idx_products_name ON Products(productName)")
            connection.createStatement().execute("CREATE INDEX idx_products_category ON Products(category)")
            connection.createStatement().execute("CREATE INDEX idx_products_active ON Products(isActive)")
            
            println("Products table created successfully")
        }
    }
    
    /**
     * Migrates sales data from old schema to new schema
     */
    private fun migrateSalesData(connection: Connection) {
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        // Get all data from old table
        val statement = connection.prepareStatement("SELECT pid, productName, qty, time, price FROM SalesProducts")
        val resultSet = statement.executeQuery()
        
        val insertStatement = connection.prepareStatement("""
            INSERT INTO SalesProducts_new 
            (pid, productName, qty, unitPrice, totalPrice, time, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent())
        
        var migratedCount = 0
        
        while (resultSet.next()) {
            val pid = resultSet.getInt(1)
            val productName = resultSet.getString(2) ?: ""
            val qtyStr = resultSet.getString(3) ?: "1"
            val time = resultSet.getString(4) ?: currentTime
            val priceStr = resultSet.getString(5) ?: "0"
            
            val qty = qtyStr.toIntOrNull() ?: 1
            val unitPrice = priceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty.toLong()))
            
            insertStatement.setInt(1, pid)
            insertStatement.setString(2, productName)
            insertStatement.setInt(3, qty)
            insertStatement.setString(4, unitPrice.toString())
            insertStatement.setString(5, totalPrice.toString())
            insertStatement.setString(6, time)
            insertStatement.setString(7, time)
            insertStatement.setString(8, currentTime)
            
            insertStatement.executeUpdate()
            migratedCount++
        }
        
        println("Migrated $migratedCount sales records")
    }
    
    /**
     * Validates the migration was successful
     */
    fun validateMigration(driver: JdbcSqliteDriver): Boolean {
        return try {
            val connection = driver.getConnection()
            val statement = connection.createStatement()
            
            // Check SalesProducts table
            val salesResult = statement.executeQuery("SELECT COUNT(*) FROM SalesProducts")
            salesResult.next()
            val salesCount = salesResult.getInt(1)
            println("Validation: Found $salesCount records in SalesProducts table")
            
            // Check Products table
            val productsResult = statement.executeQuery("SELECT COUNT(*) FROM Products")
            productsResult.next()
            val productsCount = productsResult.getInt(1)
            println("Validation: Found $productsCount records in Products table")
            
            true
        } catch (e: Exception) {
            println("Validation failed: ${e.message}")
            false
        }
    }
    
    /**
     * Rolls back the migration if needed
     */
    fun rollbackMigration(driver: JdbcSqliteDriver) {
        println("Rolling back migration...")
        val connection = driver.getConnection()
        connection.autoCommit = false
        
        try {
            // Drop the new tables
            connection.createStatement().execute("DROP TABLE IF EXISTS SalesProducts")
            connection.createStatement().execute("DROP TABLE IF EXISTS Products")
            
            // Recreate the old table structure
            connection.createStatement().execute("""
                CREATE TABLE SalesProducts (
                  pid INTEGER PRIMARY KEY NOT NULL,
                  productName TEXT NOT NULL,
                  qty TEXT NOT NULL,
                  time TEXT NOT NULL,
                  price TEXT NOT NULL
                )
            """.trimIndent())
            
            connection.commit()
            setVersion(driver, 1)
            println("Rollback completed")
        } catch (e: Exception) {
            connection.rollback()
            println("Rollback failed: ${e.message}")
            throw e
        } finally {
            connection.autoCommit = true
        }
    }
} 