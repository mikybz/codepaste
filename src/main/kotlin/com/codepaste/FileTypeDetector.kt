package com.codepaste

import org.slf4j.LoggerFactory

enum class FileType {
    JAVA,
    KOTLIN,
    OTHER,
    UNKNOWN
}

class FileTypeDetector {
    private val logger = LoggerFactory.getLogger(FileTypeDetector::class.java)

    fun detectFileType(contentLines: List<String>): FileType {
        logger.debug("Detecting file type from content")

        if (contentLines.isEmpty()) {
            logger.warn("Empty content, cannot detect file type")
            return FileType.UNKNOWN
        }

        // Check for package declaration to identify Java/Kotlin files
        val packageLine = contentLines.find { it.trim().startsWith("package ") }

        // Check for Java-specific syntax
        val hasJavaImports = contentLines.any { it.trim().startsWith("import java.") || it.trim().startsWith("import javax.") }
        val hasJavaClass = contentLines.any { it.contains("class ") && it.contains("{") && !it.contains("->") }

        // Check for Kotlin-specific syntax
        val hasKotlinImports = contentLines.any { it.trim().startsWith("import kotlin.") }
        val hasKotlinSyntax = contentLines.any {
            it.contains("val ") || it.contains("var ") || it.contains("fun ") || it.contains("->")
        }

        val fileType = when {
            packageLine != null && (hasJavaImports || hasJavaClass) && !hasKotlinSyntax -> FileType.JAVA
            packageLine != null && (hasKotlinImports || hasKotlinSyntax) -> FileType.KOTLIN
            packageLine != null -> {
                // If we have a package but can't determine between Java and Kotlin, check for file extension hints
                val className = extractClassName(contentLines)
                when {
                    className.endsWith(".kt") -> FileType.KOTLIN
                    className.endsWith(".java") -> FileType.JAVA
                    // If we still can't determine, check for more Kotlin-specific features
                    contentLines.any { it.contains("companion object") || it.contains("object :") } -> FileType.KOTLIN
                    else -> FileType.JAVA // Default to Java if we have a package but can't determine further
                }
            }
            else -> {
                // Look for XML, properties, JSON, YAML, etc.
                when {
                    contentLines.any { it.trim().startsWith("<?xml") || (it.trim().startsWith("<") && it.contains(">")) } -> FileType.OTHER
                    contentLines.any { it.contains("=") && !it.contains(";") } -> FileType.OTHER
                    contentLines.any { it.trim().startsWith("{") && contentLines.any { line -> line.trim().startsWith("}") } } -> FileType.OTHER
                    contentLines.any { it.trim().startsWith("---") || it.contains(": ") } -> FileType.OTHER
                    else -> FileType.UNKNOWN
                }
            }
        }

        logger.info("Detected file type: $fileType")
        return fileType
    }

    fun extractClassName(contentLines: List<String>): String {
        // Try to find the class name
        val classLine = contentLines.find {
                line -> line.contains("class ") || line.contains("interface ") || line.contains("enum ")
        }

        return classLine?.let {
            // Extract the class name
            val keyword = when {
                it.contains("class ") -> "class "
                it.contains("interface ") -> "interface "
                it.contains("enum ") -> "enum "
                else -> return "Unknown"
            }

            val afterKeyword = it.substring(it.indexOf(keyword) + keyword.length).trim()
            val endIndex = afterKeyword.indexOfAny(charArrayOf('(', '{', ':', '<', ' ')).takeIf { idx -> idx > 0 } ?: afterKeyword.length
            afterKeyword.substring(0, endIndex).trim()
        } ?: "Unknown"
    }

    fun extractPackageName(contentLines: List<String>): String? {
        return contentLines.find { it.trim().startsWith("package ") }
            ?.replace("package", "")
            ?.replace(";", "")
            ?.trim()
    }

    fun determineFileName(fileContent: String): String? {
        // Try to extract the file name based on content patterns
        val lines = fileContent.lines()

        // Check for XML files with root element that might indicate filename
        val xmlRootElement = lines.find { it.trim().startsWith("<") && it.contains("xmlns") }
        if (xmlRootElement != null) {
            // Extract the root element name
            val rootName = xmlRootElement.trim().substringAfter("<").substringBefore(" ")
            if (rootName.isNotEmpty()) {
                return "$rootName.xml"
            }
        }

        // Check for build files
        if (fileContent.contains("dependencies") && fileContent.contains("repositories")) {
            if (fileContent.contains("buildscript") && fileContent.contains("kotlin")) {
                return "build.gradle.kts"
            } else if (fileContent.contains("plugins")) {
                return "build.gradle"
            }
        }

        // Check for property files
        if (lines.count { it.contains("=") && !it.contains(";") } > lines.size / 2) {
            return "application.properties"
        }

        // Check for YAML files
        if (lines.count { it.contains(":") && !it.contains(";") } > lines.size / 2) {
            return "config.yaml"
        }

        // Check for JSON files
        if (fileContent.trim().startsWith("{") && fileContent.trim().endsWith("}")) {
            return "config.json"
        }

        return null
    }
}