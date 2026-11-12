package ru.nodebook.core.config

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

object AppPaths {

    private val log = LoggerFactory.getLogger(AppPaths::class.java)
    const val APP_NAME = "nodebook"

    @JvmStatic
    internal var _configDir: Path? = null

    val configDir: Path
        get() = _configDir ?: throw IllegalStateException("Config directory not initialized. Call initConfigDir() first.")

    /**
     * Initialize the config directory using XDG_CONFIG_HOME or ~/.config.
     */
    fun initConfigDir() {
        if (_configDir != null) return

        val xdgConfig = System.getenv("XDG_CONFIG_HOME")
        _configDir = if (!xdgConfig.isNullOrBlank()) {
            Paths.get(xdgConfig, APP_NAME)
        } else {
            Paths.get(System.getProperty("user.home"), ".config", APP_NAME)
        }

        try {
            Files.createDirectories(_configDir!!)
            log.info("Config directory initialized: $configDir")
        } catch (e: Exception) {
            log.error("Failed to create config directory: $configDir", e)
            throw IllegalStateException("Cannot create config directory: $configDir", e)
        }
    }
}
