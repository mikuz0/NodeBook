package ru.nodebook.ui.section

import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.util.Callback
import ru.nodebook.core.model.*
import ru.nodebook.core.repo.NodeRepository
import java.nio.file.Path
import org.slf4j.LoggerFactory
import ru.nodebook.ui.common.UiUtils

class SectionWindow : BookAware {
    private val log = LoggerFactory.getLogger(SectionWindow::class.java)
    private val executor = java.util.concurrent.Executors.newFixedThreadPool(2) { r ->
        Thread(r, "section-io").apply { isDaemon = true }
    }
    private val stage = Stage()
    private val treeView = javafx.scene.control.TreeView<TreeItemData>()
    private lateinit var currentSectionDir: Path
    private lateinit var currentSectionName: String

    // Generic form fields
    private val nameField = TextField()
    private val descriptionArea = TextArea().apply { prefRowCount = 4 }
    private val typeLabel = Label()
    private val slugLabel = Label()
    private val saveBtn = Button("Save")
    private val revertBtn = Button("Revert")
    private val statusLabel = Label().apply { style = "-fx-text-fill: gray; -fx-padding: 5 0 0 0;" }

    // Component form fields
    private val componentNameField = TextField().apply { isEditable = false }
    private val componentDescriptionArea = TextArea().apply { isEditable = false; prefRowCount = 3 }
    private val componentTypeLabel = Label()
    private val componentSlugLabel = Label()
    private val componentRefreshBtn = Button("Refresh Component")

    // Ports table
    private val portNumberCol = TableColumn<PortRow, Int>("#")
    private val portPinNumberCol = TableColumn<PortRow, String>("Pin#")
    private val portNameCol = TableColumn<PortRow, String>("Name")
    private val portDescriptionCol = TableColumn<PortRow, String>("Description")
    private val portTableView = TableView<PortRow>().apply { prefHeight = 200.0 }

    var onClose: (() -> Unit)? = null
    private var isDirty = false
    private var currentMeta: NodeMeta? = null
    private var currentComponent: ComponentNode? = null
    private var currentNodeRef: NodeRef? = null
    private var isReadOnly = false
    private var currentPortData: ObservableList<PortRow> = FXCollections.observableArrayList()

    private lateinit var genericPane: VBox
    private lateinit var componentScrollPane: ScrollPane
    private lateinit var splitPane: SplitPane

    override fun getStage(): Stage? = stage
    override fun showWindow() {
        stage.minWidth = 900.0
        stage.minHeight = 600.0
        stage.show()
    }

    override fun openSection(bookPath: Path, section: String) {
        currentSectionName = section
        currentSectionDir = bookPath.resolve(section)
        isReadOnly = section !in listOf("components")

        val bookName = bookPath.fileName?.toString() ?: "unknown"
        stage.title = "NodeBook — $bookName / $section"
        stage.minWidth = 900.0
        stage.minHeight = 600.0
        stage.show()

        val task = object : Task<List<NodeRef>>() {
            override fun call(): List<NodeRef> = NodeRepository.loadTree(currentSectionDir)
            override fun succeeded() {
                buildTree(value ?: emptyList())
                updateFormReadOnly(isReadOnly)
            }
            override fun failed() {
                log.error("Failed to load section tree", exception)
                UiUtils.errorAlert(stage, "Load Error", "Failed to load section: ${exception?.message}")
            }
        }
        executor.submit(task)
    }

    private fun updateFormReadOnly(readOnly: Boolean) {
        nameField.isEditable = !readOnly
        descriptionArea.isEditable = !readOnly
        saveBtn.isVisible = !readOnly
        revertBtn.isVisible = !readOnly
        updateFormState()
    }

    private fun populateForm(meta: NodeMeta) {
        nameField.text = meta.name
        descriptionArea.text = meta.description
        typeLabel.text = meta.type.name
        slugLabel.text = meta.slug.ifBlank { "(auto-generated)" }
        currentMeta = meta
    }

    private fun updateFormState() {
        val hasSelection = currentNodeRef != null
        saveBtn.isDisable = !hasSelection || !isDirty
        revertBtn.isDisable = !isDirty
    }

    private fun buildTree(nodes: List<NodeRef>) {
        val rootItem = TreeItemData("root", null)
        val rootTreeItem = javafx.scene.control.TreeItem(rootItem)
        rootTreeItem.isExpanded = true
        nodes.forEach { nodeRef ->
            rootTreeItem.children.add(createTreeItem(nodeRef, rootTreeItem))
        }
        treeView.root = rootTreeItem
        setupSelectionListener()
        setupContextMenu()
    }

    private fun setupSelectionListener() {
        treeView.selectionModel.selectedItemProperty().addListener(
            ChangeListener { _, _, newSel ->
                newSel?.let { treeItem ->
                    val data = treeItem.value
                    if (data.nodeRef != null) {
                        currentNodeRef = data.nodeRef!!
                        isDirty = false

                        when (val content = data.nodeRef!!.content) {
                            is NodeContent.Leaf -> {
                                currentComponent = content.component
                                currentMeta = null
                                showComponentPane(currentComponent!!)
                            }
                            is NodeContent.Container -> {
                                currentMeta = content.meta
                                currentComponent = null
                                showGenericPane(currentMeta!!)
                            }
                        }
                    } else {
                        currentNodeRef = null
                        currentMeta = null
                        currentComponent = null
                        clearAllPanes()
                    }
                }
            }
        )
    }

    private fun showComponentPane(component: ComponentNode) {
        splitPane.getItems().remove(genericPane)
        if (!splitPane.getItems().contains(componentScrollPane)) {
            splitPane.getItems().add(componentScrollPane)
        }
        splitPane.getDividerPositions().set(0, 0.35)
        updateComponentPane(component)
    }

    private fun showGenericPane(meta: NodeMeta) {
        splitPane.getItems().remove(componentScrollPane)
        if (!splitPane.getItems().contains(genericPane)) {
            splitPane.getItems().add(genericPane)
        }
        splitPane.getDividerPositions().set(0, 0.35)
        populateForm(meta)
        updateFormState()
    }

    private fun clearAllPanes() {
        nameField.text = ""
        descriptionArea.text = ""
        typeLabel.text = ""
        slugLabel.text = ""
        componentNameField.text = ""
        componentDescriptionArea.text = ""
        componentTypeLabel.text = ""
        componentSlugLabel.text = ""
        currentPortData.clear()
    }

    private fun createTreeItem(nodeRef: NodeRef, parent: javafx.scene.control.TreeItem<TreeItemData>): javafx.scene.control.TreeItem<TreeItemData> {
        val displayName = nodeRef.name.ifBlank { nodeRef.slug }
        val data = TreeItemData(displayName, nodeRef)
        val item = javafx.scene.control.TreeItem(data)
        if (nodeRef.isContainer) {
            executor.submit {
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

    private fun setupContextMenu() {
        val contextMenu = ContextMenu()
        val newTypeItem = MenuItem("New type...")
        val newManufacturerItem = MenuItem("New manufacturer...")
        val newComponentItem = MenuItem("New component...")
        val deleteItem = MenuItem("Delete")

        newTypeItem.setOnAction { handleCreate(nodeRef = null, expectedType = NodeType.CATEGORY) }
        newManufacturerItem.setOnAction { handleCreate(nodeRef = currentNodeRef, expectedType = NodeType.MANUFACTURER) }
        newComponentItem.setOnAction { handleCreate(nodeRef = currentNodeRef, expectedType = NodeType.COMPONENT) }
        deleteItem.setOnAction { handleDelete() }

        contextMenu.items.setAll(newTypeItem, newManufacturerItem, newComponentItem, SeparatorMenuItem(), deleteItem)
        if (isReadOnly) {
            listOf(newTypeItem, newManufacturerItem, newComponentItem, deleteItem).forEach { it.isVisible = false }
        }

        treeView.onMousePressed = javafx.event.EventHandler { event: javafx.scene.input.MouseEvent ->
            if (event.button == MouseButton.SECONDARY) {
                val selectedItem = treeView.selectionModel.selectedItem
                selectedItem?.let { contextMenu.show(treeView, event.screenX, event.screenY) }
            }
        }
    }

    private fun handleCreate(nodeRef: NodeRef?, expectedType: NodeType) {
        val parentDir = when (expectedType) {
            NodeType.CATEGORY -> currentSectionDir
            NodeType.MANUFACTURER -> nodeRef?.path ?: currentSectionDir
            NodeType.COMPONENT -> nodeRef?.path ?: currentSectionDir
            else -> currentSectionDir
        }

        val dialog = Dialog<Pair<String, String>>()
        dialog.title = "New $expectedType"
        dialog.headerText = "Enter name for new node"
        dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val nameField = TextField().apply { promptText = "Name (required)" }
        val descArea = TextArea().apply { promptText = "Description (optional)"; prefRowCount = 3 }
        dialog.dialogPane.content = VBox(10.0, nameField, descArea)
        dialog.setResultConverter(Callback { button: ButtonType ->
            if (button == ButtonType.OK) Pair(nameField.text, descArea.text) else null
        })

        val result = dialog.showAndWait()
        if (result.isPresent) {
            val pair = result.get()
            val name = pair.first
            val desc = pair.second
            if (name.isBlank()) {
                UiUtils.errorAlert(stage, "Error", "Name is required")
                return
            }

            val slug = NodeRepository.generateUniqueSlug(parentDir, name)

            val task = object : Task<Unit>() {
                override fun call() {
                    when (expectedType) {
                        NodeType.CATEGORY, NodeType.MANUFACTURER -> {
                            val meta = NodeMeta(type = expectedType, name = name, description = desc)
                            NodeRepository.createContainer(parentDir, slug, meta)
                        }
                        NodeType.COMPONENT -> {
                            val component = ComponentNode(name = name, description = desc, refPrefix = "X")
                            NodeRepository.createLeaf(parentDir, slug, component)
                        }
                        else -> throw IllegalArgumentException("Unexpected type: $expectedType")
                    }
                }
                override fun succeeded() {
                    refreshParentChildren(parentDir)
                    statusLabel.text = "Created: $name"
                }
                override fun failed() {
                    UiUtils.errorAlert(stage, "Create Error", exception?.message ?: "Unknown")
                }
            }
            executor.submit(task)
        }
    }

    private fun handleDelete() {
        val nodeRef = currentNodeRef ?: return
        if (nodeRef.type == NodeType.SECTION) {
            UiUtils.infoAlert(stage, "Info", "Cannot delete section root")
            return
        }
        if (!UiUtils.confirmAlert(stage, "Delete", "Delete \"${nodeRef.name}\" and all children?")) {
            return
        }
        val parentPath = nodeRef.path.parent
        val task = object : Task<Unit>() {
            override fun call() { NodeRepository.delete(nodeRef.path) }
            override fun succeeded() {
                refreshParentChildren(parentPath)
                currentNodeRef = null
                currentMeta = null
                currentComponent = null
                clearAllPanes()
                isDirty = false
                updateFormState()
                statusLabel.text = "Deleted: ${nodeRef.name}"
            }
            override fun failed() {
                UiUtils.errorAlert(stage, "Delete Error", exception?.message ?: "Unknown")
            }
        }
        executor.submit(task)
    }

    private fun refreshParentChildren(parentDir: Path) {
        val task = object : Task<List<NodeRef>>() {
            override fun call(): List<NodeRef> = NodeRepository.listChildren(parentDir)
            override fun succeeded() {
                val childRefs = value
                javafx.application.Platform.runLater {
                    val parentItem = findTreeItemByPath(treeView.root, parentDir)
                    parentItem?.let {
                        it.children.clear()
                        childRefs.forEach { child ->
                            it.children.add(createTreeItem(child, it))
                        }
                        it.isExpanded = true
                    }
                }
            }
            override fun failed() {
                log.error("Failed to refresh children of $parentDir", exception)
            }
        }
        executor.submit(task)
    }

    private fun findTreeItemByPath(item: javafx.scene.control.TreeItem<TreeItemData>?, targetPath: Path): javafx.scene.control.TreeItem<TreeItemData>? {
        item?.let {
            if (it.value?.nodeRef?.path == targetPath) return it
            it.children.forEach { child ->
                findTreeItemByPath(child, targetPath)?.let { result -> return result }
            }
        }
        return null
    }

    private fun saveChanges() {
        val meta = currentMeta ?: return
        val nodeRef = currentNodeRef ?: return
        val updatedMeta = meta.copy(name = nameField.text, description = descriptionArea.text)
        val task = object : Task<Unit>() {
            override fun call() { NodeRepository.save(nodeRef.path, updatedMeta) }
            override fun succeeded() {
                isDirty = false
                currentMeta = updatedMeta
                updateFormState()
                treeView.selectionModel.selectedItem?.let {
                    it.value = TreeItemData(updatedMeta.name.ifBlank { updatedMeta.slug }, nodeRef)
                }
                statusLabel.text = "Saved: ${updatedMeta.name}"
            }
            override fun failed() {
                UiUtils.errorAlert(stage, "Save Error", exception?.message ?: "Unknown")
            }
        }
        executor.submit(task)
    }

    private fun revertChanges() {
        val nodeRef = currentNodeRef ?: return
        val task = object : Task<NodeRef>() {
            override fun call(): NodeRef = NodeRepository.load(nodeRef.path)
                ?: throw IllegalStateException("Node not found: ${nodeRef.path}")
            override fun succeeded() {
                when (val content = value.content) {
                    is NodeContent.Container -> populateForm(content.meta)
                    is NodeContent.Leaf -> updateComponentPane(content.component)
                }
                isDirty = false
                updateFormState()
                statusLabel.text = "Reverted"
            }
            override fun failed() {
                UiUtils.errorAlert(stage, "Revert Error", exception?.message ?: "Unknown")
            }
        }
        executor.submit(task)
    }

    private fun refreshSection() {
        val selectedNodeRef = treeView.selectionModel.selectedItem?.value?.nodeRef
        val expandedPaths = mutableSetOf<Path>()
        collectExpandedPaths(treeView.root, expandedPaths)

        val task = object : Task<Pair<List<NodeRef>, NodeRef?>>() {
            override fun call(): Pair<List<NodeRef>, NodeRef?> {
                val tree = NodeRepository.loadTree(currentSectionDir)
                val selected = selectedNodeRef?.let { NodeRepository.load(it.path) }
                return Pair(tree, selected)
            }
            override fun succeeded() {
                val (tree, selected) = value
                buildTree(tree ?: emptyList())
                restoreExpanded(treeView.root, expandedPaths)
                selected?.let { nr ->
                    val idx = findNodeIndex(treeView.root, nr.path)
                    if (idx >= 0) {
                        treeView.selectionModel.select(idx)
                        when (val content = nr.content) {
                            is NodeContent.Leaf -> {
                                updateComponentPane(content.component)
                                statusLabel.text = "Component refreshed: ${content.component.name}"
                            }
                            is NodeContent.Container -> statusLabel.text = "Refreshed"
                        }
                    } else {
                        statusLabel.text = "Refreshed (selected node not found)"
                    }
                } ?: run { statusLabel.text = "Refreshed" }
            }
            override fun failed() {
                UiUtils.errorAlert(stage, "Refresh Error", exception?.message ?: "Unknown")
            }
        }
        executor.submit(task)
    }

    private fun collectExpandedPaths(item: javafx.scene.control.TreeItem<TreeItemData>?, paths: MutableSet<Path>) {
        item?.let {
            it.value?.nodeRef?.path?.let { p -> paths.add(p) }
            it.children.forEach { collectExpandedPaths(it, paths) }
        }
    }

    private fun restoreExpanded(item: javafx.scene.control.TreeItem<TreeItemData>?, expandedPaths: Set<Path>) {
        item?.let {
            it.value?.nodeRef?.path?.let { p ->
                if (p in expandedPaths) it.isExpanded = true
            }
            it.children.forEach { restoreExpanded(it, expandedPaths) }
        }
    }

    private fun findNodeIndex(item: javafx.scene.control.TreeItem<TreeItemData>?, targetPath: Path): Int {
        val queue = java.util.ArrayDeque<javafx.scene.control.TreeItem<TreeItemData>>()
        item?.let { queue.add(it) }
        var idx = 0
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.value?.nodeRef?.path == targetPath) return idx
            current.children.forEach { queue.add(it) }
            idx++
        }
        return -1
    }

    private data class TreeItemData(val displayName: String, val nodeRef: NodeRef?) {
        override fun toString(): String = displayName
    }

    private fun initPortColumns() {
        portNumberCol.setCellValueFactory(PropertyValueFactory("number"))
        portNumberCol.prefWidth = 50.0
        portPinNumberCol.setCellValueFactory(PropertyValueFactory("pinNumber"))
        portPinNumberCol.prefWidth = 80.0
        portNameCol.setCellValueFactory(PropertyValueFactory("name"))
        portNameCol.prefWidth = 150.0
        portDescriptionCol.setCellValueFactory(PropertyValueFactory("description"))
        portDescriptionCol.prefWidth = 250.0
        portTableView.columns.setAll(portNumberCol, portPinNumberCol, portNameCol, portDescriptionCol)
        portTableView.items = currentPortData
        portTableView.isEditable = false
    }

    private fun createComponentPane(): ScrollPane {
        val titleBox = HBox(10.0).apply {
            children.addAll(
                Label("Name:"), componentNameField.apply { prefWidth = 200.0 },
                Label("  Type:"), componentTypeLabel,
                Label("  Slug:"), componentSlugLabel
            )
        }
        val formGrid = GridPane().apply {
            hgap = 10.0; vgap = 8.0; padding = Insets(10.0)
            add(Label("Description:"), 0, 0)
            add(componentDescriptionArea, 1, 0)
        }
        val portTablePane = VBox(5.0, Label("Ports:"), portTableView).apply {
            HBox.setHgrow(portTableView, Priority.ALWAYS)
            VBox.setVgrow(portTableView, Priority.ALWAYS)
            padding = Insets(0.0, 10.0, 10.0, 10.0)
        }
        val buttons = HBox(10.0, componentRefreshBtn).apply {
            alignment = Pos.CENTER_LEFT; padding = Insets(0.0, 10.0, 10.0, 10.0)
        }
        val componentPane = VBox(0.0, titleBox, formGrid, portTablePane, buttons, statusLabel).apply {
            HBox.setHgrow(componentDescriptionArea, Priority.ALWAYS)
            VBox.setVgrow(componentDescriptionArea, Priority.ALWAYS)
            style = "-fx-padding: 0;"
        }
        val resultScrollPane = ScrollPane(componentPane).apply {
            isFitToWidth = true
            VBox.setVgrow(this, Priority.ALWAYS)
        }
        componentRefreshBtn.setOnAction { refreshComponent() }
        return resultScrollPane
    }

    private fun updateComponentPane(component: ComponentNode) {
        componentNameField.text = component.name.ifBlank { currentNodeRef?.slug ?: "" }
        componentDescriptionArea.text = component.description
        componentTypeLabel.text = "COMPONENT"
        componentSlugLabel.text = currentNodeRef?.slug ?: "(auto-generated)"

        currentPortData.clear()
        val ports = component.ports.map { PortRow(it.number, it.pinNumber, it.name, it.description) }
        currentPortData.addAll(ports.sortedBy { it.number })
        portTableView.items = currentPortData
    }

    private fun refreshComponent() {
        val nodeRef = currentNodeRef ?: return
        val task = object : Task<NodeRef>() {
            override fun call(): NodeRef = NodeRepository.load(nodeRef.path)
                ?: throw IllegalStateException("Node not found: ${nodeRef.path}")
            override fun succeeded() {
                val loaded = value
                javafx.application.Platform.runLater {
                    if (loaded.content is NodeContent.Leaf) {
                        updateComponentPane((loaded.content as NodeContent.Leaf).component)
                        statusLabel.text = "Component refreshed: ${(loaded.content as NodeContent.Leaf).component.name}"
                    }
                }
            }
            override fun failed() {
                log.error("Failed to refresh component", exception)
                UiUtils.errorAlert(stage, "Refresh Error", exception?.message ?: "Unknown")
            }
        }
        executor.submit(task)
    }

    init {
        stage.setOnCloseRequest { executor.shutdownNow() }
        initPortColumns()

        val formGrid = GridPane().apply {
            hgap = 10.0; vgap = 8.0; padding = Insets(10.0)
            add(Label("Name:"), 0, 0); add(nameField, 1, 0)
            add(Label("Description:"), 0, 1); add(descriptionArea, 1, 1)
            add(Label("Type:"), 0, 2); add(typeLabel, 1, 2)
            add(Label("Slug:"), 0, 3); add(slugLabel, 1, 3)
        }
        saveBtn.setOnAction { saveChanges() }
        revertBtn.setOnAction { revertChanges() }
        val formButtons = HBox(10.0, saveBtn, revertBtn).apply {
            alignment = Pos.CENTER_LEFT; padding = Insets(0.0, 0.0, 10.0, 10.0)
        }
        genericPane = VBox(0.0, formGrid, formButtons, statusLabel).apply {
            HBox.setHgrow(descriptionArea, Priority.ALWAYS)
            VBox.setVgrow(descriptionArea, Priority.ALWAYS)
        }

        componentScrollPane = createComponentPane()

        val leftPane = Pane().apply {
            children.add(ScrollPane(treeView).apply { prefWidth = 300.0 })
        }
        val refreshBtn = Button("Refresh").apply { setOnAction { refreshSection() } }
        val closeBtn = Button("Close").apply { setOnAction { stage.close(); onClose?.invoke() } }
        val buttonBox = HBox(10.0, refreshBtn, closeBtn).apply {
            alignment = Pos.CENTER_RIGHT; padding = Insets(10.0)
        }

        splitPane = SplitPane()
        splitPane.getItems().addAll(leftPane, genericPane)
        splitPane.getDividerPositions().set(0, 0.35)
        stage.scene = Scene(VBox(0.0, splitPane, buttonBox), 900.0, 600.0)
    }
}
