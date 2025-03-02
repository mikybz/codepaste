package com.codepaste

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.io.File

class CodePasteApp : Application() {
    private val logger = LoggerFactory.getLogger(CodePasteApp::class.java)
    private val configManager = ConfigManager()
    private val fileHandler = FileHandler()
    private val clipboardManager = ClipboardManager()
    private lateinit var ui: UIComponents

    override fun start(stage: Stage) {
        logger.info("Starting CodePaste application")

        try {
            val config = configManager.loadConfig()

            // Initialize UI components
            ui = UIComponents(stage, config, configManager) { handlePaste() }

            // Set up the scene
            val scene = Scene(ui.root, config.windowWidth.toDouble(), config.windowHeight.toDouble())

            // Add key event handler
            scene.setOnKeyPressed { event ->
                if (event.code == KeyCode.ENTER || event.code == KeyCode.SPACE) {
                    handlePaste()
                }
            }

            stage.title = "CodePaste"
            stage.scene = scene
            stage.show()

            // Ensure paste button has focus
            Platform.runLater {
                ui.pasteButton.requestFocus()
            }

            logger.info("CodePaste UI initialized")
        } catch (e: Exception) {
            logger.error("Error initializing application", e)
            Platform.exit()
        }
    }

    private fun handlePaste() {
        logger.info("Paste button pressed")
        ui.updateStatus("Processing...", false)

        val projectDir = ui.projectDirField.text
        if (projectDir.isBlank() || !File(projectDir).exists()) {
            ui.updateStatus("No valid project directory selected", true)
            return
        }

        Thread {
            try {
                val clipboardContent = clipboardManager.getClipboardContent()
                if (clipboardContent.isBlank()) {
                    Platform.runLater { ui.updateStatus("No code in clipboard", true) }
                    return@Thread
                }

                val result = fileHandler.processAndSaveFile(clipboardContent, projectDir)
                Platform.runLater { ui.updateStatus(result, !result.startsWith("Error")) }
            } catch (e: Exception) {
                logger.error("Error processing file", e)
                Platform.runLater { ui.updateStatus("Error: ${e.message}", true) }
            }
        }.start()
    }
}

fun main() {
    try {
        Application.launch(CodePasteApp::class.java)
    } catch (e: Exception) {
        val logger = LoggerFactory.getLogger("Main")
        logger.error("Fatal application error", e)
    }
}