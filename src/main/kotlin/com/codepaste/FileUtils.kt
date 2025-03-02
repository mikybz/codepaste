package com.codepaste

import java.util.regex.Pattern

object FileUtils {
    private val packagePattern = Pattern.compile("""package\s+([\w.]+)""")
    private val classPattern = Pattern.compile("""(?:public\s+)?(?:class|interface|enum)\s+(\w+)""")
    private val kotlinClassPattern = Pattern.compile("""(?:data\s+)?(?:class|interface|object)\s+(\w+)""")

    /**
     * Extracts the package name from file content
     */
    fun extractPackageName(content: String): String {
        val matcher = packagePattern.matcher(content)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            ""
        }
    }

    /**
     * Extracts the class name from Java or Kotlin file content
     */
    fun extractClassName(content: String, isKotlin: Boolean = false): String {
        val pattern = if (isKotlin) kotlinClassPattern else classPattern
        val matcher = pattern.matcher(content)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            "Untitled"
        }
    }

    /**
     * Determines a suitable filename based on file content
     */
    fun determineFileName(content: String, fileType: FileType): String {
        val defaultName = when (fileType) {
            FileType.JAVA -> "JavaClass.java"
            FileType.KOTLIN -> "KotlinFile.kt"
            FileType.OTHER -> "file.txt"
        }

        return when (fileType) {
            FileType.JAVA -> {
                val className = extractClassName(content)
                if (className.isNotEmpty()) "$className.java" else defaultName
            }
            FileType.KOTLIN -> {
                val className = extractClassName(content, true)
                if (className.isNotEmpty()) "$className.kt" else defaultName
            }
            else -> defaultName
        }
    }

    /**
     * Replaces text in content while preserving line numbers
     */
    fun replace(content: String, target: String, replacement: String): String {
        return content.replace(target, replacement)
    }

    /**
     * Creates a file path from package name
     */
    fun packageToPath(packageName: String): String {
        return packageName.replace(".", "/")
    }
}