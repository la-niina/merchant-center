package viewmodel

import java.security.SecureRandom
import java.util.UUID

object UniqueIdGenerator {
    // Secure random generator for better uniqueness
    private val secureRandom = SecureRandom()

    // Generate a unique integer ID
    fun generateNumericId(): Int {
        return secureRandom.nextInt(Int.MAX_VALUE)
    }

    // Generate a unique UUID-based ID
    fun generateUUIDId(): String {
        return UUID.randomUUID().toString()
    }

    // Generate a compact unique ID with timestamp and random component
    fun generateCompactId(): String {
        val timestamp = System.currentTimeMillis()
        val randomComponent = secureRandom.nextInt(10000)
        return "$timestamp-$randomComponent"
    }
}
