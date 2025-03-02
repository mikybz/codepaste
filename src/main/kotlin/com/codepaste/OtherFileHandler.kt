package com.codepaste

import java.io.File

class OtherFileHandler : FileHandlerBase() {
    override fun processFile(fileContent: String, projectDir: String, config: AppConfig): String {
        // For other file types, try to determine the file name from the content
        val fileName = FileUtils.determineFileName(fileContent)

        logger.info("Processing other file type. Filename: $fileName")

        // Create target file in project directory
        val targetFile = File(projectDir, fileName)

        // Write the content to the file
        try {
            writeFile(targetFile, fileContent)
            return "Saved file: ${targetFile.absolutePath}"
        } catch (e: Exception) {
            throw CodePasteException("Error writing file: ${e.message}")
        }
    }
}