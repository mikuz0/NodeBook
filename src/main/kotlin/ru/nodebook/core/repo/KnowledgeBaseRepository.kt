package ru.nodebook.core.repo

import kotlinx.serialization.json.Json
import ru.nodebook.core.model.*
import ru.nodebook.core.samples.ComponentSamples
import java.nio.file.*
import org.slf4j.LoggerFactory

object KnowledgeBaseRepository {
    private val log = LoggerFactory.getLogger(KnowledgeBaseRepository::class.java)

    private val BOOK_FILE = "book.json"
    private val VIEW_FILE = "view.json"
    private val FIELDS_FILE = "fields.json"
    private val TYPE_VIEW_FILE = "view.json"

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

            // Create section directories with node.json and view.json
            createSection(bookDir, "components", "Components", "🔌", "Electronic components library")
            createSection(bookDir, "modules", "Modules", "🔧", "Circuit modules and assemblies")
            createSection(bookDir, "standards", "Standards", "📋", "Technical standards and specifications")

            // Create sample component structure in components section
            val componentsDir = bookDir.resolve("components")
            createSampleComponentStructure(componentsDir)

            log.info("Created knowledge base: $bookDir")
            bookDir
        } catch (e: Exception) {
            log.error("Failed to create knowledge base '$name' in $workspace", e)
            throw IllegalArgumentException("Failed to create knowledge base: ${e.message}", e)
        }
    }

    /**
     * Создать раздел: директория + node.json + view.json
     */
    private fun createSection(bookDir: Path, sectionName: String, title: String, icon: String, description: String) {
        val sectionDir = bookDir.resolve(sectionName)
        Files.createDirectories(sectionDir)

        // node.json для раздела
        val sectionMeta = NodeMeta(
            schemaVersion = 1,
            slug = sectionName,
            type = NodeType.SECTION,
            name = sectionName,
            description = "Section: $sectionName"
        )
        NodeRepository.save(sectionDir, sectionMeta)

        // view.json для раздела
        val view = SectionView(
            schemaVersion = 1,
            kind = "tree",
            title = title,
            description = description,
            icon = icon,
            listMode = "tree",
            listColumns = emptyList(),
            cardBlocks = listOf("header", "mainProperties", "ports", "extraProperties"),
            portColumns = listOf("number", "pinNumber", "name", "description")
        )
        val viewFile = sectionDir.resolve(VIEW_FILE)
        val viewContent = json.encodeToString(SectionView.serializer(), view)
        Files.write(viewFile, viewContent.toByteArray(Charsets.UTF_8))
    }

    /**
     * Create sample component structure in the components section directory.
     */
    private fun createSampleComponentStructure(componentsDir: Path) {
        try {
            // Define categories and which samples belong to each
            val categorySamples = mapOf(
                "resistor" to listOf("Resistor"),
                "capacitor" to listOf("Capacitor"),
                "microcontroller" to listOf("ATmega328P", "STM32F103C8T6", "PHY6222QC-W04"),
                "sensor" to listOf("CHT8310", "CHT8320")
            )

            categorySamples.forEach { (categorySlug, sampleNames) ->
                val categoryDir = componentsDir.resolve(categorySlug)
                Files.createDirectories(categoryDir)

                // node.json для категории
                val categoryMeta = NodeMeta(
                    schemaVersion = 1,
                    slug = categorySlug,
                    type = NodeType.CATEGORY,
                    name = categorySlug.replaceFirstChar { it.uppercaseChar() },
                    description = "Category: $categorySlug"
                )
                NodeRepository.save(categoryDir, categoryMeta)

                // view.json для категории (шаблон просмотра)
                createCategoryView(categoryDir, categorySlug)

                // Manufacturer директория
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

                // Компоненты (листья) в новом формате
                ComponentSamples.all().forEach { sample ->
                    if (sample.name in sampleNames) {
                        val uniqueSlug = NodeRepository.generateUniqueSlug(manufacturerDir, sample.name)
                        NodeRepository.createLeaf(manufacturerDir, uniqueSlug, sample)
                    }
                }
            }

            log.info("Created sample component structure in $componentsDir")
        } catch (e: Exception) {
            log.warn("Failed to create sample component structure in $componentsDir", e)
        }
    }

    /**
     * Создать view.json для категории с настройками отображения.
     */
    private fun createCategoryView(categoryDir: Path, categorySlug: String) {
        val view = when (categorySlug) {
            "resistor" -> SectionView(
                schemaVersion = 1,
                kind = "tree",
                title = "Resistors",
                description = "Resistor components",
                icon = "⚡",
                listMode = "table",
                listColumns = listOf("Номинал", "Мощность", "Корпус"),
                cardBlocks = listOf("header", "mainProperties", "ports", "extraProperties"),
                portColumns = listOf("number", "pinNumber", "name", "description"),
                unitDictionaries = mapOf(
                    "Номинал" to listOf("Ом", "кОм", "МОм"),
                    "Мощность" to listOf("Вт", "мВт"),
                    "Допуск" to listOf("%")
                )
            )
            "capacitor" -> SectionView(
                schemaVersion = 1,
                kind = "tree",
                title = "Capacitors",
                description = "Capacitor components",
                icon = "⚡",
                listMode = "table",
                listColumns = listOf("Ёмкость", "Напряжение", "Корпус", "Диэлектрик"),
                cardBlocks = listOf("header", "mainProperties", "ports", "extraProperties"),
                portColumns = listOf("number", "pinNumber", "name", "description"),
                unitDictionaries = mapOf(
                    "Ёмкость" to listOf("пФ", "нФ", "мкФ"),
                    "Напряжение" to listOf("В", "кВ"),
                    "Допуск" to listOf("%")
                )
            )
            "microcontroller" -> SectionView(
                schemaVersion = 1,
                kind = "tree",
                title = "Microcontrollers",
                description = "MCU and SoC components",
                icon = "🔲",
                listMode = "table",
                listColumns = listOf("Ядро", "Частота", "Flash", "RAM", "Корпус"),
                cardBlocks = listOf("header", "mainProperties", "ports", "extraProperties"),
                portColumns = listOf("number", "pinNumber", "name", "description"),
                unitDictionaries = mapOf(
                    "Частота" to listOf("МГц", "ГГц"),
                    "Flash" to listOf("KB", "MB"),
                    "RAM" to listOf("KB", "MB")
                )
            )
            "sensor" -> SectionView(
                schemaVersion = 1,
                kind = "tree",
                title = "Sensors",
                description = "Sensor components",
                icon = "📡",
                listMode = "table",
                listColumns = listOf("Тип", "Интерфейс", "Корпус"),
                cardBlocks = listOf("header", "mainProperties", "ports", "extraProperties"),
                portColumns = listOf("number", "pinNumber", "name", "description")
            )
            else -> SectionView(
                schemaVersion = 1,
                kind = "tree",
                title = categorySlug.replaceFirstChar { it.uppercaseChar() }
            )
        }

        val viewFile = categoryDir.resolve("view.json")
        val viewContent = json.encodeToString(SectionView.serializer(), view)
        Files.write(viewFile, viewContent.toByteArray(Charsets.UTF_8))

        // Create fields.json (placeholder)
        val fieldsFile = categoryDir.resolve(FIELDS_FILE)
        Files.writeString(fieldsFile, "{\"schemaVersion\": 1, \"fields\": []}")
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

    /**
     * Проверить, есть ли fields.json в папке типа.
     */
    fun hasTypeFields(typeDir: Path): Boolean {
        return Files.isRegularFile(typeDir.resolve(FIELDS_FILE))
    }

    /**
     * Проверить, есть ли view.json типа в папке типа.
     */
    fun hasTypeView(typeDir: Path): Boolean {
        return Files.isRegularFile(typeDir.resolve(TYPE_VIEW_FILE))
    }
}
