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
        val contentLines = fileContent.lines()

        // Check if it's an empty file
        if (contentLines.isEmpty()) {
            logger.warn("Clipboard content is empty")
            return "Clipboard content is empty"
        }

        try {
            // Try to determine file type
            val fileType = fileTypeDetector.detectFileType(contentLines)
            logger.info("Processing file of type: $fileType")

            return when (fileType) {
                FileType.JAVA -> javaHandler.processFile(fileContent, projectDir, config)
                FileType.KOTLIN -> kotlinHandler.processFile(fileContent, projectDir, config)
                FileType.OTHER -> otherHandler.processFile(fileContent, projectDir, config)
                FileType.UNKNOWN -> throw CodePasteException("Unable to determine file type")
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