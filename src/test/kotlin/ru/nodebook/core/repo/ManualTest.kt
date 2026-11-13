package ru.nodebook.core.repo

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

class ManualTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `create and save path`() {
        val workspace = tempDir.resolve("workspace")
        Files.createDirectories(workspace)
        val bookDir = KnowledgeBaseRepository.create("test-book", workspace)
        // Save path to file for inspection
        Files.writeString(tempDir.resolve("book_path.txt"), bookDir.toString())
        println("Created: $bookDir")
        
        // Verify structure
        val componentsDir = bookDir.resolve("components")
        assertTrue(Files.isDirectory(componentsDir))
        assertTrue(Files.isDirectory(componentsDir.resolve("resistor/manufacturer")))
        assertTrue(Files.isDirectory(componentsDir.resolve("capacitor/manufacturer")))
        assertTrue(Files.isDirectory(componentsDir.resolve("microcontroller/manufacturer")))
        assertTrue(Files.isDirectory(componentsDir.resolve("sensor/manufacturer")))
        
        // Verify component files
        assertTrue(Files.isRegularFile(componentsDir.resolve("resistor/manufacturer/resistor.node")))
        assertTrue(Files.isRegularFile(componentsDir.resolve("capacitor/manufacturer/capacitor.node")))
        assertTrue(Files.isRegularFile(componentsDir.resolve("microcontroller/manufacturer/atmega328p.node")))
        assertTrue(Files.isRegularFile(componentsDir.resolve("microcontroller/manufacturer/stm32f103c8t6.node")))
        assertTrue(Files.isRegularFile(componentsDir.resolve("microcontroller/manufacturer/phy6222qc-w04.node")))
        assertTrue(Files.isRegularFile(componentsDir.resolve("sensor/manufacturer/cht8310.node")))
        assertTrue(Files.isRegularFile(componentsDir.resolve("sensor/manufacturer/cht8320.node")))
        
        println("All checks passed!")
    }
}
