package domain.model

import pherus.merchant.center.Products
import pherus.merchant.center.SelectAllActive
import pherus.merchant.center.SearchProducts
import pherus.merchant.center.SelectProductById
import pherus.merchant.center.SelectProductByNumber
import pherus.merchant.center.SelectProductsByCategory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

/**
 * Helper function to parse date strings with multiple format support
 */
private fun parseDateTime(dateString: String?): LocalDateTime {
    if (dateString == null || dateString.isBlank()) {
        return LocalDateTime.now()
    }
    
    return try {
        // Try ISO format first
        LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } catch (e: Exception) {
        try {
            // Try SQLite default format (YYYY-MM-DD HH:MM:SS)
            LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (e: Exception) {
            try {
                // Try with milliseconds
                LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            } catch (e: Exception) {
                // If all parsing fails, return current time
                LocalDateTime.now()
            }
        }
    }
}

/**
 * Domain model for Product entity
 */
data class Product(
    val productId: Int,
    val productNumber: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val description: String?,
    val category: String?,
    val stockQuantity: Int,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * Formats the unit price for display
     */
    fun formattedUnitPrice(): String {
        return NumberFormat.getNumberInstance(Locale.US).format(unitPrice)
    }

    /**
     * Formats the stock quantity for display
     */
    fun formattedStockQuantity(): String {
        return NumberFormat.getNumberInstance(Locale.US).format(stockQuantity)
    }

    /**
     * Formats the creation date for display
     */
    fun formattedCreatedAt(): String {
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }

    /**
     * Formats the updated date for display
     */
    fun formattedUpdatedAt(): String {
        return updatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }

    /**
     * Gets display name with product number
     */
    fun displayName(): String {
        return "$productNumber - $productName"
    }

    /**
     * Checks if product is in stock
     */
    fun isInStock(): Boolean {
        return stockQuantity > 0
    }

    /**
     * Gets stock status text
     */
    fun stockStatus(): String {
        return when {
            stockQuantity <= 0 -> "Out of Stock"
            stockQuantity <= 10 -> "Low Stock ($stockQuantity)"
            else -> "In Stock ($stockQuantity)"
        }
    }

    companion object {
        /**
         * Creates a new product with default values
         */
        fun create(
            productNumber: String,
            productName: String,
            unitPrice: BigDecimal,
            description: String? = null,
            category: String? = null,
            stockQuantity: Int = 0
        ): Product {
            val now = LocalDateTime.now()
            return Product(
                productId = 0, // Will be set by database
                productNumber = productNumber,
                productName = productName,
                unitPrice = unitPrice,
                description = description,
                category = category,
                stockQuantity = stockQuantity,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

/**
 * Extension function to convert database model to domain model
 */
fun Products.toProduct(): Product {
    return Product(
        productId = productId?.toInt() ?: 0,
        productNumber = productNumber ?: "",
        productName = productName ?: "",
        unitPrice = unitPrice?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        description = description,
        category = category,
        stockQuantity = stockQuantity?.toInt() ?: 0,
        isActive = (isActive ?: 1L) == 1L,
        createdAt = parseDateTime(created_at),
        updatedAt = parseDateTime(updated_at)
    )
}

/**
 * Extension function to convert SelectAllActive to domain model
 */
fun SelectAllActive.toProduct(): Product {
    return Product(
        productId = productId.toInt(),
        productNumber = productNumber,
        productName = productName,
        unitPrice = unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        description = description,
        category = category,
        stockQuantity = stockQuantity?.toInt() ?: 0,
        isActive = true,
        createdAt = parseDateTime(created_at),
        updatedAt = parseDateTime(updated_at)
    )
}

/**
 * Extension function to convert SearchProducts to domain model
 */
fun SearchProducts.toProduct(): Product {
    return Product(
        productId = productId.toInt(),
        productNumber = productNumber,
        productName = productName,
        unitPrice = unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        description = description,
        category = category,
        stockQuantity = stockQuantity?.toInt() ?: 0,
        isActive = true,
        createdAt = parseDateTime(created_at),
        updatedAt = parseDateTime(updated_at)
    )
}

/**
 * Extension function to convert SelectProductById to domain model
 */
fun SelectProductById.toProduct(): Product {
    return Product(
        productId = productId.toInt(),
        productNumber = productNumber,
        productName = productName,
        unitPrice = unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        description = description,
        category = category,
        stockQuantity = stockQuantity?.toInt() ?: 0,
        isActive = true,
        createdAt = parseDateTime(created_at),
        updatedAt = parseDateTime(updated_at)
    )
}

/**
 * Extension function to convert SelectProductByNumber to domain model
 */
fun SelectProductByNumber.toProduct(): Product {
    return Product(
        productId = productId.toInt(),
        productNumber = productNumber,
        productName = productName,
        unitPrice = unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        description = description,
        category = category,
        stockQuantity = stockQuantity?.toInt() ?: 0,
        isActive = true,
        createdAt = parseDateTime(created_at),
        updatedAt = parseDateTime(updated_at)
    )
}

/**
 * Extension function to convert SelectProductsByCategory to domain model
 */
fun SelectProductsByCategory.toProduct(): Product {
    return Product(
        productId = productId.toInt(),
        productNumber = productNumber,
        productName = productName,
        unitPrice = unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        description = description,
        category = category,
        stockQuantity = stockQuantity?.toInt() ?: 0,
        isActive = true,
        createdAt = parseDateTime(created_at),
        updatedAt = parseDateTime(updated_at)
    )
}

/**
 * Extension function to convert domain model to database model
 */
fun Product.toDatabaseModel(): Products {
    return Products(
        productId = productId.toLong(),
        productNumber = productNumber,
        productName = productName,
        unitPrice = unitPrice.toString(),
        description = description,
        category = category,
        stockQuantity = stockQuantity.toLong(),
        isActive = if (isActive) 1L else 0L,
        created_at = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updated_at = updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
} 