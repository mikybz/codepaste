package com.codepaste

import java.io.File

class OtherFileHandler : FileHandlerBase() {
    override fun processFile(fileContent: String, projectDir: String, config: AppConfig): String {
        // For other file types, try to determine the file name from the content
        val fileName = fileTypeDetector.determineFileName(fileContent)
            ?: throw CodePasteException("Could not determine the file name for non-Java/Kotlin file")

        logger.info("Processing other file type. Filename: $fileName")

        // Try to find the existing file to replace
        val existingFile = findExistingFile(File(projectDir), fileName)
            ?: throw CodePasteException("Could not find existing file with name '$fileName' in project directory")

        // Write the content to the file
        try {
            writeFile(existingFile, fileContent)
            return "Saved file: ${existingFile.absolutePath}"
        } catch (e: Exception) {
            throw CodePasteException("Error writing file: ${e.message}")
        }
    }
}