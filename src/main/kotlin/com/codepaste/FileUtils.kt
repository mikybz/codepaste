package com.codepaste

import org.slf4j.LoggerFactory

object FileUtils {
    private val logger = LoggerFactory.getLogger(FileUtils::class.java)
    
    // Patterns for file type detection
    private val kotlinExtPattern = java.util.regex.Pattern.compile("""\.kt$|\.kts$""")
    private val javaExtPattern = java.util.regex.Pattern.compile("""\.java$""")
    
    // Patterns for content extraction
    private val packagePattern = java.util.regex.Pattern.compile("""package\s+([\w.]+)""")
    private val javaClassPattern = java.util.regex.Pattern.compile("""(?:public\s+)?(?:class|interface|enum)\s+(\w+)""")
    private val kotlinClassPattern = java.util.regex.Pattern.compile("""(?:data\s+)?(?:class|interface|object)\s+(\w+)""")
    
    // Language syntax patterns for detection
    private val kotlinSyntaxPatterns = listOf(
        java.util.regex.Pattern.compile("""package\s+[\w.]+"""),              // package declaration
        java.util.regex.Pattern.compile("""import\s+[\w.]+"""),               // import statements
        java.util.regex.Pattern.compile("""fun\s+\w+\s*\("""),                // function declarations
        java.util.regex.Pattern.compile("""val\s+\w+\s*[:=]"""),              // val declarations
        java.util.regex.Pattern.compile("""var\s+\w+\s*[:=]"""),              // var declarations
        java.util.regex.Pattern.compile("""class\s+\w+(\s*[:(]|$)"""),        // class declarations
        java.util.regex.Pattern.compile("""object\s+\w+(\s*[:(]|$)"""),       // object declarations
        java.util.regex.Pattern.compile("""data\s+class"""),                  // data class
        java.util.regex.Pattern.compile("""companion\s+object"""),            // companion object
        java.util.regex.Pattern.compile("""when\s*\(""")                      // when expression
    )
    
    private val javaSyntaxPatterns = listOf(
        java.util.regex.Pattern.compile("""public\s+(class|interface|enum)"""),   // public class/interface/enum
        java.util.regex.Pattern.compile("""private\s+\w+\s+\w+\s*=?"""),          // private field
        java.util.regex.Pattern.compile("""public\s+\w+\s+\w+\s*\("""),           // public method
        java.util.regex.Pattern.compile("""public\s+static\s+void\s+main"""),     // main method
        java.util.regex.Pattern.compile("""@Override"""),                          // @Override annotation
        java.util.regex.Pattern.compile("""System\.out\.println"""),              // System.out.println
        java.util.regex.Pattern.compile("""instanceof\s+\w+"""),                  // instanceof operator
        java.util.regex.Pattern.compile("""try\s*\{.*\}\s*catch\s*\(""", java.util.regex.Pattern.DOTALL)  // try-catch blocks
    )

    /**
     * Detects the type of file based on content and optional filename
     */
    fun detectFileType(content: String, fileName: String? = null): FileType {
        // First check by file name/extension if available
        if (fileName != null) {
            if (kotlinExtPattern.matcher(fileName).find()) {
                logger.debug("Detected Kotlin file by extension: $fileName")
                return FileType.KOTLIN
            }
            if (javaExtPattern.matcher(fileName).find()) {
                logger.debug("Detected Java file by extension: $fileName")
                return FileType.JAVA
            }
        }
        
        // Next, analyze content for language-specific markers
        val kotlinScore = kotlinSyntaxPatterns.count { it.matcher(content).find() }
        val javaScore = javaSyntaxPatterns.count { it.matcher(content).find() }
        
        logger.debug("Content analysis scores - Kotlin: $kotlinScore, Java: $javaScore")
        
        return when {
            kotlinScore > javaScore -> {
                logger.info("Detected Kotlin file by content analysis")
                FileType.KOTLIN
            }
            javaScore > kotlinScore -> {
                logger.info("Detected Java file by content analysis")
                FileType.JAVA
            }
            // If scores are tied, additional heuristics can be applied
            content.contains("package") && !content.contains("public class") -> {
                logger.info("Detected Kotlin file (tie-breaker)")
                FileType.KOTLIN
            }
            content.contains("public class") -> {
                logger.info("Detected Java file (tie-breaker)")
                FileType.JAVA
            }
            else -> {
                logger.info("Could not determine file type, defaulting to OTHER")
                FileType.OTHER
            }
        }
    }

    /**
     * Extracts the package name from content lines
     */
    fun extractPackageName(contentLines: List<String>): String? {
        for (line in contentLines) {
            val matcher = packagePattern.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    /**
     * Extracts the package name from a content string
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
     * Extracts the class name from content lines
     */
    fun extractClassName(contentLines: List<String>): String {
        // Combined pattern that handles various declaration forms
        val typeDeclarationPattern = java.util.regex.Pattern.compile(
            """(?:public\s+|private\s+|protected\s+|internal\s+|data\s+|sealed\s+|open\s+|abstract\s+)?
                (?:class|interface|enum\s+class|object|annotation\s+class)\s+
                (\w+)""".trimIndent().replace("\n", ""),
            java.util.regex.Pattern.COMMENTS
        )
        
        for (line in contentLines) {
            val matcher = typeDeclarationPattern.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        
        // Fallback to original patterns if the enhanced pattern doesn't match
        for (line in contentLines) {
            // Try Java pattern
            var matcher = javaClassPattern.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }
            
            // Try Kotlin pattern
            matcher = kotlinClassPattern.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        
        return "Untitled"
    }

    /**
     * Extracts the class name from content string
     */
    fun extractClassName(content: String, isKotlin: Boolean = false): String {
        // Use the more robust implementation by converting to lines
        return extractClassName(content.lines())
    }

    /**
     * Determines a suitable filename based on file content
     */
    fun determineFileName(content: String): String {
        val fileType = detectFileType(content)
        val contentLines = content.lines()
        val className = extractClassName(contentLines)
        
        return when (fileType) {
            FileType.JAVA -> "$className.java"
            FileType.KOTLIN -> "$className.kt"
            FileType.OTHER -> "$className.txt"
        }
    }

    /**
     * Creates a file path from package name
     */
    fun packageToPath(packageName: String): String {
        return packageName.replace(".", "/")
    }
}