package viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pherus.merchant.center.Database

class StockViewModel {
    private val driver = JdbcSqliteDriver(
        url = "jdbc:sqlite:stock_products.db",
        schema = Database.Schema
    )
    private val database = Database(driver)
    private val queries = database.stockProductsQueries

    private val _stockState = MutableStateFlow(StockState())
    val stockState: StateFlow<StockState> = _stockState.asStateFlow()

    data class StockState(
        val products: List<Product> = emptyList(),
        val lowStockProducts: List<Product> = emptyList(),
        val totalInventoryValue: Double = 0.0,
        val totalSales: Double = 0.0,
        val error: String? = null
    )

    data class Product(
        val id: String,
        val productName: String,
        val description: String?,
        val estimatedPrice: Double,
        val currentStock: Int,
        val minimumStockThreshold: Int,
        val totalSoldStock: Int,
        val unitOfMeasurement: String,
        val lastRestockedDate: Long?
    )

    init {
        fetchProducts()
        fetchLowStockProducts()
        calculateInventoryValue()
    }

    fun addProduct(
        productName: String,
        description: String? = null,
        estimatedPrice: Double,
        initialStock: Int,
        minimumStockThreshold: Int = 10,
        unitOfMeasurement: String = "pieces"
    ): String {
        // Generate a unique ID
        var productId: String
        do {
            productId = UniqueIdGenerator.generateUUIDId()
        } while (isProductIdExists(productId))

        try {
            queries.insertProduct(
                id = productId,
                productName = productName,
                description = description,
                estimatedPrice = estimatedPrice,
                currentStock = initialStock,
                minimumStockThreshold = minimumStockThreshold,
                unitOfMeasurement = unitOfMeasurement
            )

            // Refresh data
            fetchProducts()
            fetchLowStockProducts()
            calculateInventoryValue()

            return productId
        } catch (e: Exception) {
            _stockState.update {
                it.copy(error = "Failed to add product: ${e.message}")
            }
            throw e
        }
    }

    private fun isProductIdExists(productId: String): Boolean {
        return try {
            queries.checkProductIdExists(productId).executeAsOne().toInt() > 0
        } catch (e: Exception) {
            false
        }
    }

    fun fetchProducts() {
        try {
            val products = queries.getAllProducts().executeAsList().map { product ->
                Product(
                    id = product.id,
                    productName = product.productName,
                    description = product.description,
                    estimatedPrice = product.estimatedPrice,
                    currentStock = product.currentStock,
                    minimumStockThreshold = product.minimumStockThreshold,
                    totalSoldStock = product.totalSoldStock,
                    unitOfMeasurement = product.unitOfMeasurement ?: "pieces",
                    lastRestockedDate = product.lastRestockedDate?.toLong()
                )
            }

            _stockState.update {
                it.copy(products = products)
            }
        } catch (e: Exception) {
            _stockState.update {
                it.copy(error = "Failed to fetch products: ${e.message}")
            }
        }
    }

    fun fetchLowStockProducts() {
        try {
            val lowStockProducts = queries.getLowStockProducts().executeAsList().map { product ->
                Product(
                    id = product.id,
                    productName = product.productName,
                    description = product.description,
                    estimatedPrice = product.estimatedPrice,
                    currentStock = product.currentStock,
                    minimumStockThreshold = product.minimumStockThreshold,
                    totalSoldStock = product.totalSoldStock,
                    unitOfMeasurement = product.unitOfMeasurement ?: "pieces",
                    lastRestockedDate = product.lastRestockedDate?.toLong()
                )
            }

            _stockState.update {
                it.copy(lowStockProducts = lowStockProducts)
            }
        } catch (e: Exception) {
            _stockState.update {
                it.copy(error = "Failed to fetch low stock products: ${e.message}")
            }
        }
    }

    fun updateStock(productId: String, quantityToAdd: Int) {
        try {
            queries.addStock(quantityToAdd, productId)
            fetchProducts()
            fetchLowStockProducts()
            calculateInventoryValue()
        } catch (e: Exception) {
            _stockState.update {
                it.copy(error = "Failed to update stock: ${e.message}")
            }
        }
    }

    fun sellProduct(productId: String, quantitySold: Int) {
        try {
            queries.removeStock(quantitySold, quantitySold, productId)
            fetchProducts()
            fetchLowStockProducts()
            calculateInventoryValue()
        } catch (e: Exception) {
            _stockState.update {
                it.copy(error = "Failed to sell product: ${e.message}")
            }
        }
    }

    fun deleteProduct(productId: String) {
        try {
            queries.deleteProduct(productId)
            fetchProducts()
            fetchLowStockProducts()
            calculateInventoryValue()
        } catch (e: Exception) {
            _stockState.update {
                it.copy(error = "Failed to delete product: ${e.message}")
            }
        }
    }

    private fun calculateInventoryValue() {
        try {
            val inventoryValue = queries.getTotalInventoryValue().executeAsOne()
            _stockState.update {
                it.copy(
                    totalInventoryValue = inventoryValue.totalValue?.toDouble() ?: 0.0,
                    totalSales = inventoryValue.totalSales?.toDouble() ?: 0.0
                )
            }
        } catch (e: Exception) {
            _stockState.update {
                it.copy(error = "Failed to calculate inventory value: ${e.message}")
            }
        }
    }

    object UniqueIdGenerator {
        fun generateUUIDId(): String {
            return java.util.UUID.randomUUID().toString()
        }
    }
}