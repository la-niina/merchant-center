package viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pherus.merchant.center.Database
import java.security.SecureRandom
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import domain.model.SalesProduct
import domain.model.toSalesProduct
import domain.model.toDatabaseModel
import java.math.BigDecimal
import kotlin.text.toBigDecimalOrNull
import core.database.DatabaseMigration

class MainViewModel {
    // Optimize SQLite connection with larger cache and better write-ahead logging
    private val driver = JdbcSqliteDriver(
        url = "jdbc:sqlite:sales_products.db?journal_mode=WAL&synchronous=NORMAL&cache_size=50000&mmap_size=268435456",
        schema = Database.Schema
    )

    private val database = Database(driver)
    private val queries = database.salesProductsQueries
    // Use limited dispatcher for better resource management
    @OptIn(ExperimentalCoroutinesApi::class)
    private val limitedDispatcher = Dispatchers.IO.limitedParallelism(4)
    val scope = CoroutineScope(limitedDispatcher + SupervisorJob())

    // Use StateFlow with proper capacity hint
    private val _productsList = MutableStateFlow<List<SalesProduct>>(emptyList())
    val productsList = _productsList.asStateFlow()

    private val _allProductsList = MutableStateFlow<List<SalesProduct>>(emptyList())
    val allproductsList = _allProductsList.asStateFlow()

    // Cache formatters for better performance
    private val dayNameFormatter by lazy { DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH) }
    private val dateFormatter by lazy { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    private val timeFormatter by lazy { DateTimeFormatter.ofPattern("h:mm:ss a") }

    // Use separate flow for time to reduce updates to UI
    private val _currentDateTime = MutableStateFlow(getCurrentFormattedDateTime())
    val currentDateTime = _currentDateTime.asStateFlow()

    // Cache current date to avoid repeated calculations
    private val currentDateStr by lazy { LocalDate.now().toString() }
    
    private val mutex = Mutex()
    private val secureRandom = SecureRandom()

    init {
        scope.launch {
            // Run database migration first
            runDatabaseMigration()
            loadInitialData()
            startTimeUpdates()
        }
    }

    private suspend fun loadInitialData() {
        withContext(limitedDispatcher) {
            // Run in parallel
            val productsJob = launch { loadProducts() }
            val allProductsJob = launch { loadAllProducts() }
            productsJob.join()
            allProductsJob.join()
        }
    }

    private fun startTimeUpdates() {
        scope.launch {
            // Use fixed rate timer for more accurate timing
            while (true) {
                _currentDateTime.value = getCurrentFormattedDateTime()
                delay(1000)
            }
        }
    }

    fun loadProducts() {
        scope.launch {
            try {
                val products = queries.selectAll()
                    .executeAsList()
                    .asSequence()
                    .map { it.toSalesProduct() }
                    .filter { it.time.toLocalDate().toString() == currentDateStr }
                    .toList()
                _productsList.value = products
            } catch (e: Exception) {
                println("Error loading products: "+e.message)
                _productsList.value = emptyList()
            }
        }
    }
    
    // Removed redundant loadCurrentDateTime function as it's handled by startTimeUpdates

    fun loadAllProducts() {
        scope.launch {
            try {
                val products = queries.selectAll().executeAsList().map { it.toSalesProduct() }
                _allProductsList.value = products
            } catch (e: Exception) {
                println("Error loading all products: "+e.message)
                _allProductsList.value = emptyList()
            }
        }
    }

    fun refreshProducts() = loadProducts()

    private fun getTotalPriceOfProducts(): BigDecimal {
        return try {
            val products = queries.selectTodaysProducts(currentDateStr + "%").executeAsList().map { it.toSalesProduct() }
            products.sumOf { it.totalPrice }
        } catch (e: Exception) {
            println("Error calculating total price: "+e.message)
            BigDecimal.ZERO
        }
    }

    private fun getTotalPriceOfAllProducts(): BigDecimal {
        return try {
            val sum = queries.selectSumOfAllPrices().executeAsOne()
            when (sum) {
                is Double -> BigDecimal.valueOf(sum)
                is Float -> BigDecimal.valueOf(sum.toDouble())
                is Long -> BigDecimal.valueOf(sum)
                is Int -> BigDecimal.valueOf(sum.toLong())
                else -> BigDecimal.ZERO
            }
        } catch (e: Exception) {
            println("Error calculating total price: "+e.message)
            BigDecimal.ZERO
        }
    }

    // loadProductsForDate
    internal fun loadProductsForDate(date: String) {
        scope.launch {
            try {
                val products = queries.selectTodaysProducts(date + "%").executeAsList().map { it.toSalesProduct() }
                _productsList.value = products
            } catch (e: Exception) {
                println("Error loading products for date: "+e.message)
                _productsList.value = emptyList()
            }
        }
    }

    fun addProduct(
        productName: String, qty: Int, unitPrice: BigDecimal
    ) {
        scope.launch {
            try {
                mutex.withLock {
                    val pid = generateUniqueId()
                    val now = getCurrentTimestamp()
                    val totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty.toLong()))
                    val salesProduct = SalesProduct(
                        pid = pid,
                        productName = productName,
                        qty = qty,
                        unitPrice = unitPrice,
                        totalPrice = totalPrice,
                        time = now,
                        createdAt = now,
                        updatedAt = now
                    )
                    queries.insertProduct(
                        pid = salesProduct.pid.toLong(),
                        productName = salesProduct.productName,
                        qty = salesProduct.qty.toLong(),
                        unitPrice = salesProduct.unitPrice.toString(),
                        totalPrice = salesProduct.totalPrice.toString(),
                        time = salesProduct.time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        created_at = salesProduct.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        updated_at = salesProduct.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    )
                    if (salesProduct.time.toLocalDate().toString() == currentDateStr) {
                        _productsList.update { it + salesProduct }
                    }
                }
            } catch (e: Exception) {
                println("Error adding product: "+e.message)
            }
        }
    }

    fun removeProductById(pid: Int) {
        scope.launch {
            try {
                mutex.withLock {
                    queries.deleteById(pid.toLong())
                    _productsList.update { it.filterNot { product -> product.pid == pid } }
                    _allProductsList.update { it.filterNot { product -> product.pid == pid } }
                }
            } catch (e: Exception) {
                println("Error removing product: "+e.message)
            }
        }
    }

    fun clearProducts() {
        scope.launch {
            try {
                mutex.withLock {
                    queries.deleteAll()
                    _productsList.value = emptyList()
                    _allProductsList.value = emptyList()
                }
            } catch (e: Exception) {
                println("Error clearing products: "+e.message)
            }
        }
    }

    fun getFormattedTotalPriceOfAllProducts(): String {
        val totalPrice = getTotalPriceOfAllProducts()
        return "%s UGX".format(totalPrice.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString())
    }

    fun getFormattedTotalPriceOfProducts(): String {
        val totalPrice = getTotalPriceOfProducts()
        return "%s UGX".format(totalPrice.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString())
    }

    private fun generateUniqueId(): Int {
        return secureRandom.nextInt(Int.MAX_VALUE)
    }

    private fun getCurrentTimestamp(): LocalDateTime {
        return LocalDateTime.now()
    }

    private fun getCurrentFormattedDateTime(): String {
        val currentDateTime = LocalDateTime.now()
        val dayOfWeek = currentDateTime.format(dayNameFormatter)
        val formattedDate = currentDateTime.format(dateFormatter)
        val formattedTime = currentDateTime.format(timeFormatter)
        return "$dayOfWeek - $formattedDate $formattedTime"
    }

    private suspend fun runDatabaseMigration() {
        try {
            println("Starting database migration...")
            DatabaseMigration.migrateDatabase(driver, database)
            println("Database migration completed successfully")
        } catch (e: Exception) {
            println("Database migration failed: ${e.message}")
            // If migration fails, we'll continue with the current schema
            // The app will handle missing columns gracefully
        }
    }
}