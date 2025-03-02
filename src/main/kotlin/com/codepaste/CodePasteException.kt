package com.codepaste

// Base exception for the entire application
open class CodePasteException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

// Specific exception types
class ClipboardException(message: String, cause: Throwable? = null) :
    CodePasteException(message, cause)

class ConfigurationException(message: String, cause: Throwable? = null) :
    CodePasteException(message, cause)

class DatabaseException(message: String, cause: Throwable? = null) :
    CodePasteException(message, cause)

class FileException(message: String, cause: Throwable? = null) :
    CodePasteException(message, cause)

class NetworkIssueException(message: String, cause: Throwable? = null) :
    CodePasteException(message, cause)