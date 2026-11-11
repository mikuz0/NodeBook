package ru.nodebook.core.repo

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.nodebook.core.model.NodeMeta
import ru.nodebook.core.model.NodeType
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
        assertEquals(NodeType.SECTION, loaded.meta.type)
        assertEquals(slug, loaded.meta.slug)
        assertEquals("Test Container", loaded.meta.name)
        assertTrue(loaded.isContainer)
    }

    @Test
    fun `createLeaf and load returns correct node`() {
        val parentDir = tempDir.resolve("parent")
        Files.createDirectories(parentDir)

        val meta = NodeMeta(name = "Test Component", type = NodeType.COMPONENT)
        val slug = NodeRepository.generateUniqueSlug(parentDir, "Test Component")
        val leafPath = NodeRepository.createLeaf(parentDir, slug, meta)

        // Verify file exists
        assertTrue(Files.isRegularFile(leafPath))
        assertTrue(leafPath.fileName.toString().endsWith(".node"))

        // Load should return the node
        val loaded = NodeRepository.load(leafPath)
        assertNotNull(loaded)
        assertEquals(NodeType.COMPONENT, loaded.meta.type)
        assertEquals(slug, loaded.meta.slug)
        assertEquals("Test Component", loaded.meta.name)
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

        // Create a leaf component inside category
        val compSlug = NodeRepository.generateUniqueSlug(catPath, "STM32F103")
        NodeRepository.createLeaf(catPath, compSlug, NodeMeta(name = "STM32F103", type = NodeType.COMPONENT))

        // Load tree
        val tree = NodeRepository.loadTree(sectionDir)

        // Should have: root (components), manufacturer, category, component
        assertEquals(4, tree.size)
        assertEquals(NodeType.SECTION, tree[0].meta.type)
        assertEquals(NodeType.MANUFACTURER, tree[1].meta.type)
        assertEquals(NodeType.CATEGORY, tree[2].meta.type)
        assertEquals(NodeType.COMPONENT, tree[3].meta.type)
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
        NodeRepository.createLeaf(parentDir, child2Slug, NodeMeta(name = "Child 2", type = NodeType.COMPONENT))

        val children = NodeRepository.listChildren(parentDir)

        // Should have 2 children (not counting the root node.json itself)
        assertEquals(2, children.size)
    }
}
