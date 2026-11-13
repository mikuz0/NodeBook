package ru.nodebook.core.repo

import kotlinx.serialization.json.Json
import ru.nodebook.core.model.KnowledgeBase
import ru.nodebook.core.model.KnowledgeBaseInfo
import ru.nodebook.core.model.NodeMeta
import ru.nodebook.core.model.NodeType
import ru.nodebook.core.samples.ComponentSamples
import ru.nodebook.core.model.SectionView
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

    private val VIEW_FILE = "view.json"

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

            // Create section directories with node.json and view.json
            val sectionTitles = mapOf(
                "components" to "Components",
                "modules" to "Modules",
                "standards" to "Standards"
            )
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

                // Create view.json for each section
                val view = SectionView(
                    schemaVersion = 1,
                    kind = "tree",
                    title = sectionTitles[sectionName] ?: sectionName.replaceFirstChar { it.uppercaseChar() }
                )
                val viewFile = sectionDir.resolve(VIEW_FILE)
                val viewContent = json.encodeToString(SectionView.serializer(), view)
                Files.write(viewFile, viewContent.toByteArray(Charsets.UTF_8))
            }

            // Create sample component structure in components section
            val componentsDir = bookDir.resolve("components")
            createSampleComponentStructure(componentsDir)

            // Create empty section directories for modules and standards
            Files.createDirectories(bookDir.resolve("modules"))
            Files.createDirectories(bookDir.resolve("standards"))

            log.info("Created knowledge base: $bookDir")
            bookDir
        } catch (e: Exception) {
            log.error("Failed to create knowledge base '$name' in $workspace", e)
            throw IllegalArgumentException("Failed to create knowledge base: ${e.message}", e)
        }
    }

    /**
     * Create sample component structure in the components section directory.
     */
    private fun createSampleComponentStructure(componentsDir: Path) {
        try {
            // Define categories and which samples belong to each
            val categorySamples = mapOf(
                Pair("Resistor", "resistor") to listOf("resistor"),
                Pair("Capacitor", "capacitor") to listOf("capacitor"),
                Pair("Microcontroller", "microcontroller") to listOf("atmega328p", "stm32f103c8t6", "phy6222qc-w04"),
                Pair("Sensor", "sensor") to listOf("cht8310", "cht8320")
            )

            categorySamples.forEach { (categoryKey, sampleSlugs) ->
                val categoryName = categoryKey.first
                val categorySlug = categoryKey.second
                val categoryDir = componentsDir.resolve(categorySlug)
                Files.createDirectories(categoryDir)
                val categoryMeta = NodeMeta(
                    schemaVersion = 1,
                    slug = categorySlug,
                    type = NodeType.CATEGORY,
                    name = categoryName,
                    description = "Category: $categoryName"
                )
                NodeRepository.save(categoryDir, categoryMeta)

                // Create manufacturer directory in each category
                val manufacturerDir = categoryDir.resolve("manufacturer")
                Files.createDirectories(manufacturerDir)
                val manufacturerMeta = NodeMeta(
                    schemaVersion = 1,
                    slug = "manufacturer",
                    type = NodeType.MANUFACTURER,
                    name = "Manufacturer",
                    description = "Manufacturer placeholder"
                )
                NodeRepository.save(manufacturerDir, manufacturerMeta)

                // Create component leaf nodes for this category
                ComponentSamples.all().forEach { sample ->
                    if (sample.slug in sampleSlugs) {
                        // Generate unique slug to avoid collisions
                        val uniqueSlug = NodeRepository.generateUniqueSlug(manufacturerDir, sample.name)
                        val meta = NodeMeta(
                            schemaVersion = 1,
                            slug = uniqueSlug,
                            type = NodeType.COMPONENT,
                            name = sample.name,
                            description = sample.description,
                            properties = sample.toProperties()
                        )
                        NodeRepository.createLeaf(manufacturerDir, uniqueSlug, meta)
                    }
                }
            }

            log.info("Created sample component structure in $componentsDir")
        } catch (e: Exception) {
            log.warn("Failed to create sample component structure in $componentsDir", e)
            // Don't fail book creation if samples fail
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

    /**
     * Проверить, есть ли view.json в папке раздела.
     */
    fun hasSectionView(sectionDir: Path): Boolean {
        return Files.isRegularFile(sectionDir.resolve(VIEW_FILE))
    }

    /**
     * Загрузить SectionView из папки раздела.
     * Возвращает null, если view.json отсутствует или некорректен.
     */
    fun loadSectionView(sectionDir: Path): SectionView? {
        return try {
            val viewFile = sectionDir.resolve(VIEW_FILE)
            if (!Files.isRegularFile(viewFile)) return null
            val content = Files.readString(viewFile)
            json.decodeFromString(SectionView.serializer(), content)
        } catch (e: Exception) {
            log.warn("Failed to load section view from $sectionDir: ${e.message}")
            null
        }
    }
}
