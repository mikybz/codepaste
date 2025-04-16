package com.codepaste

import org.slf4j.LoggerFactory
import java.io.File

class MultiFileHandler : FileHandlerBase() {
    override fun processFile(fileContent: String, projectDir: String, config: AppConfig): String {
        logger.info("Processing multi-file input")

        // Parse the content to extract individual files
        val fileEntries = parseMultiFileContent(fileContent)

        if (fileEntries.isEmpty()) {
            return "No valid file entries found in input"
        }

        val results = mutableListOf<String>()
        var errorOccurred = false

        // Process each file entry
        for (entry in fileEntries) {
            try {
                when (entry) {
                    is FileEntry.CreateFile -> {
                        val targetFile = File(projectDir, entry.path)
                        logger.info("Creating/updating file: ${targetFile.absolutePath}")

                        // Ensure parent directories exist
                        targetFile.parentFile?.mkdirs()

                        // Write file content
                        writeFile(targetFile, entry.content)
                        results.add("Saved file: ${targetFile.absolutePath}")
                    }

                    is FileEntry.RemoveFile -> {
                        val targetFile = File(projectDir, entry.path)
                        logger.info("Removing file: ${targetFile.absolutePath}")

                        if (targetFile.exists()) {
                            val deleted = targetFile.delete()
                            if (deleted) {
                                results.add("Deleted file: ${targetFile.absolutePath}")
                            } else {
                                results.add("Failed to delete file: ${targetFile.absolutePath}")
                                errorOccurred = true
                            }
                        } else {
                            results.add("File does not exist: ${targetFile.absolutePath}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error processing file entry: $entry", e)
                results.add("Error: ${e.message}")
                errorOccurred = true
            }
        }

        // Generate summary message
        val summary = if (errorOccurred) {
            "Processed ${fileEntries.size} files with errors"
        } else {
            "Successfully processed ${fileEntries.size} files"
        }

        return "$summary\n${results.joinToString("\n")}"
    }

    private fun parseMultiFileContent(content: String): List<FileEntry> {
        val lines = content.lines()
        val result = mutableListOf<FileEntry>()

        var currentFilePath: String? = null
        val currentFileContent = StringBuilder()

        for (line in lines) {
            // Check if this line is a file marker
            val fileMatch = FILE_START_PATTERN.find(line)
            val removeMatch = FILE_REMOVE_PATTERN.find(line)

            when {
                fileMatch != null -> {
                    // If we were collecting content for a previous file, add it to results
                    if (currentFilePath != null && currentFileContent.isNotEmpty()) {
                        result.add(FileEntry.CreateFile(currentFilePath!!, currentFileContent.toString().trimEnd()))
                    }

                    // Start new file
                    currentFilePath = fileMatch.groupValues[1].trim()
                    currentFileContent.clear()
                }

                removeMatch != null -> {
                    // If we were collecting content for a previous file, add it to results
                    if (currentFilePath != null && currentFileContent.isNotEmpty()) {
                        result.add(FileEntry.CreateFile(currentFilePath!!, currentFileContent.toString().trimEnd()))
                    }

                    // Add a remove file entry
                    val pathToRemove = removeMatch.groupValues[1].trim()
                    result.add(FileEntry.RemoveFile(pathToRemove))

                    // Reset current file tracking
                    currentFilePath = null
                    currentFileContent.clear()
                }

                currentFilePath != null -> {
                    // If we have an active file, add this line to its content
                    if (currentFileContent.isNotEmpty()) {
                        currentFileContent.append("\n")
                    }
                    currentFileContent.append(line)
                }
            }
        }

        // Don't forget to add the last file if there is one
        if (currentFilePath != null && currentFileContent.isNotEmpty()) {
            result.add(FileEntry.CreateFile(currentFilePath!!, currentFileContent.toString().trimEnd()))
        }

        return result
    }

    sealed class FileEntry {
        data class CreateFile(val path: String, val content: String) : FileEntry()
        data class RemoveFile(val path: String) : FileEntry()
    }

    companion object {
        // Regex patterns for detecting file markers
        val FILE_START_PATTERN = Regex("""^\s*//\s*File:\s*(.+)$""")
        val FILE_REMOVE_PATTERN = Regex("""^\s*//\s*FileToRemove:\s*(.+)$""")

        /**
         * Check if the content contains file markers for multi-file format
         */
        fun isMultiFileInput(content: String): Boolean {
            // Check if the content contains file markers
            val fileMarkerPattern = Regex("""^\s*//\s*File:\s*.+$""", RegexOption.MULTILINE)
            val removeMarkerPattern = Regex("""^\s*//\s*FileToRemove:\s*.+$""", RegexOption.MULTILINE)

            return fileMarkerPattern.find(content) != null || removeMarkerPattern.find(content) != null
        }
    }
}