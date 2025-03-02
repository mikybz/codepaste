package com.codepaste

import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class FileTypeDetector {
    private val logger = LoggerFactory.getLogger(FileTypeDetector::class.java)

    // File extension patterns
    private val kotlinPattern = Pattern.compile("""\.kt$|\.kts$""")
    private val javaPattern = Pattern.compile("""\.java$""")

    // Language syntax patterns
    private val kotlinSyntaxPatterns = listOf(
        Pattern.compile("""package\s+[\w.]+"""),              // package declaration
        Pattern.compile("""import\s+[\w.]+"""),               // import statements
        Pattern.compile("""fun\s+\w+\s*\("""),                // function declarations
        Pattern.compile("""val\s+\w+\s*[:=]"""),              // val declarations
        Pattern.compile("""var\s+\w+\s*[:=]"""),              // var declarations
        Pattern.compile("""class\s+\w+(\s*[:(]|$)"""),        // class declarations
        Pattern.compile("""object\s+\w+(\s*[:(]|$)"""),       // object declarations
        Pattern.compile("""data\s+class"""),                  // data class
        Pattern.compile("""companion\s+object"""),            // companion object
        Pattern.compile("""when\s*\(""")                      // when expression
    )

    private val javaSyntaxPatterns = listOf(
        Pattern.compile("""public\s+(class|interface|enum)"""),   // public class/interface/enum
        Pattern.compile("""private\s+\w+\s+\w+\s*=?"""),          // private field
        Pattern.compile("""public\s+\w+\s+\w+\s*\("""),           // public method
        Pattern.compile("""public\s+static\s+void\s+main"""),     // main method
        Pattern.compile("""@Override"""),                          // @Override annotation
        Pattern.compile("""System\.out\.println"""),              // System.out.println
        Pattern.compile("""instanceof\s+\w+"""),                  // instanceof operator
        Pattern.compile("""try\s*\{.*\}\s*catch\s*\(""", Pattern.DOTALL)  // try-catch blocks
    )

    fun detectFileType(content: String, fileName: String? = null): FileType {
        // First check by file name/extension if available
        if (fileName != null) {
            if (kotlinPattern.matcher(fileName).find()) {
                logger.debug("Detected Kotlin file by extension: $fileName")
                return FileType.KOTLIN
            }
            if (javaPattern.matcher(fileName).find()) {
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

    fun extractPackageName(contentLines: List<String>): String? {
        val packagePattern = Pattern.compile("""package\s+([\w.]+)""")
        for (line in contentLines) {
            val matcher = packagePattern.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    fun extractClassName(contentLines: List<String>): String {
        val javaClassPattern = Pattern.compile("""(?:public\s+)?(?:class|interface|enum)\s+(\w+)""")
        val kotlinClassPattern = Pattern.compile("""(?:data\s+)?(?:class|interface|object)\s+(\w+)""")

        for (line in contentLines) {
            // Try Java pattern first
            var matcher = javaClassPattern.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }

            // Then try Kotlin pattern
            matcher = kotlinClassPattern.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }

        return "Untitled"
    }

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
}

enum class FileType {
    JAVA, KOTLIN, OTHER
}