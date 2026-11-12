package ru.nodebook.core.repo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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

    /**
     * Load a node from a path (either a directory with node.json or a .node file).
     */
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

    /**
     * Recursively load the tree from a section directory.
     */
    fun loadTree(sectionDir: Path, maxDepth: Int = Int.MAX_VALUE): List<NodeRef> {
        return try {
            if (!Files.isDirectory(sectionDir)) {
                log.warn("Section directory does not exist or is not a directory: $sectionDir")
                return emptyList()
            }
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

    /**
     * List direct children of a container directory.
     */
    fun listChildren(containerDir: Path): List<NodeRef> {
        return try {
            if (!Files.isDirectory(containerDir)) {
                log.warn("Container directory does not exist: $containerDir")
                return emptyList()
            }
            val children = Files.list(containerDir).use { it.toList() }
            children.filter { Files.isDirectory(it) || it.fileName.toString().endsWith(NODE_FILE_EXTENSION) }
                .mapNotNull { load(it) }
                .toList()
        } catch (e: Exception) {
            log.error("Failed to list children of $containerDir", e)
            emptyList()
        }
    }

    /**
     * Create a container node (directory with node.json).
     */
    fun createContainer(parentDir: Path, slug: String, meta: NodeMeta): Path {
        return try {
            val dir = parentDir.resolve(slug)
            Files.createDirectories(dir)
            save(dir, meta.copy(slug = slug))
            log.info("Created container node: $dir")
            dir
        } catch (e: IOException) {
            log.error("Failed to create container at $parentDir/$slug", e)
            throw IllegalArgumentException("Failed to create container: ${e.message}", e)
        }
    }

    /**
     * Create a leaf node (file with .node extension).
     */
    fun createLeaf(parentDir: Path, slug: String, meta: NodeMeta): Path {
        return try {
            val file = parentDir.resolve("$slug$NODE_FILE_EXTENSION")
            saveLeaf(file, meta.copy(slug = slug))
            log.info("Created leaf node: $file")
            file
        } catch (e: IOException) {
            log.error("Failed to create leaf at $parentDir/$slug", e)
            throw IllegalArgumentException("Failed to create leaf: ${e.message}", e)
        }
    }

    /**
     * Save metadata to a container's node.json.
     */
    fun save(nodePath: Path, meta: NodeMeta) {
        try {
            val file = nodePath.resolve(NODE_FILE)
            Files.createDirectories(nodePath)
            val content = json.encodeToString(NodeMeta.serializer(), meta)
            Files.write(file, content.toByteArray(Charsets.UTF_8))
        } catch (e: IOException) {
            log.error("Failed to save node to $nodePath", e)
            throw IllegalArgumentException("Failed to save node: ${e.message}", e)
        }
    }

    /**
     * Delete a node (recursively if it's a directory).
     */
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

    /**
     * Generate a slug from a name: lowercase, spaces to -, only [a-z0-9-].
     */
    fun generateSlug(name: String): String {
        return SLUG_PATTERN.matcher(name.lowercase()).replaceAll("-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .takeIf { it.isNotEmpty() } ?: "untitled"
    }

    /**
     * Generate a unique slug by appending -1, -2, etc. if there's a collision.
     */
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
        val dirPath = parentDir.resolve(slug)
        if (Files.isDirectory(dirPath)) return true
        val filePath = parentDir.resolve("$slug$NODE_FILE_EXTENSION")
        if (Files.isRegularFile(filePath)) return true
        return false
    }

    // ---- Private helpers ----

    private fun loadContainer(dir: Path): NodeRef? {
        val file = dir.resolve(NODE_FILE)
        if (!Files.isRegularFile(file)) return null
        val meta = json.decodeFromString(NodeMeta.serializer(), Files.readString(file))
        return NodeRef(dir, meta, isContainer = true)
    }

    private fun loadLeaf(file: Path): NodeRef? {
        val meta = json.decodeFromString(NodeMeta.serializer(), Files.readString(file))
        return NodeRef(file, meta, isContainer = false)
    }

    private fun saveLeaf(file: Path, meta: NodeMeta) {
        val content = json.encodeToString(NodeMeta.serializer(), meta)
        Files.write(file, content.toByteArray(Charsets.UTF_8))
    }

    private fun loadChildrenRecursive(
        dir: Path,
        nodes: MutableList<NodeRef>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth >= maxDepth) return
        try {
            val children = Files.list(dir).use { it.toList() }
            children.filter { Files.isDirectory(it) || it.fileName.toString().endsWith(NODE_FILE_EXTENSION) }
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
