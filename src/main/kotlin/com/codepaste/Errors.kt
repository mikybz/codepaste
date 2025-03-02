package com.codepaste

// Instead of extending Exception classes directly, use composition
// or create your own exception hierarchy

// Option 1: Composition approach
class ConfigurationError(message: String, cause: Throwable? = null) {
    private val exception = RuntimeException(message, cause)

    fun getMessage() = exception.message
    fun getCause() = exception.cause
    // Add other methods as needed
}

class DatabaseError(message: String, cause: Throwable? = null) {
    private val exception = RuntimeException(message, cause)

    fun getMessage() = exception.message
    fun getCause() = exception.cause
}

// Option 2: Create your own exception hierarchy
open class ApplicationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

class ConfigLoadingException(message: String, cause: Throwable? = null) :
    ApplicationException(message, cause)

class DatabaseConnectionException(message: String, cause: Throwable? = null) :
    ApplicationException(message, cause)

class FileProcessingException(message: String, cause: Throwable? = null) :
    ApplicationException(message, cause)

class NetworkConnectivityException(message: String, cause: Throwable? = null) :
    ApplicationException(message, cause)