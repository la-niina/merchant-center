package viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pherus.merchant.center.Database
import kotlin.time.Duration.Companion.seconds

class AuthViewModel {
    private val driver = JdbcSqliteDriver(
        url = "jdbc:sqlite:user_authentication.db",
        schema = Database.Schema
    )
    private val database = Database(driver)
    private val queries = database.userAuthenticationQueries

    private val viewModelScope = CoroutineScope(Dispatchers.Default)
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    data class AuthState(
        val isAuthenticated: Boolean = false,
        val errorMessage: String? = null,
        val timeRemaining: Long = 0,
        val isSessionExpired: Boolean = false
    )

    init {
        // Ensure default authentication is set up
        initializeDefaultAuth()
        startSessionMonitoring()
    }

    private fun initializeDefaultAuth() {
        try {
            // Check if any record exists
            val existingRecords = queries.checkPassword("12345678").executeAsOne()

            if (existingRecords == 0L) {
                // No records, insert default
                queries.initializeDefaultAuth()
            }
        } catch (e: Exception) {
            _authState.update {
                it.copy(
                    errorMessage = "Authentication setup failed: ${e.message}",
                    isAuthenticated = false
                )
            }
        }
    }

    private fun startSessionMonitoring() {
        viewModelScope.launch {
            while (true) {
                checkSessionValidity()
                delay(1.seconds)
            }
        }
    }

    private fun checkSessionValidity() {
        try {
            // Prevent immediate expiration by adding a buffer
            val isValid = try {
                val currentTime = System.currentTimeMillis() / 1000
                val lastLogin = queries.getLastLoginTimestamp().executeAsOne().toLong()
                val elapsedTime = currentTime - lastLogin

                // Increase the session timeout and add a small buffer
                elapsedTime <= 3900 && queries.getCurrentLoginStatus().executeAsOne() == 1
            } catch (e: Exception) {
                false
            }

            if (!isValid) {
                // Session expired or not logged in
                queries.logout()
                _authState.update {
                    it.copy(
                        isAuthenticated = false,
                        isSessionExpired = true,
                        errorMessage = "Session expired",
                        timeRemaining = 0
                    )
                }
            } else {
                // Calculate and update remaining time
                val remainingTime = calculateRemainingTime()
                _authState.update {
                    it.copy(
                        isAuthenticated = true,
                        timeRemaining = remainingTime,
                        isSessionExpired = false,
                        errorMessage = null
                    )
                }
            }
        } catch (e: Exception) {
            _authState.update {
                it.copy(
                    isAuthenticated = false,
                    isSessionExpired = true,
                    errorMessage = "Session validation failed: ${e.message}"
                )
            }
        }
    }

    private fun calculateRemainingTime(): Long {
        return try {
            val lastLogin = queries.getLastLoginTimestamp().executeAsOne().toLong()
            val currentTime = System.currentTimeMillis() / 1000
            val elapsedTime = currentTime - lastLogin

            // Increase the session timeout and add a buffer
            maxOf(0L, 3900L - elapsedTime)
        } catch (e: Exception) {
            0L
        }
    }

    fun authenticate(password: String) {
        try {
            val isValid = queries.checkPassword(password.trim()).executeAsOne() > 0
            if (isValid) {
                // Force login and update timestamp
                queries.forceLogin()

                _authState.update {
                    it.copy(
                        isAuthenticated = true,
                        errorMessage = null,
                        timeRemaining = 3600, // 1 hour session
                        isSessionExpired = false
                    )
                }
            } else {
                _authState.update {
                    it.copy(
                        isAuthenticated = false,
                        errorMessage = "Incorrect password",
                        isSessionExpired = false
                    )
                }
            }
        } catch (e: Exception) {
            _authState.update {
                it.copy(
                    isAuthenticated = false,
                    errorMessage = "Authentication failed: ${e.message}",
                    isSessionExpired = false
                )
            }
        }
    }
    
    fun changePassword(oldPassword: String, newPassword: String) {
        try {
            // Verify current password
            val currentPassword = queries.getCurrentPassword().executeAsOne()

            if (currentPassword == oldPassword) {
                queries.updatePassword(newPassword)

                _authState.update {
                    it.copy(
                        errorMessage = null,
                        isSessionExpired = false
                    )
                }
            } else {
                _authState.update {
                    it.copy(
                        errorMessage = "Current password is incorrect",
                        isSessionExpired = false
                    )
                }
            }
        } catch (e: Exception) {
            _authState.update {
                it.copy(
                    errorMessage = "Password change failed: ${e.message}",
                    isSessionExpired = false
                )
            }
        }
    }

    fun logout() {
        queries.logout()
        _authState.update {
            it.copy(
                isAuthenticated = false,
                errorMessage = null,
                timeRemaining = 0,
                isSessionExpired = false
            )
        }
    }

    fun acknowledgeSessionExpiration() {
        _authState.update {
            it.copy(isSessionExpired = false)
        }
    }
}