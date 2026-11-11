package ru.nodebook.core.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsStoreTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `save and load settings round-trip`() {
        // Override config dir for testing
        val testConfigDir = tempDir.resolve("config")
        Files.createDirectories(testConfigDir)
        AppPaths._configDir = testConfigDir

        try {
            val settings = Settings(workspace = "/home/user/workspace")
            SettingsStore.save(settings)

            val loaded = SettingsStore.load()
            assertEquals("/home/user/workspace", loaded.workspace)
        } finally {
            AppPaths._configDir = null
        }
    }

    @Test
    fun `load returns defaults when settings file does not exist`() {
        val testConfigDir = tempDir.resolve("config")
        Files.createDirectories(testConfigDir)
        AppPaths._configDir = testConfigDir

        try {
            val loaded = SettingsStore.load()
            assertEquals(null, loaded.workspace)
        } finally {
            AppPaths._configDir = null
        }
    }

    @Test
    fun `initConfigDir creates directory`() {
        // Reset to null so initConfigDir actually creates the directory
        AppPaths._configDir = null
        val testConfigDir = tempDir.resolve("new-config")

        try {
            AppPaths.initConfigDir()
            assertNotNull(AppPaths.configDir)
            assertTrue(Files.isDirectory(AppPaths.configDir))
        } finally {
            AppPaths._configDir = null
        }
    }

}
