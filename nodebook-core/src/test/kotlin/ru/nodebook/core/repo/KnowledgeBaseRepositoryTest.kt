package ru.nodebook.core.repo

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
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
}
