package viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
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
        url = "jdbc:sqlite:sales_products.db", // Persistent database file
        schema = Database.Schema
    )
    private val database = Database(driver)
    private val queries = database.salesProductsQueries

    private val _productsList = MutableStateFlow<List<SalesProducts>>(emptyList())
    val productsList: StateFlow<List<SalesProducts>> = _productsList.asStateFlow()

    private val _currentDateTime = MutableStateFlow(getCurrentFormattedDateTime())
    val currentDateTime: StateFlow<String> = _currentDateTime.asStateFlow()

    // Mutex for thread-safe operations
    private val mutex = Mutex()

    // Secure random generator for unique ID
    private val secureRandom = SecureRandom()

    init {
        loadProducts()
    }

    suspend fun loadCurrentDateTime() {
        withContext(Dispatchers.IO) {
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

    fun loadProducts() {
        try {
            val currentDate = LocalDate.now().toString()
            val products = queries.selectAll()
                .executeAsList()
                .filter { product ->
                    product.time.startsWith(currentDate)
                }
                .map { it.toSalesProduct() }
            _productsList.value = products
        } catch (e: Exception) {
            println("Error loading products: ${e.message}")
            _productsList.value = emptyList()
        }
    }

    fun refreshProducts() {
        loadProducts()
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

    suspend fun addProduct(
        productName: String,
        qty: Int,
        price: Double
    ) {
        try {
            mutex.withLock {
                val newProduct = SalesProducts(
                    pid = generateUniqueId(),
                    productName = productName,
                    qty = qty.toString(),
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

    suspend fun removeProductById(pid: Int) {
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

    suspend fun clearProducts() {
        try {
            mutex.withLock {
                queries.deleteAll()
                _productsList.value = emptyList()
            }
        } catch (e: Exception) {
            println("Error clearing products: ${e.message}")
        }
    }

    fun getFormattedTotalPriceOfAllProducts(): String {
        val totalPrice = getTotalPriceOfAllProducts()
        return "UGX ${"%,d".format(totalPrice.toLong())}"
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
            pid = pid,
            productName = productName,
            qty = qty,
            time = time,
            price = price
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
    val pid: Int,
    val productName: String,
    val qty: String,
    val time: String,
    val price: String
) {
    // Comprehensive validation with UGX-specific constraints
    fun isValid(): Boolean =
        productName.isNotBlank() &&
                productName.length <= 100 &&
                qty.toIntOrNull()?.let { it >= 0 } ?: false &&
                price.toDoubleOrNull()?.let { it >= 0.0 } ?: false

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
    fun formattedTime(): String =
        time.takeIf { it.isNotBlank() }?.let {
            LocalDateTime.parse(it).format(DateTimeFormatter.ofPattern("h:mm a"))
        } ?: "Unknown Time"
}