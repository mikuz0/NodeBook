package ru.nodebook.core.repo

import kotlinx.serialization.json.Json
import ru.nodebook.core.model.*
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

object NodeRepository {
    private val log = LoggerFactory.getLogger(NodeRepository::class.java)

    private val NODE_FILE = "node.json"
    private val NODE_FILE_EXTENSION = ".node"
    private val SLUG_PATTERN = Pattern.compile("[^a-z0-9-]")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(nodePath: Path): NodeRef? {
        return try {
            when {
                Files.isDirectory(nodePath) -> loadContainer(nodePath)
                nodePath.fileName.toString().endsWith(NODE_FILE_EXTENSION) -> loadLeaf(nodePath)
                else -> null
            }
        } catch (e: Exception) {
            log.error("Failed to load node from $nodePath", e)
            null
        }
    }

    fun loadTree(sectionDir: Path, maxDepth: Int = Int.MAX_VALUE): List<NodeRef> {
        return try {
            if (!Files.isDirectory(sectionDir)) return emptyList()
            val nodes = mutableListOf<NodeRef>()
            val root = load(sectionDir)
            if (root != null) {
                nodes.add(root)
                loadChildrenRecursive(sectionDir, nodes, 1, maxDepth)
            }
            nodes
        } catch (e: Exception) {
            log.error("Failed to load tree from $sectionDir", e)
            emptyList()
        }
    }

    fun listChildren(containerDir: Path): List<NodeRef> {
        return try {
            if (!Files.isDirectory(containerDir)) return emptyList()
            Files.list(containerDir).use { it.toList() }
                .filter { Files.isDirectory(it) || it.fileName.toString().endsWith(NODE_FILE_EXTENSION) }
                .mapNotNull { load(it) }
        } catch (e: Exception) {
            log.error("Failed to list children of $containerDir", e)
            emptyList()
        }
    }

    fun createContainer(parentDir: Path, slug: String, meta: NodeMeta): Path {
        val dir = parentDir.resolve(slug)
        Files.createDirectories(dir)
        save(dir, meta.copy(slug = slug))
        log.info("Created container node: $dir")
        return dir
    }

    fun createLeaf(parentDir: Path, slug: String, component: ComponentNode): Path {
        val file = parentDir.resolve("$slug$NODE_FILE_EXTENSION")
        saveLeaf(file, component)
        log.info("Created leaf node: $file")
        return file
    }

    fun save(nodePath: Path, meta: NodeMeta) {
        val file = nodePath.resolve(NODE_FILE)
        Files.createDirectories(nodePath)
        val content = json.encodeToString(NodeMeta.serializer(), meta)
        Files.write(file, content.toByteArray(Charsets.UTF_8))
    }

    fun delete(nodePath: Path) {
        try {
            if (Files.isDirectory(nodePath)) {
                Files.walkFileTree(nodePath, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        Files.delete(file)
                        return FileVisitResult.CONTINUE
                    }
                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        Files.delete(dir)
                        return FileVisitResult.CONTINUE
                    }
                })
            } else {
                Files.delete(nodePath)
            }
            log.info("Deleted node: $nodePath")
        } catch (e: IOException) {
            log.error("Failed to delete $nodePath", e)
            throw IllegalArgumentException("Failed to delete: ${e.message}", e)
        }
    }

    fun generateSlug(name: String): String {
        return SLUG_PATTERN.matcher(name.lowercase()).replaceAll("-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .takeIf { it.isNotEmpty() } ?: "untitled"
    }

    fun generateUniqueSlug(parentDir: Path, name: String): String {
        val baseSlug = generateSlug(name)
        var slug = baseSlug
        var counter = 1
        while (existsSlug(parentDir, slug)) {
            slug = "$baseSlug-$counter"
            counter++
        }
        return slug
    }

    private fun existsSlug(parentDir: Path, slug: String): Boolean {
        return Files.isDirectory(parentDir.resolve(slug)) ||
                Files.isRegularFile(parentDir.resolve("$slug$NODE_FILE_EXTENSION"))
    }

    private fun loadContainer(dir: Path): NodeRef? {
        val file = dir.resolve(NODE_FILE)
        if (!Files.isRegularFile(file)) return null
        val meta = json.decodeFromString(NodeMeta.serializer(), Files.readString(file))
        return NodeRef(dir, NodeContent.Container(meta), isContainer = true)
    }

    private fun loadLeaf(file: Path): NodeRef? {
        val component = json.decodeFromString(ComponentNode.serializer(), Files.readString(file))
        return NodeRef(file, NodeContent.Leaf(component), isContainer = false)
    }

    private fun saveLeaf(file: Path, component: ComponentNode) {
        val content = json.encodeToString(ComponentNode.serializer(), component)
        Files.write(file, content.toByteArray(Charsets.UTF_8))
    }

    private fun loadChildrenRecursive(dir: Path, nodes: MutableList<NodeRef>, currentDepth: Int, maxDepth: Int) {
        if (currentDepth >= maxDepth) return
        try {
            Files.list(dir).use { it.toList() }
                .filter { Files.isDirectory(it) || it.fileName.toString().endsWith(NODE_FILE_EXTENSION) }
                .mapNotNull { load(it) }
                .forEach { child ->
                    nodes.add(child)
                    if (child.isContainer) {
                        loadChildrenRecursive(child.path, nodes, currentDepth + 1, maxDepth)
                    }
                }
        } catch (e: IOException) {
            log.warn("Failed to list children of $dir", e)
        }
    }
}
