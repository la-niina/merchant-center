package viewmodel

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object TimeUtils {
    // ISO 8601 format with local date and time
    fun getCurrentTimestamp(format: TimestampFormat = TimestampFormat.ISO_LOCAL): String {
        return when (format) {
            TimestampFormat.ISO_LOCAL -> LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            TimestampFormat.READABLE -> LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            TimestampFormat.UNIX_TIMESTAMP -> System.currentTimeMillis().toString()
        }
    }

    enum class TimestampFormat {
        ISO_LOCAL,      // 2024-02-15T10:30:45.123
        READABLE,       // 2024-02-15 10:30:45
        UNIX_TIMESTAMP  // 1707989445123
    }
}

fun formatTime(isoDateTime: String): String {
    return try {
        val parsedDateTime = LocalDateTime.parse(isoDateTime)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        parsedDateTime.format(timeFormatter)
    } catch (e: Exception) {
        "Unknown Time"
    }
}