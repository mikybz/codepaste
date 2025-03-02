package com.codepaste

import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.io.File

class UIComponents(
    private val stage: Stage,
    private val config: AppConfig,
    private val configManager: ConfigManager,
    private val onPasteAction: () -> Unit
) {
    private val logger = LoggerFactory.getLogger(UIComponents::class.java)

    val root = VBox().apply {
        spacing = 8.0  // Reduced spacing to help with height reduction
        padding = Insets(8.0)  // Reduced padding to help with height reduction
    }

    val projectDirField = TextField(configManager.loadProjectDirectory()).apply {
        isEditable = false
        prefWidth = config.fieldWidth.toDouble()
    }

    val selectDirButton = Button("Select Project Dir").apply {
        prefWidth = config.buttonWidth.toDouble()
        setOnAction {
            selectProjectDirectory()
            // Set focus back to paste button after directory selection
            pasteButton.requestFocus()
        }
    }

    val pasteButton = Button("Paste").apply {
        prefWidth = config.buttonWidth.toDouble()
        prefHeight = (config.pasteButtonHeight - 10).toDouble() // Reduced height to help with overall height reduction
        setOnAction {
            onPasteAction()
        }
    }

    val statusLabel = Label("Ready").apply {
        prefWidth = config.fieldWidth.toDouble()
    }

    init {
        root.children.addAll(
            Label("Project Directory:"),
            projectDirField,
            selectDirButton,
            pasteButton,
            statusLabel
        )
        
        // Request focus on the paste button when app starts
        pasteButton.requestFocus()
    }

    fun selectProjectDirectory() {
        logger.info("Opening directory chooser")
        val directoryChooser = DirectoryChooser().apply {
            title = "Select Project Directory"
            val currentDir = File(projectDirField.text)
            if (currentDir.exists() && currentDir.isDirectory) {
                initialDirectory = currentDir
            }
        }

        val selectedDirectory = directoryChooser.showDialog(stage)
        if (selectedDirectory != null) {
            val path = selectedDirectory.absolutePath
            logger.info("Selected project directory: $path")
            projectDirField.text = path
            configManager.saveProjectDirectory(path)
        }
    }

    fun updateStatus(message: String, isError: Boolean) {
        logger.info("Status update: $message")
        statusLabel.text = message
        if (isError) {
            statusLabel.style = "-fx-text-fill: red;"
        } else {
            statusLabel.style = "-fx-text-fill: green;"
        }
    }
}