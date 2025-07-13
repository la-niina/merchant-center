package core.validation

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Comprehensive data validation utility for Merchant Center application
 */
object DataValidator {
    
    // Validation constants
    private const val MAX_PRODUCT_NAME_LENGTH = 100
    private const val MIN_PRODUCT_NAME_LENGTH = 1
    private const val MAX_QUANTITY = 999999
    private const val MIN_QUANTITY = 1
    private const val MAX_PRICE = 999999999.99
    private const val MIN_PRICE = 0.01
    
    // Currency symbols and separators to remove
    private val CURRENCY_SYMBOLS = setOf("UGX", "USD", "EUR", "£", "$", "€", "₦", "₹", "¥")
    private val PRICE_SEPARATORS = setOf(",", " ", "_")
    
    /**
     * Validation result sealed class
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
    
    /**
     * Validates product name
     */
    fun validateProductName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Product name cannot be empty")
            name.length < MIN_PRODUCT_NAME_LENGTH -> ValidationResult.Error("Product name must be at least $MIN_PRODUCT_NAME_LENGTH character")
            name.length > MAX_PRODUCT_NAME_LENGTH -> ValidationResult.Error("Product name cannot exceed $MAX_PRODUCT_NAME_LENGTH characters")
            containsInvalidCharacters(name) -> ValidationResult.Error("Product name contains invalid characters")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates quantity
     */
    fun validateQuantity(quantity: String): ValidationResult {
        return when {
            quantity.isBlank() -> ValidationResult.Error("Quantity cannot be empty")
            !quantity.matches(Regex("^\\d+$")) -> ValidationResult.Error("Quantity must be a positive number")
            else -> {
                val qty = quantity.toIntOrNull()
                when {
                    qty == null -> ValidationResult.Error("Invalid quantity format")
                    qty < MIN_QUANTITY -> ValidationResult.Error("Quantity must be at least $MIN_QUANTITY")
                    qty > MAX_QUANTITY -> ValidationResult.Error("Quantity cannot exceed $MAX_QUANTITY")
                    else -> ValidationResult.Success
                }
            }
        }
    }
    
    /**
     * Validates and sanitizes price input
     */
    fun validateAndSanitizePrice(priceInput: String): ValidationResult {
        if (priceInput.isBlank()) {
            return ValidationResult.Error("Price cannot be empty")
        }
        
        val sanitizedPrice = sanitizePriceInput(priceInput)
        
        return when {
            sanitizedPrice.isBlank() -> ValidationResult.Error("Invalid price format")
            !sanitizedPrice.matches(Regex("^\\d+(\\.\\d{1,2})?$")) -> ValidationResult.Error("Price must be a valid number with up to 2 decimal places")
            else -> {
                val price = sanitizedPrice.toBigDecimalOrNull()
                when {
                    price == null -> ValidationResult.Error("Invalid price format")
                    price < BigDecimal.valueOf(MIN_PRICE) -> ValidationResult.Error("Price must be at least ${MIN_PRICE}")
                    price > BigDecimal.valueOf(MAX_PRICE) -> ValidationResult.Error("Price cannot exceed ${MAX_PRICE}")
                    else -> ValidationResult.Success
                }
            }
        }
    }
    
    /**
     * Sanitizes price input by removing currency symbols and separators
     */
    fun sanitizePriceInput(priceInput: String): String {
        var sanitized = priceInput.trim()
        
        // Remove currency symbols
        CURRENCY_SYMBOLS.forEach { symbol ->
            sanitized = sanitized.replace(symbol, "", ignoreCase = true)
        }
        
        // Remove separators
        PRICE_SEPARATORS.forEach { separator ->
            sanitized = sanitized.replace(separator, "")
        }
        
        return sanitized.trim()
    }
    
    /**
     * Validates date time string
     */
    fun validateDateTime(dateTimeString: String): ValidationResult {
        return try {
            LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ValidationResult.Success
        } catch (e: DateTimeParseException) {
            ValidationResult.Error("Invalid date format: ${e.message}")
        }
    }
    
    /**
     * Validates complete product data
     */
    fun validateProductData(
        productName: String,
        quantity: String,
        unitPrice: String
    ): ValidationResult {
        // Validate product name
        val nameValidation = validateProductName(productName)
        if (nameValidation is ValidationResult.Error) {
            return nameValidation
        }
        
        // Validate quantity
        val quantityValidation = validateQuantity(quantity)
        if (quantityValidation is ValidationResult.Error) {
            return quantityValidation
        }
        
        // Validate unit price
        val priceValidation = validateAndSanitizePrice(unitPrice)
        if (priceValidation is ValidationResult.Error) {
            return priceValidation
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Checks if product name contains invalid characters
     */
    private fun containsInvalidCharacters(name: String): Boolean {
        // Allow letters, numbers, spaces, hyphens, and common punctuation
        val validPattern = Regex("^[a-zA-Z0-9\\s\\-.,&'()]+$")
        return !validPattern.matches(name)
    }
    
    /**
     * Formats price for display
     */
    fun formatPriceForDisplay(price: BigDecimal): String {
        return "%,.2f".format(price)
    }
    
    /**
     * Formats price for display with currency
     */
    fun formatPriceWithCurrency(price: BigDecimal, currency: String = "UGX"): String {
        return "${formatPriceForDisplay(price)} $currency"
    }
    
    /**
     * Calculates total price from quantity and unit price
     */
    fun calculateTotalPrice(quantity: Int, unitPrice: BigDecimal): BigDecimal {
        return unitPrice.multiply(BigDecimal.valueOf(quantity.toLong()))
    }
    
    /**
     * Sanitizes product name for database storage
     */
    fun sanitizeProductName(name: String): String {
        return name.trim()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .replace(Regex("[<>\"'&]"), "") // Remove potentially dangerous characters
    }
    
    /**
     * Validates date range
     */
    fun validateDateRange(startDate: String, endDate: String): ValidationResult {
        return try {
            val start = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val end = LocalDateTime.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            
            if (start.isAfter(end)) {
                ValidationResult.Error("Start date cannot be after end date")
            } else {
                ValidationResult.Success
            }
        } catch (e: DateTimeParseException) {
            ValidationResult.Error("Invalid date format: ${e.message}")
        }
    }
} 