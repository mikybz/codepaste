package com.codepaste

import java.io.File

class KotlinFileHandler : FileHandlerBase() {
    override fun processFile(fileContent: String, projectDir: String, config: AppConfig): String {
        val contentLines = fileContent.lines()
        val packageName = fileTypeDetector.extractPackageName(contentLines)
        val className = fileTypeDetector.extractClassName(contentLines)

        if (packageName == null) {
            logger.warn("No package declaration found in Kotlin file")
            throw CodePasteException("No package declaration found in Kotlin file")
        }

        logger.info("Processing Kotlin file. Package: $packageName, Class: $className")

        // Find the appropriate root directory
        val rootDir = findSourceRoot(File(projectDir), config.kotlinSourceRoots)
            ?: throw CodePasteException("Could not find Kotlin source root in project directory")

        // Create package directories if they don't exist
        val packagePath = packageName.replace('.', File.separatorChar)
        val targetDir = File(rootDir, packagePath)
        if (!targetDir.exists()) {
            logger.info("Creating package directories: ${targetDir.absolutePath}")
            val dirCreated = targetDir.mkdirs()
            if (!dirCreated) {
                logger.error("Failed to create package directories: ${targetDir.absolutePath}")
                throw CodePasteException("Failed to create package directories for Kotlin file")
            }
        }

        // Determine the file name
        val fileName = "$className.kt"
        val targetFile = File(targetDir, fileName)

        // Write the content to the file
        try {
            writeFile(targetFile, fileContent)
            return "Saved Kotlin file: ${targetFile.absolutePath}"
        } catch (e: Exception) {
            throw CodePasteException("Error writing Kotlin file: ${e.message}")
        }
    }
}