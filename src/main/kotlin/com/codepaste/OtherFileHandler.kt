package com.codepaste

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class OtherFileHandler : FileHandlerBase() {
//    private val logger = LoggerFactory.getLogger(OtherFileHandler::class.java)
    
    override fun processFile(fileContent: String, projectDir: String, config: AppConfig): String {
        logger.info("Processing file of type OTHER")
        
        val contentLines = fileContent.lines()
        if (contentLines.isEmpty()) {
            throw CodePasteException("File content is empty")
        }
        
        // Look for potential matches in git repository
        if (isGitRepo(projectDir)) {
            val matchResult = searchForMatchingFiles(contentLines, projectDir)
            if (matchResult.isSuccess) {
                return matchResult.getOrNull() ?: "File processing canceled"
            }
        } else {
            logger.info("Not a git repository: $projectDir")
        }
        
        // If we reach here, either it's not a git repo or no matches were found/selected
        // Show file placement dialog
        val result = showFilePlacementDialog(fileContent, projectDir)
        return result ?: throw CodePasteException("File processing canceled")
    }
    
    private fun isGitRepo(projectDir: String): Boolean {
        val gitDir = File(projectDir, ".git")
        return gitDir.exists() && gitDir.isDirectory
    }
    
    private fun searchForMatchingFiles(contentLines: List<String>, projectDir: String): Result<String?> {
        // Select lines that are long enough for good matching
        val candidateLines = contentLines.filter { it.trim().length > 40 }
        if (candidateLines.isEmpty()) {
            logger.info("No candidate lines found for matching")
            return Result.success(null)
        }
        
        // Limit the number of lines to search
        val linesToSearch = candidateLines.take(40)
        
        for (line in linesToSearch) {
            val cleanLine = line.trim().replace("\"", "\\\"").replace("'", "\\'")
            if (cleanLine.isBlank()) continue
            
            logger.info("Searching for matching files with line: ${cleanLine.take(40)}...")
            
            val matchingFiles = try {
                runGitGrep(cleanLine, projectDir)
            } catch (e: Exception) {
                logger.error("Error running git grep: ${e.message}")
                continue
            }
            
            if (matchingFiles.size == 1) {
                val matchedFile = File(projectDir, matchingFiles.first())
                logger.info("Found a matching file: ${matchedFile.absolutePath}")
                
                val shouldReplace = showReplaceDialog(matchedFile.absolutePath)
                when (shouldReplace) {
                    ReplaceOption.YES -> {
                        writeFile(matchedFile, contentLines.joinToString("\n"))
                        return Result.success("Updated existing file: ${matchedFile.absolutePath}")
                    }
                    ReplaceOption.NO -> continue // Try next line
                    ReplaceOption.CANCEL -> return Result.success(null) // User canceled
                }
            } else if (matchingFiles.size > 1) {
                logger.info("Found multiple matching files, continuing search")
                continue
            }
        }
        
        logger.info("No suitable matching file found")
        return Result.success(null)
    }
    
    private fun runGitGrep(searchText: String, projectDir: String): List<String> {
        val command = arrayOf("git", "grep", "-l", searchText)
        val process = ProcessBuilder(*command)
            .directory(File(projectDir))
            .redirectErrorStream(true)
            .start()
        
        val matchingFiles = mutableListOf<String>()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { matchingFiles.add(it) }
            }
        }
        
        process.waitFor()
        return matchingFiles
    }
    
    private fun showReplaceDialog(filePath: String): ReplaceOption {
        val result = CompletableFuture<ReplaceOption>()
        
        Platform.runLater {
            val dialog = Dialog<ReplaceOption>()
            dialog.title = "Matching File Found"
            dialog.headerText = "Found a matching file. Do you want to replace it?"
            dialog.contentText = "File: $filePath"
            
            dialog.dialogPane.buttonTypes.addAll(
                ButtonType.YES,
                ButtonType.NO,
                ButtonType.CANCEL
            )
            
            dialog.setResultConverter { buttonType ->
                when (buttonType) {
                    ButtonType.YES -> ReplaceOption.YES
                    ButtonType.NO -> ReplaceOption.NO
                    else -> ReplaceOption.CANCEL
                }
            }
            
            val dialogResult = dialog.showAndWait()
            result.complete(dialogResult.orElse(ReplaceOption.CANCEL))
        }
        
        return result.get()
    }
    
    private fun showFilePlacementDialog(fileContent: String, projectDir: String): String? {
        val result = CompletableFuture<String?>()
        val cancelled = AtomicBoolean(false)
        
        Platform.runLater {
            val stage = Stage()
            stage.title = "Select File Location"
            stage.initModality(Modality.APPLICATION_MODAL)
            
            val pathField = TextField()
            pathField.promptText = "Enter file path (relative to project)"
            
            val createButton = Button("Create")
            createButton.isDisable = true
            
            // Enable create button only when pathField has content
            pathField.textProperty().addListener { _, _, newValue ->
                createButton.isDisable = newValue.isNullOrBlank()
            }
            
            val filePickerButton = Button("Select Existing File")
            val cancelButton = Button("Cancel")
            
            // Set up file picker
            filePickerButton.setOnAction {
                val fileChooser = FileChooser()
                fileChooser.title = "Select File to Replace"
                fileChooser.initialDirectory = File(projectDir)
                
                val selectedFile = fileChooser.showOpenDialog(stage)
                if (selectedFile != null) {
                    // Calculate relative path
                    val projectDirFile = File(projectDir)
                    val relativePath = projectDirFile.toPath().relativize(selectedFile.toPath()).toString()
                    pathField.text = relativePath
                }
            }
            
            // Set up create button action
            createButton.setOnAction {
                val targetPath = pathField.text
                if (targetPath.isNotBlank()) {
                    val targetFile = File(projectDir, targetPath)
                    try {
                        if (!targetFile.parentFile.exists()) {
                            targetFile.parentFile.mkdirs()
                        }
                        writeFile(targetFile, fileContent)
                        result.complete("Saved file to: ${targetFile.absolutePath}")
                        stage.close()
                    } catch (e: Exception) {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Error"
                        alert.headerText = "Failed to save file"
                        alert.contentText = e.message
                        alert.showAndWait()
                    }
                }
            }
            
            // Set up cancel button
            cancelButton.setOnAction {
                cancelled.set(true)
                result.complete(null)
                stage.close()
            }
            
            // Layout components
            val pathBox = HBox(10.0)
            pathBox.children.addAll(pathField, createButton)
            HBox.setHgrow(pathField, Priority.ALWAYS)
            
            val buttonsBox = HBox(10.0)
            buttonsBox.children.addAll(filePickerButton, cancelButton)
            
            val root = VBox(10.0)
            root.padding = Insets(20.0)
            root.children.addAll(
                Label("No matching file found. Select where to save the file:"),
                pathBox,
                buttonsBox
            )
            
            val scene = Scene(root)
            stage.scene = scene
            stage.sizeToScene()
            stage.showAndWait()
            
            // If dialog was closed without create or cancel, treat as cancel
            if (!result.isDone) {
                result.complete(null)
            }
        }
        
        return result.get()
    }
    
    enum class ReplaceOption {
        YES, NO, CANCEL
    }
}