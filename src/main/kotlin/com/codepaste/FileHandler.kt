package com.codepaste

import org.slf4j.LoggerFactory

class FileHandler {
    private val logger = LoggerFactory.getLogger(FileHandler::class.java)
    private val configManager = ConfigManager()
    private val fileTypeDetector = FileTypeDetector()
    private val javaHandler = JavaFileHandler()
    private val kotlinHandler = KotlinFileHandler()
    private val otherHandler = OtherFileHandler()

    fun processAndSaveFile(fileContent: String, projectDir: String): String {
        val config = configManager.loadConfig()

        // Check if it's an empty file
        if (fileContent.isEmpty()) {
            logger.warn("Clipboard content is empty")
            return "Clipboard content is empty"
        }

        try {
            // Try to determine file type
            val fileType = fileTypeDetector.detectFileType(fileContent)
            logger.info("Processing file of type: $fileType")

            return when (fileType) {
                FileType.JAVA -> javaHandler.processFile(fileContent, projectDir, config)
                FileType.KOTLIN -> kotlinHandler.processFile(fileContent, projectDir, config)
                FileType.OTHER -> otherHandler.processFile(fileContent, projectDir, config)
            }
        } catch (e: CodePasteException) {
            logger.error("Error processing file: ${e.message}")
            return "Error: ${e.message}"
        } catch (e: Exception) {
            logger.error("Unexpected error: ${e.message}", e)
            return "Unexpected error: ${e.message}"
        }
    }
}