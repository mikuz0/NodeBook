package ru.nodebook.core.repo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KnowledgeBaseRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `create book creates correct directory structure and book json`() {
        val workspace = tempDir.resolve("workspace")
        Files.createDirectories(workspace)

        val bookDir = KnowledgeBaseRepository.create("test-book", workspace)

        // Check book directory exists
        assertTrue(Files.isDirectory(bookDir))
        assertEquals(workspace.resolve("test-book"), bookDir)

        // Check book.json exists and is valid JSON
        val bookFile = bookDir.resolve("book.json")
        assertTrue(Files.isRegularFile(bookFile))
        val content = Files.readString(bookFile)
        assertTrue(content.contains("\"name\"") && content.contains("test-book"))
        assertTrue(content.contains("\"sections\""))

        // Check sections exist with node.json
        val sections = listOf("components", "modules", "standards")
        sections.forEach { sectionName ->
            val sectionDir = bookDir.resolve(sectionName)
            assertTrue(Files.isDirectory(sectionDir), "Section directory $sectionName should exist")
            val nodeFile = sectionDir.resolve("node.json")
            assertTrue(Files.isRegularFile(nodeFile), "node.json should exist in $sectionName")
            val nodeContent = Files.readString(nodeFile)
            assertTrue(nodeContent.contains("\"type\"") && nodeContent.contains("SECTION"), "Section type should be SECTION")
            assertTrue(nodeContent.contains("\"slug\"") && nodeContent.contains(sectionName), "Section slug should be $sectionName")
        }
    }

    @Test
    fun `scan finds books by book json`() {
        val workspace = tempDir.resolve("workspace")
        Files.createDirectories(workspace)

        // Create two books
        KnowledgeBaseRepository.create("alpha-book", workspace)
        KnowledgeBaseRepository.create("beta-book", workspace)

        // Scan should find both
        val books = KnowledgeBaseRepository.scan(workspace)

        assertEquals(2, books.size)
        val names = books.map { it.name }
        assertTrue(names.contains("alpha-book"))
        assertTrue(names.contains("beta-book"))

        // Should be sorted by name
        assertEquals("alpha-book", books[0].name)
        assertEquals("beta-book", books[1].name)
    }

    @Test
    fun `scan ignores directories without book json`() {
        val workspace = tempDir.resolve("workspace")
        Files.createDirectories(workspace)

        // Create a random directory (not a book)
        Files.createDirectories(workspace.resolve("random-dir"))

        // Create a valid book
        KnowledgeBaseRepository.create("valid-book", workspace)

        val books = KnowledgeBaseRepository.scan(workspace)
        assertEquals(1, books.size)
        assertEquals("valid-book", books[0].name)
    }

    @Test
    fun `scan returns empty list for non-existent workspace`() {
        val books = KnowledgeBaseRepository.scan(tempDir.resolve("non-existent"))
        assertTrue(books.isEmpty())
    }

    @Test
    fun `load and save round-trip`() {
        val workspace = tempDir.resolve("workspace")
        Files.createDirectories(workspace)

        val bookDir = KnowledgeBaseRepository.create("round-trip-book", workspace)
        val loaded = KnowledgeBaseRepository.load(bookDir)

        assertEquals("round-trip-book", loaded.name)
        assertEquals(1, loaded.schemaVersion)
        assertEquals(1, loaded.version)
        assertEquals(listOf("components", "modules", "standards"), loaded.sections)
    }

    @Test
    fun `create book creates sample component structure`() {
        val workspace = tempDir.resolve("workspace")
        Files.createDirectories(workspace)

        val bookDir = KnowledgeBaseRepository.create("sample-book", workspace)
        val componentsDir = bookDir.resolve("components")

        // Check categories exist
        val categories = mapOf(
            "resistor" to "Resistor",
            "capacitor" to "Capacitor",
            "microcontroller" to "Microcontroller",
            "sensor" to "Sensor"
        )

        categories.forEach { (slug, name) ->
            val categoryDir = componentsDir.resolve(slug)
            assertTrue(Files.isDirectory(categoryDir), "Category directory $slug should exist")

            val nodeFile = categoryDir.resolve("node.json")
            assertTrue(Files.isRegularFile(nodeFile), "node.json should exist in $slug")
            val content = Files.readString(nodeFile)
            assertTrue(content.contains("\"type\"") && content.contains("\"CATEGORY\""), "Category type should be CATEGORY")
            assertTrue(content.contains("\"slug\"") && content.contains("\"$slug\""), "Category slug should be $slug")
            assertTrue(content.contains("\"name\"") && content.contains("\"$name\""), "Category name should be $name")

            // Check manufacturer exists
            val manufacturerDir = categoryDir.resolve("manufacturer")
            assertTrue(Files.isDirectory(manufacturerDir), "Manufacturer directory should exist in $slug")

            val manufNodeFile = manufacturerDir.resolve("node.json")
            assertTrue(Files.isRegularFile(manufNodeFile), "node.json should exist in manufacturer")
            val manufContent = Files.readString(manufNodeFile)
            assertTrue(manufContent.contains("\"type\"") && manufContent.contains("\"MANUFACTURER\""), "Manufacturer type should be MANUFACTURER")
        }

        // Check specific component files exist
        val microcontrollerManufacturer = componentsDir.resolve("microcontroller/manufacturer")
        assertTrue(Files.isRegularFile(microcontrollerManufacturer.resolve("atmega328p.node")), "atmega328p.node should exist")
        assertTrue(Files.isRegularFile(microcontrollerManufacturer.resolve("phy6222qc-w04.node")), "phy6222qc-w04.node should exist")
        assertTrue(Files.isRegularFile(microcontrollerManufacturer.resolve("stm32f103c8t6.node")), "stm32f103c8t6.node should exist")

        val capacitorManufacturer = componentsDir.resolve("capacitor/manufacturer")
        assertTrue(Files.isRegularFile(capacitorManufacturer.resolve("capacitor.node")), "capacitor.node should exist")

        val sensorManufacturer = componentsDir.resolve("sensor/manufacturer")
        assertTrue(Files.isRegularFile(sensorManufacturer.resolve("cht8310.node")), "cht8310.node should exist")
        assertTrue(Files.isRegularFile(sensorManufacturer.resolve("cht8320.node")), "cht8320.node should exist")

        // Load ATmega328P and verify structure
        val atmegaRef = ru.nodebook.core.repo.NodeRepository.load(microcontrollerManufacturer.resolve("atmega328p.node"))
        assertNotNull(atmegaRef, "ATmega328P node should be loadable")

        assertEquals("ATmega328P", atmegaRef.meta.name)
        assertEquals(ru.nodebook.core.model.NodeType.COMPONENT, atmegaRef.meta.type)
        assertTrue(atmegaRef.meta.slug.startsWith("atmega"), "Slug should start with 'atmega'")

        // Verify properties
        val properties = atmegaRef.meta.properties
        assertTrue(properties.containsKey("model3dPath"), "properties should contain model3dPath")
        assertTrue(properties.containsKey("ports"), "properties should contain ports")

        // Verify ports
        val portsJson = properties["ports"]
        assertNotNull(portsJson, "ports should not be null")
        assertTrue(portsJson is JsonArray, "ports should be a JsonArray")
        assertEquals(28, (portsJson as JsonArray).size.toInt(), "ATmega328P should have 28 ports")

        // Verify first port
        val firstPort = portsJson[0] as kotlinx.serialization.json.JsonObject
        val numberValue = (firstPort["number"] as kotlinx.serialization.json.JsonPrimitive).content
        val pinNumberValue = (firstPort["pinNumber"] as kotlinx.serialization.json.JsonPrimitive).content
        assertEquals(1, numberValue.toInt())
        assertEquals("1", pinNumberValue)
    }

    @Test
    fun `create book creates section view files`() {
        val workspace = tempDir.resolve("workspace")
        Files.createDirectories(workspace)

        val bookDir = KnowledgeBaseRepository.create("view-test-book", workspace)

        // Check view.json exists in each section
        val sections = listOf("components", "modules", "standards")
        val expectedTitles = mapOf(
            "components" to "Components",
            "modules" to "Modules",
            "standards" to "Standards"
        )

        sections.forEach { sectionName ->
            val sectionDir = bookDir.resolve(sectionName)
            val viewFile = sectionDir.resolve("view.json")
            assertTrue(Files.isRegularFile(viewFile), "view.json should exist in $sectionName")

            // Check hasSectionView
            assertTrue(KnowledgeBaseRepository.hasSectionView(sectionDir), "hasSectionView should return true for $sectionName")

            // Check loadSectionView
            val view = KnowledgeBaseRepository.loadSectionView(sectionDir)
            assertNotNull(view, "loadSectionView should not return null for $sectionName")
            assertEquals(1, view.schemaVersion)
            assertEquals("tree", view.kind)
            assertEquals(expectedTitles[sectionName], view.title)
            assertEquals(expectedTitles[sectionName], view.displayName(sectionName))
        }
    }
}
