package viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val driver = JdbcSqliteDriver(
        url = "jdbc:sqlite:sales_products.db?journal_mode=WAL&synchronous=NORMAL&cache_size=10000",
        schema = Database.Schema
    )

    private val database = Database(driver)
    private val queries = database.salesProductsQueries
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Use StateFlow for better performance with collections
    private val _productsList = MutableStateFlow<List<SalesProducts>>(emptyList())
    val productsList = _productsList.asStateFlow()

    private val _allproductsList = MutableStateFlow<List<SalesProducts>>(emptyList())
    val allproductsList = _allproductsList.asStateFlow()

    private val _currentDateTime = MutableStateFlow(getCurrentFormattedDateTime())
    val currentDateTime = _currentDateTime.asStateFlow()

    private val mutex = Mutex()
    private val secureRandom = SecureRandom()

    init {
        scope.launch {
            loadInitialData()
            startTimeUpdates()
        }
    }

    private suspend fun loadInitialData() {
        withContext(Dispatchers.IO) {
            launch { loadProducts() }
            launch { loadAllProducts() }
        }
    }

    private suspend fun startTimeUpdates() {
        withContext(Dispatchers.IO) {
            while (true) {
                _currentDateTime.value = getCurrentFormattedDateTime()
                delay(1000)
            }
        }
    }

    fun loadProducts() {
        scope.launch(Dispatchers.IO) {
            try {
                val currentDate = LocalDate.now().toString()
                val products = withContext(Dispatchers.IO) {
                    queries.selectAll()
                        .executeAsList()
                        .asSequence()
                        .filter { it.time.startsWith(currentDate) }
                        .map { it.toSalesProduct() }
                        .toList()
                }
                _productsList.value = products
            } catch (e: Exception) {
                println("Error loading products: ${e.message}")
                _productsList.value = emptyList()
            }
        }
    }

    fun loadCurrentDateTime() {
        scope.launch(Dispatchers.IO) {
            flow {
                while (true) {
                    emit(getCurrentFormattedDateTime())
                    delay(1000) // Update every second
                }
            }.collect { formattedTime ->
                _currentDateTime.value = formattedTime
            }
        }
    }

    fun loadAllProducts() {
        scope.launch(Dispatchers.IO) {
            try {
                val products = queries.selectAll().executeAsList().map { it.toSalesProduct() }
                _allproductsList.value = products
            } catch (e: Exception) {
                println("Error loading products: ${e.message}")
                _allproductsList.value = emptyList()
            }
        }
    }

    fun refreshProducts() {
        loadProducts()
    }

    private fun getTotalPriceOfProducts(): Double {
        try {
            val currentDate = LocalDate.now().toString()
            val products = queries.selectAll().executeAsList().filter { product ->
                product.time.startsWith(currentDate)
            }
            return products.sumOf { product ->
                product.price.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            println("Error calculating total price: ${e.message}")
            return 0.0
        }
    }

    private fun getTotalPriceOfAllProducts(): Double {
        return try {
            val products = queries.selectAll().executeAsList()
            products.sumOf { product ->
                product.price.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            println("Error calculating total price: ${e.message}")
            0.0
        }
    }

    fun addProduct(
        productName: String, qty: String, price: Double
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                mutex.withLock {
                    val newProduct = SalesProducts(
                        pid = generateUniqueId(),
                        productName = productName,
                        qty = qty,
                        time = getCurrentTimestamp(),
                        price = price.toString()
                    )

                    if (newProduct.isValid()) {
                        queries.insertProduct(
                            pid = newProduct.pid,
                            productName = newProduct.productName,
                            qty = newProduct.qty,
                            time = newProduct.time,
                            price = newProduct.price
                        )

                        _productsList.update { currentList ->
                            currentList + newProduct
                        }
                    } else {
                        throw IllegalArgumentException("Invalid product data")
                    }
                }
            } catch (e: Exception) {
                println("Error adding product: ${e.message}")
            }
        }
    }

    fun removeProductById(pid: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                mutex.withLock {
                    queries.deleteById(pid)
                    _productsList.update { currentList ->
                        currentList.filter { it.pid != pid }
                    }
                }
            } catch (e: Exception) {
                println("Error removing product: ${e.message}")
            }
        }
    }

    fun clearProducts() {
        scope.launch(Dispatchers.IO) {
            try {
                mutex.withLock {
                    queries.deleteAll()
                    _productsList.value = emptyList()
                }
            } catch (e: Exception) {
                println("Error clearing products: ${e.message}")
            }
        }
    }

    fun getFormattedTotalPriceOfAllProducts(): String {
        val totalPrice = getTotalPriceOfAllProducts()
        return "${"%,d".format(totalPrice.toLong())} UGX"
    }

    fun getFormattedTotalPriceOfProducts(): String {
        val totalPrice = getTotalPriceOfProducts()
        return "${"%,d".format(totalPrice.toLong())} UGX"
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
        return SalesProducts(
            pid = pid, productName = productName, qty = qty, time = time, price = price
        )
    }

    private fun getCurrentFormattedDateTime(): String {
        val currentDateTime = LocalDateTime.now()

        val dayOfWeek = currentDateTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val formattedDate = currentDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val formattedTime = currentDateTime.format(DateTimeFormatter.ofPattern("h:mm:ss a"))

        return "$dayOfWeek - $formattedDate $formattedTime"
    }
}

data class SalesProducts(
    val pid: Int, val productName: String, val qty: String, val time: String, val price: String
) {
    // Comprehensive validation with UGX-specific constraints
    fun isValid(): Boolean =
        productName.isNotBlank() && productName.length <= 100 && price.toDoubleOrNull()
            ?.let { it >= 0.0 } ?: false

    // Formatted price in UGX with locale-specific formatting
    fun formattedPrice(): String {
        return try {
            val priceValue = price.toDoubleOrNull()?.toLong() ?: 0L
            "${"%,d".format(priceValue)} UGX"
        } catch (e: Exception) {
            "0 UGX"
        }
    }

    // Formatted time in a readable format
    fun formattedTime(): String = time.takeIf { it.isNotBlank() }?.let {
        LocalDateTime.parse(it).format(DateTimeFormatter.ofPattern("h:mm a"))
    } ?: "Unknown Time"
}