package com.codepaste

import javafx.application.Platform
import javafx.scene.input.Clipboard
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ClipboardManager {
    private val logger = LoggerFactory.getLogger(ClipboardManager::class.java)

    fun getClipboardContent(): String {
        logger.info("Reading content from clipboard")
        
        val future = CompletableFuture<String>()
        
        Platform.runLater {
            try {
                val clipboard = Clipboard.getSystemClipboard()
                val content = if (clipboard.hasString()) {
                    clipboard.string ?: ""
                } else {
                    ""
                }
                future.complete(content)
            } catch (e: Exception) {
                logger.error("Error accessing clipboard", e)
                future.completeExceptionally(ClipboardException("Failed to access clipboard: ${e.message}", e))
            }
        }
        
        try {
            // Wait for a reasonable amount of time for the clipboard operation to complete
            return future.get(5, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            val cause = e.cause
            if (cause is ClipboardException) {
                throw cause
            }
            throw ClipboardException("Failed to access clipboard: ${e.message}", e)
        } catch (e: TimeoutException) {
            throw ClipboardException("Clipboard operation timed out", e)
        } catch (e: Exception) {
            throw ClipboardException("Error retrieving clipboard content: ${e.message}", e)
        }
    }
    
    fun setClipboardContent(content: String) {
        val future = CompletableFuture<Void>()
        
        Platform.runLater {
            try {
                val clipboard = Clipboard.getSystemClipboard()
                val clipboardContent = javafx.scene.input.ClipboardContent()
                clipboardContent.putString(content)
                clipboard.setContent(clipboardContent)
                future.complete(null)
            } catch (e: Exception) {
                logger.error("Error setting clipboard content", e)
                future.completeExceptionally(ClipboardException("Failed to set clipboard content: ${e.message}", e))
            }
        }
        
        try {
            future.get(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw ClipboardException("Error setting clipboard content: ${e.message}", e)
        }
    }
}