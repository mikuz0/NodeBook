package ru.nodebook.ui

import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage
import kotlinx.serialization.json.Json
import ru.nodebook.core.model.NodeRef
import ru.nodebook.core.repo.NodeRepository
import java.nio.file.Path
import org.slf4j.LoggerFactory

class SectionWindow : BookAware {

    private val log = LoggerFactory.getLogger(SectionWindow::class.java)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    private val stage = Stage()
    private val treeView = TreeView<TreeItemData>()
    private val titleLabel = Label()
    private val jsonArea = TextArea().apply {
        isEditable = false
        prefRowCount = 20
    }

    var onClose: (() -> Unit)? = null

    override fun getStage(): Stage? = stage

    override fun showWindow() {
        stage.title = "Section"
        stage.minWidth = 900.0
        stage.minHeight = 600.0
        stage.show()
    }

    override fun openSection(bookPath: Path, section: String) {
        stage.title = "Section: $section"

        // Build tree in background
        val sectionDir = bookPath.resolve(section)
        val task = object : Task<List<NodeRef>>() {
            override fun call(): List<NodeRef> {
                return NodeRepository.loadTree(sectionDir)
            }

            override fun succeeded() {
                buildTree(value ?: emptyList())
            }

            override fun failed() {
                log.error("Failed to load section tree", exception)
                UiUtils.errorAlert(stage, "Load Error", "Failed to load section: ${exception?.message}")
            }
        }
        java.util.concurrent.Executors.newSingleThreadExecutor().submit(task)
    }

    private fun buildTree(nodes: List<NodeRef>) {
        val rootItem = TreeItemData("root", null)
        val rootTreeItem = javafx.scene.control.TreeItem(rootItem)
        rootTreeItem.isExpanded = true

        nodes.forEach { nodeRef ->
            val treeItem = createTreeItem(nodeRef, rootTreeItem)
            rootTreeItem.children.add(treeItem)
        }

        treeView.root = rootTreeItem
        treeView.selectionModel.selectedItemProperty().addListener { _, _, newSel ->
            newSel?.let { treeItem ->
                val data = treeItem.value
                if (data.nodeRef != null) {
                    val nr = data.nodeRef!!
                    titleLabel.text = nr.meta.name.ifBlank { nr.meta.slug }
                    jsonArea.text = json.encodeToString(ru.nodebook.core.model.NodeMeta.serializer(), nr.meta)
                } else {
                    titleLabel.text = "Root"
                    jsonArea.text = ""
                }
            }
        }
    }

    private fun createTreeItem(nodeRef: NodeRef, parent: javafx.scene.control.TreeItem<TreeItemData>): javafx.scene.control.TreeItem<TreeItemData> {
        val displayName = nodeRef.meta.name.ifBlank { nodeRef.meta.slug }
        val data = TreeItemData(displayName, nodeRef)
        val item = javafx.scene.control.TreeItem(data)

        if (nodeRef.isContainer) {
            // Load children in background
            java.util.concurrent.Executors.newSingleThreadExecutor().submit {
                val childRefs = NodeRepository.listChildren(nodeRef.path)
                if (childRefs.isNotEmpty()) {
                    javafx.application.Platform.runLater {
                        item.children.clear()
                        childRefs.forEach { child ->
                            item.children.add(createTreeItem(child, item))
                        }
                        item.isExpanded = true
                    }
                }
            }
        }

        return item
    }

    private data class TreeItemData(
        val displayName: String,
        val nodeRef: NodeRef?
    ) {
        override fun toString(): String = displayName
    }

    init {
        val refreshBtn = Button("Refresh").apply {
            setOnAction {
                UiUtils.infoAlert(stage, "Info", "Refresh requires re-opening the section")
            }
        }

        val closeBtn = Button("Close").apply {
            setOnAction {
                stage.close()
                onClose?.invoke()
            }
        }

        val buttonBox = HBox(10.0, refreshBtn, closeBtn).apply {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(10.0)
        }

        val headerBox = VBox(5.0, titleLabel).apply {
            padding = Insets(10.0)
            style = "-fx-border-color: #ddd; -fx-border-width: 0 0 1 0;"
        }

        val rightPane = VBox(10.0, headerBox, jsonArea).apply {
            padding = Insets(10.0)
            HBox.setHgrow(jsonArea, Priority.ALWAYS)
            VBox.setVgrow(jsonArea, Priority.ALWAYS)
        }

        val leftPane = Pane().apply {
            val scrollPane = ScrollPane(treeView).apply {
                prefWidth = 300.0
            }
            children.add(scrollPane)
        }

        val splitPane = SplitPane()
        splitPane.getItems().addAll(leftPane, rightPane)
        splitPane.getDividerPositions().set(0, 0.35)

        val root = VBox(0.0, splitPane, buttonBox)
        stage.scene = Scene(root, 900.0, 600.0)
    }
}
