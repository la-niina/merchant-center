package domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Improved SalesProduct data model with proper data types
 */
data class SalesProduct(
    val pid: Int,
    val productName: String,
    val qty: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val time: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    
    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a")
    }
    
    /**
     * Validates if the sales product data is valid
     */
    fun isValid(): Boolean {
        return productName.isNotBlank() && 
               productName.length <= 100 &&
               qty > 0 && 
               qty <= 999999 &&
               unitPrice >= BigDecimal.ZERO &&
               totalPrice >= BigDecimal.ZERO &&
               unitPrice.multiply(BigDecimal.valueOf(qty.toLong())) == totalPrice
    }
    
    /**
     * Formats the unit price for display
     */
    fun formattedUnitPrice(): String {
        return "%,.2f".format(unitPrice)
    }
    
    /**
     * Formats the total price for display
     */
    fun formattedTotalPrice(): String {
        return "%,.2f".format(totalPrice)
    }
    
    /**
     * Formats the total price with currency
     */
    fun formattedTotalPriceWithCurrency(currency: String = "UGX"): String {
        return "${formattedTotalPrice()} $currency"
    }
    
    /**
     * Formats the unit price with currency
     */
    fun formattedUnitPriceWithCurrency(currency: String = "UGX"): String {
        return "${formattedUnitPrice()} $currency"
    }
    
    /**
     * Formats the time for display
     */
    fun formattedTime(): String {
        return time.format(timeFormatter)
    }
    
    /**
     * Formats the date for display
     */
    fun formattedDate(): String {
        return time.format(dateFormatter)
    }
    
    /**
     * Formats the full date and time for display
     */
    fun formattedDateTime(): String {
        return time.format(fullDateTimeFormatter)
    }
    
    /**
     * Gets the sale date as LocalDate
     */
    fun saleDate(): java.time.LocalDate {
        return time.toLocalDate()
    }
    
    /**
     * Calculates the total value (quantity * unit price)
     */
    fun calculateTotalValue(): BigDecimal {
        return unitPrice.multiply(BigDecimal.valueOf(qty.toLong()))
    }
    
    /**
     * Checks if the total price matches the calculated value
     */
    fun isTotalPriceCorrect(): Boolean {
        return totalPrice == calculateTotalValue()
    }
    
    /**
     * Creates a copy with updated total price
     */
    fun withRecalculatedTotalPrice(): SalesProduct {
        return copy(totalPrice = calculateTotalValue())
    }
}

/**
 * Extension function to convert database result to domain model
 */
fun pherus.merchant.center.SalesProducts.toSalesProduct(): SalesProduct {
    return SalesProduct(
        pid = pid.toInt(),
        productName = productName,
        qty = qty.toInt(),
        unitPrice = unitPrice.toBigDecimal(),
        totalPrice = totalPrice.toBigDecimal(),
        time = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        createdAt = LocalDateTime.parse(created_at, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = LocalDateTime.parse(updated_at, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

/**
 * Extension function to convert domain model to database model
 */
fun SalesProduct.toDatabaseModel(): pherus.merchant.center.SalesProducts {
    return pherus.merchant.center.SalesProducts(
        pid = pid.toLong(),
        productName = productName,
        qty = qty.toLong(),
        unitPrice = unitPrice.toString(),
        totalPrice = totalPrice.toString(),
        time = time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        created_at = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updated_at = updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
} 