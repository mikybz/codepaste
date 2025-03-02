package com.codepaste

import javafx.scene.input.Clipboard
import org.slf4j.LoggerFactory

class ClipboardManager {
    private val logger = LoggerFactory.getLogger(ClipboardManager::class.java)

    fun getClipboardContent(): String {
        logger.info("Reading content from clipboard")

        try {
            val clipboard = Clipboard.getSystemClipboard()

            return if (clipboard.hasString()) {
                val content = clipboard.string
                if (content.isBlank()) {
                    logger.warn("Clipboard contains only whitespace")
                    throw ClipboardException("Clipboard contains only whitespace")
                }

                logger.debug("Found content in clipboard (${content.length} characters)")
                content
            } else {
                logger.warn("No text content found in clipboard")
                throw ClipboardException("No text content found in clipboard")
            }
        } catch (e: Exception) {
            if (e is ClipboardException) throw e
            logger.error("Error accessing clipboard", e)
            throw ClipboardException("Failed to access clipboard: ${e.message}", e)
        }
    }
}