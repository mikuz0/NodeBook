package ru.nodebook.core.config

import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.LoggerFactory

object SettingsStore {

    private val log = LoggerFactory.getLogger(SettingsStore::class.java)
    private const val SETTINGS_FILE = "settings.json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Load settings from the config directory.
     * Returns default settings if file doesn't exist or is invalid.
     */
    fun load(): Settings {
        return try {
            val settingsFile = AppPaths.configDir.resolve(SETTINGS_FILE)
            if (!Files.isRegularFile(settingsFile)) {
                log.info("Settings file not found, using defaults: $settingsFile")
                Settings()
            } else {
                val content = Files.readString(settingsFile)
                json.decodeFromString(Settings.serializer(), content)
            }
        } catch (e: Exception) {
            log.error("Failed to load settings", e)
            Settings()
        }
    }

    /**
     * Save settings to the config directory.
     */
    fun save(settings: Settings) {
        try {
            AppPaths.initConfigDir()
            val settingsFile = AppPaths.configDir.resolve(SETTINGS_FILE)
            val content = json.encodeToString(Settings.serializer(), settings)
            Files.write(settingsFile, content.toByteArray(Charsets.UTF_8))
            log.info("Saved settings to $settingsFile")
        } catch (e: Exception) {
            log.error("Failed to save settings", e)
            throw IllegalArgumentException("Failed to save settings: ${e.message}", e)
        }
    }
}
