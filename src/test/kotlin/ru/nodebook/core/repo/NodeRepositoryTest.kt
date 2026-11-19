package ru.nodebook.core.repo

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.nodebook.core.model.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NodeRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `createContainer and load returns correct node`() {
        val parentDir = tempDir.resolve("parent")
        Files.createDirectories(parentDir)

        val meta = NodeMeta(name = "Test Container", type = NodeType.SECTION)
        val slug = NodeRepository.generateUniqueSlug(parentDir, "Test Container")
        val containerPath = NodeRepository.createContainer(parentDir, slug, meta)

        // Verify directory exists
        assertTrue(Files.isDirectory(containerPath))

        // Load should return the node
        val loaded = NodeRepository.load(containerPath)
        assertNotNull(loaded)
        
        // Access content through NodeContent.Container
        val containerMeta = when (loaded.content) {
            is NodeContent.Container -> loaded.content.meta
            else -> throw AssertionError("Expected Container node")
        }
        
        assertEquals(NodeType.SECTION, containerMeta.type)
        assertEquals(slug, containerMeta.slug)
        assertEquals("Test Container", containerMeta.name)
        assertTrue(loaded.isContainer)
    }

    @Test
    fun `createLeaf and load returns correct node`() {
        val parentDir = tempDir.resolve("parent")
        Files.createDirectories(parentDir)

        // Create a ComponentNode instead of NodeMeta
        val component = ComponentNode(
            name = "Test Component",
            description = "Test description",
            ports = listOf(
                ComponentPort(1, "1", "Pin1", "First pin"),
                ComponentPort(2, "2", "Pin2", "Second pin")
            )
        )
        val slug = NodeRepository.generateUniqueSlug(parentDir, "Test Component")
        val leafPath = NodeRepository.createLeaf(parentDir, slug, component)

        // Verify file exists
        assertTrue(Files.isRegularFile(leafPath))
        assertTrue(leafPath.fileName.toString().endsWith(".node"))

        // Load should return the node
        val loaded = NodeRepository.load(leafPath)
        assertNotNull(loaded)
        
        // Access content through NodeContent.Leaf
        val loadedComponent = when (loaded.content) {
            is NodeContent.Leaf -> loaded.content.component
            else -> throw AssertionError("Expected Leaf node")
        }
        
        assertEquals(NodeType.COMPONENT, loaded.type)
        assertEquals(slug, loaded.slug)
        assertEquals("Test Component", loadedComponent.name)
        assertEquals(2, loadedComponent.ports.size)
        assertTrue(loaded.isContainer.not())
    }

    @Test
    fun `loadTree returns all nodes recursively`() {
        val sectionDir = tempDir.resolve("components")
        Files.createDirectories(sectionDir)

        // Create root section node
        val rootMeta = NodeMeta(name = "Components", type = NodeType.SECTION, slug = "components")
        NodeRepository.save(sectionDir, rootMeta)

        // Create a manufacturer container
        val manufSlug = NodeRepository.generateUniqueSlug(sectionDir, "Manufacturer 1")
        val manufPath = NodeRepository.createContainer(sectionDir, manufSlug, NodeMeta(name = "Manufacturer 1", type = NodeType.MANUFACTURER))

        // Create a category container inside manufacturer
        val catSlug = NodeRepository.generateUniqueSlug(manufPath, "Category A")
        val catPath = NodeRepository.createContainer(manufPath, catSlug, NodeMeta(name = "Category A", type = NodeType.CATEGORY))

        // Create a leaf component inside category using ComponentNode
        val compSlug = NodeRepository.generateUniqueSlug(catPath, "STM32F103")
        val compNode = ComponentNode(name = "STM32F103", description = "Test component")
        NodeRepository.createLeaf(catPath, compSlug, compNode)

        // Load tree
        val tree = NodeRepository.loadTree(sectionDir)

        // Should have: root (components), manufacturer, category, component
        assertEquals(4, tree.size)
        
        // Access types through NodeRef.type property
        assertEquals(NodeType.SECTION, tree[0].type)
        assertEquals(NodeType.MANUFACTURER, tree[1].type)
        assertEquals(NodeType.CATEGORY, tree[2].type)
        assertEquals(NodeType.COMPONENT, tree[3].type)
    }

    @Test
    fun `generateUniqueSlug appends -1 on collision`() {
        val parentDir = tempDir.resolve("parent")
        Files.createDirectories(parentDir)

        val slug1 = NodeRepository.generateUniqueSlug(parentDir, "Test")
        // Create a container with that slug
        NodeRepository.createContainer(parentDir, slug1, NodeMeta(name = "Test", type = NodeType.SECTION))

        // Next call should return slug1-1
        val slug2 = NodeRepository.generateUniqueSlug(parentDir, "Test")
        assertTrue(slug2.startsWith(slug1) && slug2.endsWith("-1"))
    }

    @Test
    fun `generateSlug converts name correctly`() {
        assertEquals("hello-world", NodeRepository.generateSlug("Hello World"))
        assertEquals("test-name", NodeRepository.generateSlug("Test_Name"))
        assertEquals("my-component", NodeRepository.generateSlug("My Component!"))
        assertEquals("untitled", NodeRepository.generateSlug("!!!"))
    }

    @Test
    fun `delete removes directory recursively`() {
        val parentDir = tempDir.resolve("parent")
        Files.createDirectories(parentDir)

        val slug = NodeRepository.generateUniqueSlug(parentDir, "To Delete")
        val containerPath = NodeRepository.createContainer(parentDir, slug, NodeMeta(name = "To Delete", type = NodeType.SECTION))

        // Create a nested structure
        val nestedSlug = NodeRepository.generateUniqueSlug(containerPath, "Nested")
        NodeRepository.createContainer(containerPath, nestedSlug, NodeMeta(name = "Nested", type = NodeType.CATEGORY))

        assertTrue(Files.isDirectory(containerPath))
        NodeRepository.delete(containerPath)
        assertTrue(Files.notExists(containerPath))
    }

    @Test
    fun `listChildren returns direct children only`() {
        val parentDir = tempDir.resolve("parent")
        Files.createDirectories(parentDir)

        // Create root section node
        val rootMeta = NodeMeta(name = "Parent", type = NodeType.SECTION, slug = "parent")
        NodeRepository.save(parentDir, rootMeta)

        // Create two direct children
        val child1Slug = NodeRepository.generateUniqueSlug(parentDir, "Child 1")
        NodeRepository.createContainer(parentDir, child1Slug, NodeMeta(name = "Child 1", type = NodeType.CATEGORY))

        val child2Slug = NodeRepository.generateUniqueSlug(parentDir, "Child 2")
        val child2Component = ComponentNode(name = "Child 2", description = "Test")
        NodeRepository.createLeaf(parentDir, child2Slug, child2Component)

        val children = NodeRepository.listChildren(parentDir)

        // Should have 2 children (not counting the root node.json itself)
        assertEquals(2, children.size)
    }

    @Test
    fun `createLeaf with ports creates correct ComponentNode`() {
        val parentDir = tempDir.resolve("parent")
        Files.createDirectories(parentDir)

        // Create component with detailed ports
        val ports = listOf(
            ComponentPort(1, "1", "VCC", "Power supply"),
            ComponentPort(2, "2", "GND", "Ground"),
            ComponentPort(3, "3", "SDA", "I2C data"),
            ComponentPort(4, "4", "SCL", "I2C clock")
        )
        val component = ComponentNode(
            name = "Sensor CHT8310",
            description = "Digital temperature and humidity sensor",
            refPrefix = "U",
            ports = ports,
            mainProperties = listOf(
                PropertyValue("package", "DFN-6", "mm"),
                PropertyValue("voltage", "3.3", "V")
            )
        )

        val slug = NodeRepository.generateUniqueSlug(parentDir, "sensor-cht8310")
        val leafPath = NodeRepository.createLeaf(parentDir, slug, component)

        // Verify file exists
        assertTrue(Files.isRegularFile(leafPath))

        // Load and verify
        val loaded = NodeRepository.load(leafPath)
        assertNotNull(loaded)

        val loadedComponent = when (loaded.content) {
            is NodeContent.Leaf -> loaded.content.component
            else -> throw AssertionError("Expected Leaf node")
        }

        assertEquals("Sensor CHT8310", loadedComponent.name)
        assertEquals("Digital temperature and humidity sensor", loadedComponent.description)
        assertEquals("U", loadedComponent.refPrefix)
        assertEquals(4, loadedComponent.ports.size)
        assertEquals(2, loadedComponent.mainProperties.size)

        // Verify ports
        assertEquals(1, loadedComponent.ports[0].number)
        assertEquals("VCC", loadedComponent.ports[0].name)
        assertEquals("Power supply", loadedComponent.ports[0].description)

        assertEquals(2, loadedComponent.ports[1].number)
        assertEquals("GND", loadedComponent.ports[1].name)
        assertEquals("Ground", loadedComponent.ports[1].description)

        // Verify properties
        assertEquals("package", loadedComponent.mainProperties[0].key)
        assertEquals("DFN-6", loadedComponent.mainProperties[0].value)
        assertEquals("mm", loadedComponent.mainProperties[0].unit)
    }

    @Test
    fun `loadTree with ComponentNode verifies content types`() {
        val sectionDir = tempDir.resolve("test-tree")
        Files.createDirectories(sectionDir)

        // Create root section node
        val rootMeta = NodeMeta(name = "Test Section", type = NodeType.SECTION, slug = "test-section")
        NodeRepository.save(sectionDir, rootMeta)

        // Create category
        val catSlug = NodeRepository.generateUniqueSlug(sectionDir, "Category")
        val catPath = NodeRepository.createContainer(sectionDir, catSlug, NodeMeta(name = "Category", type = NodeType.CATEGORY))

        // Create component leaf
        val compNode = ComponentNode(
            name = "Resistor 10k",
            description = "Generic resistor",
            ports = listOf(ComponentPort(1, "1", "P1", "Pad 1"), ComponentPort(2, "2", "P2", "Pad 2"))
        )
        val compSlug = NodeRepository.generateUniqueSlug(catPath, "resistor-10k")
        NodeRepository.createLeaf(catPath, compSlug, compNode)

        // Load tree
        val tree = NodeRepository.loadTree(sectionDir)

        assertEquals(3, tree.size) // root, category, component

        // Verify root is Container
        when (tree[0].content) {
            is NodeContent.Container -> {
                assertEquals(NodeType.SECTION, (tree[0].content as NodeContent.Container).meta.type)
            }
            else -> throw AssertionError("Root should be Container")
        }

        // Verify category is Container
        when (tree[1].content) {
            is NodeContent.Container -> {
                assertEquals(NodeType.CATEGORY, (tree[1].content as NodeContent.Container).meta.type)
            }
            else -> throw AssertionError("Category should be Container")
        }

        // Verify component is Leaf
        when (tree[2].content) {
            is NodeContent.Leaf -> {
                val leaf = tree[2].content as NodeContent.Leaf
                assertEquals("Resistor 10k", leaf.component.name)
                assertEquals(2, leaf.component.ports.size)
            }
            else -> throw AssertionError("Component should be Leaf")
        }
    }
}
