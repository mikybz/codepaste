package com.codepaste

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

data class AppConfig(
    val windowWidth: Int = 180,
    val windowHeight: Int = 300,
    val buttonWidth: Int = 160,
    val fieldWidth: Int = 160,
    val pasteButtonHeight: Int = 80,
    val javaSourceRoots: List<String> = listOf(
        "src/main/java",
        "src/test/java",
        "src/java",
        "java"
    ),
    val kotlinSourceRoots: List<String> = listOf(
        "src/main/kotlin",
        "src/test/kotlin",
        "src/kotlin",
        "kotlin"
    )
)

class ConfigManager {
    private val logger = LoggerFactory.getLogger(ConfigManager::class.java)
    private val userHome = System.getProperty("user.home")
    private val configDir = File(userHome, ".codepaste")
    private val configFile = File(configDir, "config.yaml")
    private val projectDirFile = File(configDir, "project_dir.txt")
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    init {
        try {
            if (!configDir.exists()) {
                logger.info("Creating config directory: ${configDir.absolutePath}")
                val created = configDir.mkdirs()
                if (!created) {
                    logger.error("Failed to create config directory: ${configDir.absolutePath}")
                }
            }

            if (!configFile.exists()) {
                logger.info("Creating default config file")
                saveConfig(AppConfig())
            }
        } catch (e: Exception) {
            logger.error("Error initializing configuration", e)
        }
    }

    fun loadConfig(): AppConfig {
        try {
            logger.info("Loading config from ${configFile.absolutePath}")
            return mapper.readValue(configFile)
        } catch (e: Exception) {
            logger.error("Failed to load config, using defaults", e)
            return AppConfig()
        }
    }

    fun saveConfig(config: AppConfig) {
        try {
            logger.info("Saving config to ${configFile.absolutePath}")
            mapper.writeValue(configFile, config)
        } catch (e: Exception) {
            logger.error("Failed to save config", e)
            throw ConfigurationException("Could not save configuration", e)
        }
    }

    fun loadProjectDirectory(): String {
        return try {
            if (projectDirFile.exists()) {
                logger.info("Loading project directory from ${projectDirFile.absolutePath}")
                Files.readString(projectDirFile.toPath()).trim()
            } else {
                logger.info("No saved project directory, using current directory")
                System.getProperty("user.dir")
            }
        } catch (e: Exception) {
            logger.error("Failed to load project directory, using current directory", e)
            System.getProperty("user.dir")
        }
    }

    fun saveProjectDirectory(directory: String) {
        try {
            logger.info("Saving project directory to ${projectDirFile.absolutePath}")
            Files.writeString(projectDirFile.toPath(), directory)
        } catch (e: IOException) {
            logger.error("Failed to save project directory", e)
            throw ConfigurationException("Could not save project directory", e)
        }
    }
}