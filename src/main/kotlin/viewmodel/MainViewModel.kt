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
    private val _productsList = MutableStateFlow<List<SalesProducts>>(emptyList())
    val productsList = _productsList.asStateFlow()

    private val _allProductsList = MutableStateFlow<List<SalesProducts>>(emptyList())
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
                    .filter { it.time.startsWith(currentDateStr) }
                    .map { it.toSalesProduct() }
                    .toList()
                
                _productsList.value = products
            } catch (e: Exception) {
                println("Error loading products: ${e.message}")
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
                println("Error loading all products: ${e.message}")
                _allProductsList.value = emptyList()
            }
        }
    }

    fun refreshProducts() {
        loadProducts()
    }

    private fun getTotalPriceOfProducts(): Double {
        return try {
            val products = queries.selectTodaysProducts(currentDateStr + "%").executeAsList()
            products.sumOf { product -> product.price.toDoubleOrNull() ?: 0.0 }
        } catch (e: Exception) {
            println("Error calculating total price: ${e.message}")
            0.0
        }
    }

    private fun getTotalPriceOfAllProducts(): Double {
        return try {
            queries.selectSumOfAllPrices().executeAsOne().SUM ?: 0.0
        } catch (e: Exception) {
            println("Error calculating total price: ${e.message}")
            0.0
        }
    }

    // loadProductsForDate
    internal fun loadProductsForDate(date: String) {
        scope.launch {
            try {
                val products = queries.selectTodaysProducts(date + "%").executeAsList().map { it.toSalesProduct() }
                _productsList.value = products
            } catch (e: Exception) {
                println("Error loading products for date: ${e.message}")
                _productsList.value = emptyList()
            }
        }
    }

    fun addProduct(
        productName: String, qty: String, price: Double
    ) {
        scope.launch {
            try {
                mutex.withLock {
                    val pid = generateUniqueId()
                    val time = getCurrentTimestamp()
                    val priceStr = price.toString()

                    // Validate before creating object
                    if (productName.isBlank() || productName.length > 100 || price < 0) {
                        throw IllegalArgumentException("Invalid product data")
                    }
                    
                    // Insert directly to avoid creating temporary objects
                    queries.insertProduct(
                        pid = pid,
                        productName = productName,
                        qty = qty,
                        time = time,
                        price = priceStr
                    )
                    
                    // Only update the list if it's a product from today
                    if (time.startsWith(currentDateStr)) {
                        val newProduct = SalesProducts(pid, productName, qty, time, priceStr)
                        _productsList.update { it + newProduct }
                    }
                }
            } catch (e: Exception) {
                println("Error adding product: ${e.message}")
            }
        }
    }

    fun removeProductById(pid: Int) {
        scope.launch {
            try {
                mutex.withLock {
                    queries.deleteById(pid)
                    // Efficient filter with immutable list
                    _productsList.update { it.filterNot { product -> product.pid == pid } }
                    _allProductsList.update { it.filterNot { product -> product.pid == pid } }
                }
            } catch (e: Exception) {
                println("Error removing product: ${e.message}")
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
                println("Error clearing products: ${e.message}")
            }
        }
    }

    // Cached formatters for better performance
    private val priceFormatter = "%,d"
    
    fun getFormattedTotalPriceOfAllProducts(): String {
        val totalPrice = getTotalPriceOfAllProducts()
        return "${priceFormatter.format(totalPrice.toLong())} UGX"
    }

    fun getFormattedTotalPriceOfProducts(): String {
        val totalPrice = getTotalPriceOfProducts()
        return "${priceFormatter.format(totalPrice.toLong())} UGX"
    }

    // Generate a unique numeric ID
    private fun generateUniqueId(): Int {
        return secureRandom.nextInt(Int.MAX_VALUE)
    }

    // Generate current timestamp
    private fun getCurrentTimestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    // Extension function to convert database result to domain model
    private fun pherus.merchant.center.SalesProducts.toSalesProduct(): SalesProducts {
        return SalesProducts(pid, productName, qty, time, price)
    }
    
    private fun getCurrentFormattedDateTime(): String {
        val currentDateTime = LocalDateTime.now()
        val dayOfWeek = currentDateTime.format(dayNameFormatter)
        val formattedDate = currentDateTime.format(dateFormatter)
        val formattedTime = currentDateTime.format(timeFormatter)

        return "$dayOfWeek - $formattedDate $formattedTime"
    }
}

data class SalesProducts(
    val pid: Int, val productName: String, val qty: String, val time: String, val price: String
) {
    // More efficient validation
    fun isValid(): Boolean = productName.isNotBlank() && 
                            productName.length <= 100 && 
                            price.toDoubleOrNull()?.let { it >= 0.0 } ?: false

    // Cached price formatting for better performance
    private val priceFormatter = "%,d"
    
    // Formatted price in UGX with locale-specific formatting
    fun formattedPrice(): String {
        return try {
            val priceValue = price.toDoubleOrNull()?.toLong() ?: 0L
            "${priceFormatter.format(priceValue)} UGX"
        } catch (e: Exception) {
            "0 UGX"
        }
    }

    // Cached formatter for better performance
    private companion object {
        val timeFormatter by lazy { DateTimeFormatter.ofPattern("h:mm a") }
    }
    
    // Formatted time in a readable format
    fun formattedTime(): String = time.takeIf { it.isNotBlank() }?.let {
        try {
            LocalDateTime.parse(it).format(timeFormatter)
        } catch (e: Exception) {
            "Unknown Time"
        }
    } ?: "Unknown Time"
}