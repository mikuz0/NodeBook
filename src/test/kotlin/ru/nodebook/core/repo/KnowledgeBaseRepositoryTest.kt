package ru.nodebook.core.repo

import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.nodebook.core.model.*
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
        val atmegaRef = NodeRepository.load(microcontrollerManufacturer.resolve("atmega328p.node"))
        assertNotNull(atmegaRef, "ATmega328P node should be loadable")

        // Access content through NodeContent.Leaf
        val component = when (atmegaRef.content) {
            is NodeContent.Leaf -> atmegaRef.content.component
            else -> throw AssertionError("Expected Leaf node")
        }

        assertEquals("ATmega328P", component.name)
        assertEquals(NodeType.COMPONENT, atmegaRef.type)
        assertTrue(atmegaRef.slug.startsWith("atmega"), "Slug should start with 'atmega'")

        // Verify ports (now in ComponentNode.ports instead of properties)
        val ports = component.ports
        assertNotNull(ports)
        assertTrue(ports.isNotEmpty(), "ATmega328P should have ports")

        // Verify first port
        val firstPort = ports[0]
        assertEquals(1, firstPort.number)
        assertEquals("1", firstPort.pinNumber)
        assertEquals("PC6/RESET", firstPort.name)
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

    @Test
    fun `create book creates type template placeholders`() {
        val workspace = tempDir.resolve("workspace")
        Files.createDirectories(workspace)

        val bookDir = KnowledgeBaseRepository.create("template-test-book", workspace)
        val componentsDir = bookDir.resolve("components")

        // Check type directories have fields.json and view.json
        val typeSlugs = listOf("resistor", "capacitor", "microcontroller", "sensor")

        typeSlugs.forEach { slug ->
            val typeDir = componentsDir.resolve(slug)
            assertTrue(Files.isDirectory(typeDir), "Type directory $slug should exist")

            val fieldsFile = typeDir.resolve("fields.json")
            assertTrue(Files.isRegularFile(fieldsFile), "fields.json should exist in $slug")

            val typeViewFile = typeDir.resolve("view.json")
            assertTrue(Files.isRegularFile(typeViewFile), "view.json should exist in $slug")

            // Check hasTypeFields/hasTypeView
            assertTrue(KnowledgeBaseRepository.hasTypeFields(typeDir), "hasTypeFields should return true for $slug")
            assertTrue(KnowledgeBaseRepository.hasTypeView(typeDir), "hasTypeView should return true for $slug")

            // Check fields.json content
            val fieldsContent = Files.readString(fieldsFile)
            assertTrue(fieldsContent.contains("\"schemaVersion\""), "fields.json should contain schemaVersion")
            assertTrue(fieldsContent.contains("\"fields\""), "fields.json should contain fields")
            assertTrue(fieldsContent.contains("[]"), "fields.json should have empty fields array")

            // Check view.json content
            val typeViewContent = Files.readString(typeViewFile)
            assertTrue(typeViewContent.contains("\"schemaVersion\""), "view.json should contain schemaVersion")
            assertTrue(typeViewContent.contains("\"kind\""), "view.json should contain kind")
            assertTrue(typeViewContent.contains("\"tree\""), "view.json should have kind=tree")
        }
    }
}
