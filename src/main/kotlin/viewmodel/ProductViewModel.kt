package viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pherus.merchant.center.Database
import domain.model.Product
import domain.model.toProduct
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.text.toBigDecimalOrNull

class ProductViewModel {
    // Database setup
    private val driver = JdbcSqliteDriver(
        url = "jdbc:sqlite:sales_products.db?journal_mode=WAL&synchronous=NORMAL&cache_size=50000&mmap_size=268435456",
        schema = Database.Schema
    )

    private val database = Database(driver)
    private val queries = database.productsQueries

    // Use limited dispatcher for better resource management
    @OptIn(ExperimentalCoroutinesApi::class)
    private val limitedDispatcher = Dispatchers.IO.limitedParallelism(4)
    val scope = CoroutineScope(limitedDispatcher + SupervisorJob())

    // State management
    private val _productsList = MutableStateFlow<List<Product>>(emptyList())
    val productsList = _productsList.asStateFlow()

    private val mutex = Mutex()

    init {
        scope.launch {
            loadProducts()
        }
    }

    /**
     * Load all active products
     */
    fun loadProducts() {
        scope.launch {
            try {
                val products = queries.selectAllActive()
                    .executeAsList()
                    .map { it.toProduct() }
                _productsList.value = products
            } catch (e: Exception) {
                println("Error loading products: ${e.message}")
                _productsList.value = emptyList()
            }
        }
    }

    /**
     * Search products by name or number
     */
    fun searchProducts(query: String) {
        scope.launch {
            try {
                val products = queries.searchProducts(query, query)
                    .executeAsList()
                    .map { it.toProduct() }
                _productsList.value = products
            } catch (e: Exception) {
                println("Error searching products: ${e.message}")
                _productsList.value = emptyList()
            }
        }
    }

    /**
     * Add a new product
     */
    fun addProduct(
        productNumber: String,
        productName: String,
        unitPrice: BigDecimal,
        description: String? = null,
        category: String? = null,
        stockQuantity: Int = 0
    ) {
        scope.launch {
            try {
                mutex.withLock {
                    val now = LocalDateTime.now()
                    queries.insertProduct(
                        productNumber = productNumber,
                        productName = productName,
                        unitPrice = unitPrice.toString(),
                        description = description,
                        category = category,
                        stockQuantity = stockQuantity.toLong()
                    )
                    
                    // Reload products to get the updated list
                    loadProducts()
                }
            } catch (e: Exception) {
                println("Error adding product: ${e.message}")
            }
        }
    }

    /**
     * Update an existing product
     */
    fun updateProduct(product: Product) {
        scope.launch {
            try {
                mutex.withLock {
                    queries.updateProduct(
                        productName = product.productName,
                        unitPrice = product.unitPrice.toString(),
                        description = product.description,
                        category = product.category,
                        stockQuantity = product.stockQuantity.toLong(),
                        productId = product.productId.toLong()
                    )
                    
                    // Reload products to get the updated list
                    loadProducts()
                }
            } catch (e: Exception) {
                println("Error updating product: ${e.message}")
            }
        }
    }

    /**
     * Deactivate a product (soft delete)
     */
    fun deactivateProduct(productId: Int) {
        scope.launch {
            try {
                mutex.withLock {
                    queries.deactivateProduct(productId.toLong())
                    
                    // Reload products to get the updated list
                    loadProducts()
                }
            } catch (e: Exception) {
                println("Error deactivating product: ${e.message}")
            }
        }
    }

    /**
     * Get product by ID
     */
    suspend fun getProductById(productId: Int): Product? {
        return withContext(limitedDispatcher) {
            try {
                queries.selectProductById(productId.toLong())
                    .executeAsOneOrNull()
                    ?.toProduct()
            } catch (e: Exception) {
                println("Error getting product by ID: ${e.message}")
                null
            }
        }
    }

    /**
     * Get product by number
     */
    suspend fun getProductByNumber(productNumber: String): Product? {
        return withContext(limitedDispatcher) {
            try {
                queries.selectProductByNumber(productNumber)
                    .executeAsOneOrNull()
                    ?.toProduct()
            } catch (e: Exception) {
                println("Error getting product by number: ${e.message}")
                null
            }
        }
    }

    /**
     * Update stock quantity for a product
     */
    fun updateStockQuantity(productId: Int, newQuantity: Int) {
        scope.launch {
            try {
                mutex.withLock {
                    queries.updateStockQuantity(
                        stockQuantity = newQuantity.toLong(),
                        productId = productId.toLong()
                    )
                    
                    // Reload products to get the updated list
                    loadProducts()
                }
            } catch (e: Exception) {
                println("Error updating stock quantity: ${e.message}")
            }
        }
    }

    /**
     * Get products by category
     */
    fun getProductsByCategory(category: String) {
        scope.launch {
            try {
                val products = queries.selectProductsByCategory(category)
                    .executeAsList()
                    .map { it.toProduct() }
                _productsList.value = products
            } catch (e: Exception) {
                println("Error getting products by category: ${e.message}")
                _productsList.value = emptyList()
            }
        }
    }

    /**
     * Refresh products list
     */
    fun refreshProducts() = loadProducts()

    /**
     * Check if product number already exists
     */
    suspend fun isProductNumberExists(productNumber: String): Boolean {
        return withContext(limitedDispatcher) {
            try {
                val existingProduct = queries.selectProductByNumber(productNumber)
                    .executeAsOneOrNull()
                existingProduct != null
            } catch (e: Exception) {
                println("Error checking product number: ${e.message}")
                false
            }
        }
    }

    /**
     * Get product statistics
     */
    suspend fun getProductStats(): List<ProductStats> {
        return withContext(limitedDispatcher) {
            try {
                queries.selectProductStats()
                    .executeAsList()
                    .map { result ->
                        ProductStats(
                            category = result.category ?: "Uncategorized",
                            productCount = result.product_count?.toInt() ?: 0,
                            totalStock = result.total_stock?.toInt() ?: 0,
                            averagePrice = java.math.BigDecimal.valueOf(result.avg_price ?: 0.0)
                        )
                    }
            } catch (e: Exception) {
                println("Error getting product stats: ${e.message}")
                emptyList()
            }
        }
    }
}

/**
 * Data class for product statistics
 */
data class ProductStats(
    val category: String,
    val productCount: Int,
    val totalStock: Int,
    val averagePrice: BigDecimal
) 