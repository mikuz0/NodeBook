package ru.nodebook.core.repo

import kotlinx.serialization.json.Json
import ru.nodebook.core.model.KnowledgeBase
import ru.nodebook.core.model.KnowledgeBaseInfo
import ru.nodebook.core.model.NodeMeta
import ru.nodebook.core.model.NodeType
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import org.slf4j.LoggerFactory

object KnowledgeBaseRepository {

    private val log = LoggerFactory.getLogger(KnowledgeBaseRepository::class.java)
    private val BOOK_FILE = "book.json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Create a new knowledge base: directory with book.json and section directories.
     */
    fun create(name: String, workspace: Path): Path {
        return try {
            val bookDir = workspace.resolve(name)
            Files.createDirectories(bookDir)

            // Save book.json
            val book = KnowledgeBase(name = name)
            val bookFile = bookDir.resolve(BOOK_FILE)
            val content = json.encodeToString(KnowledgeBase.serializer(), book)
            Files.write(bookFile, content.toByteArray(Charsets.UTF_8))

            // Create section directories with node.json
            book.sections.forEach { sectionName ->
                val sectionDir = bookDir.resolve(sectionName)
                Files.createDirectories(sectionDir)
                val sectionMeta = NodeMeta(
                    schemaVersion = 1,
                    slug = sectionName,
                    type = NodeType.SECTION,
                    name = sectionName,
                    description = "Section: $sectionName"
                )
                NodeRepository.save(sectionDir, sectionMeta)
            }

            log.info("Created knowledge base: $bookDir")
            bookDir
        } catch (e: Exception) {
            log.error("Failed to create knowledge base '$name' in $workspace", e)
            throw IllegalArgumentException("Failed to create knowledge base: ${e.message}", e)
        }
    }

    /**
     * Load a knowledge base from its directory.
     */
    fun load(bookDir: Path): KnowledgeBase {
        return try {
            val bookFile = bookDir.resolve(BOOK_FILE)
            if (!Files.isRegularFile(bookFile)) {
                log.error("book.json not found at $bookFile")
                throw IllegalArgumentException("Not a valid knowledge base: $bookDir (missing book.json)")
            }
            val content = Files.readString(bookFile)
            json.decodeFromString(KnowledgeBase.serializer(), content)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            log.error("Failed to load knowledge base from $bookDir", e)
            throw IllegalArgumentException("Failed to load knowledge base: ${e.message}", e)
        }
    }

    /**
     * Save a knowledge base to its directory.
     */
    fun save(bookDir: Path, book: KnowledgeBase) {
        try {
            val bookFile = bookDir.resolve(BOOK_FILE)
            val content = json.encodeToString(KnowledgeBase.serializer(), book)
            Files.write(bookFile, content.toByteArray(Charsets.UTF_8))
            log.info("Saved knowledge base: $bookDir")
        } catch (e: Exception) {
            log.error("Failed to save knowledge base to $bookDir", e)
            throw IllegalArgumentException("Failed to save knowledge base: ${e.message}", e)
        }
    }

    /**
     * Scan a workspace directory for knowledge bases by finding book.json files.
     */
    fun scan(workspace: Path): List<KnowledgeBaseInfo> {
        return try {
            if (!Files.isDirectory(workspace)) {
                log.warn("Workspace does not exist or is not a directory: $workspace")
                return emptyList()
            }
            val books = mutableListOf<KnowledgeBaseInfo>()
            Files.list(workspace).use { stream ->
                stream.filter { Files.isDirectory(it) && Files.isRegularFile(it.resolve(BOOK_FILE)) }
                    .forEach { bookDir ->
                        try {
                            val book = load(bookDir)
                            books.add(KnowledgeBaseInfo(book.name, bookDir))
                        } catch (e: Exception) {
                            log.warn("Skipping invalid knowledge base at $bookDir: ${e.message}")
                        }
                    }
            }
            books.sorted()
        } catch (e: Exception) {
            log.error("Failed to scan workspace $workspace", e)
            emptyList()
        }
    }
}
