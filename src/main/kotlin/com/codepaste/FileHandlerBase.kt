package com.codepaste

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

abstract class FileHandlerBase {
    protected val logger = LoggerFactory.getLogger(this::class.java)
    protected val fileTypeDetector = FileTypeDetector()

    abstract fun processFile(fileContent: String, projectDir: String, config: AppConfig): String

    protected fun findSourceRoot(projectDir: File, possibleRoots: List<String>): File? {
        logger.info("Looking for source root in: ${projectDir.absolutePath}")

        for (rootPath in possibleRoots) {
            val rootDir = File(projectDir, rootPath)
            if (rootDir.exists() && rootDir.isDirectory) {
                logger.info("Found source root: ${rootDir.absolutePath}")
                return rootDir
            }
        }

        // If no predefined roots are found, fallback to finding any directory with existing source files
        try {
            val javaKotlinFiles = projectDir.walk()
                .filter { it.isFile && (it.name.endsWith(".java") || it.name.endsWith(".kt")) }
                .toList()

            if (javaKotlinFiles.isNotEmpty()) {
                val firstFile = javaKotlinFiles.first()
                val fileContent = Files.readString(firstFile.toPath())
                val packageName = fileTypeDetector.extractPackageName(fileContent.lines())

                if (packageName != null) {
                    val packagePath = packageName.replace('.', File.separatorChar)
                    val path = firstFile.absolutePath
                    val pathWithoutPackage = path.removeSuffix(File.separator + firstFile.name)
                        .removeSuffix(File.separator + packagePath)

                    logger.info("Inferred source root from existing file: $pathWithoutPackage")
                    return File(pathWithoutPackage)
                }
            }
        } catch (e: Exception) {
            logger.error("Error while searching for source files", e)
        }

        // If still no root is found, use the project directory itself
        logger.warn("No source root found, using project directory itself")
        return projectDir
    }

    protected fun findExistingFile(directory: File, fileName: String): File? {
        logger.info("Searching for existing file '$fileName' in ${directory.absolutePath}")

        try {
            return directory.walk()
                .filter { it.isFile && it.name == fileName }
                .firstOrNull()
                ?.also { logger.info("Found existing file: ${it.absolutePath}") }
        } catch (e: Exception) {
            logger.error("Error searching for file: $fileName", e)
            return null
        }
    }

    protected fun writeFile(targetFile: File, content: String): Boolean {
        try {
            logger.info("Writing file to: ${targetFile.absolutePath}")
            targetFile.parentFile.mkdirs() // Ensure the directory exists

            Files.writeString(
                targetFile.toPath(),
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            return true
        } catch (e: Exception) {
            logger.error("Failed to write file: ${targetFile.absolutePath}", e)
            throw CodePasteException("Failed to write file: ${e.message}")
        }
    }
}