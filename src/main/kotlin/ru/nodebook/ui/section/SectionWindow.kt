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
import kotlinx.serialization.json.Json
import ru.nodebook.core.model.*
import ru.nodebook.core.repo.NodeRepository
import java.nio.file.Path
import org.slf4j.LoggerFactory
import ru.nodebook.ui.common.UiUtils

class SectionWindow : BookAware {

    private val log = LoggerFactory.getLogger(SectionWindow::class.java)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val executor = java.util.concurrent.Executors.newFixedThreadPool(2) { r ->
        Thread(r, "section-io").apply { isDaemon = true }
    }

    private val stage = Stage()
    private val treeView = javafx.scene.control.TreeView<TreeItemData>()
    private lateinit var currentSectionDir: Path
    private lateinit var currentSectionName: String

    private val nameField = TextField()
    private val descriptionArea = TextArea().apply { prefRowCount = 4 }
    private val typeLabel = Label()
    private val slugLabel = Label()
    private val saveBtn = Button("Save")
    private val revertBtn = Button("Revert")
    private val statusLabel = Label().apply { style = "-fx-text-fill: gray; -fx-padding: 5 0 0 0;" }

    // Компонентная панель
    private val componentNameField = TextField().apply { isEditable = false }
    private val componentDescriptionArea = TextArea().apply { isEditable = false; prefRowCount = 3 }
    private val componentModel3dPathField = TextField().apply { isEditable = false }
    private val componentTypeLabel = Label()
    private val componentSlugLabel = Label()
    private val componentRefreshBtn = Button("Refresh Component")

    // Таблица портов
    private val portNumberCol = TableColumn<PortRow, Int>("#")
    private val portPinNumberCol = TableColumn<PortRow, String>("Pin#")
    private val portNameCol = TableColumn<PortRow, String>("Name")
    private val portDescriptionCol = TableColumn<PortRow, String>("Description")
    private val portTableView = TableView<PortRow>().apply {
        prefHeight = 200.0
    }

    var onClose: (() -> Unit)? = null
    private var isDirty = false
    private var currentMeta: NodeMeta? = null
    private var currentNodeRef: NodeRef? = null
    private var isReadOnly = false
    private var currentPortData: ObservableList<PortRow> = FXCollections.observableArrayList()

    // Панели
    private lateinit var genericPane: VBox
    private lateinit var componentPane: Region
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
        
        // Установить заголовок и показать окно сразу
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
        currentMeta?.let { currentMeta = it.copy() }
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
                        currentMeta = data.nodeRef!!.meta
                        isDirty = false

                        // Переключение панелей по типу узла
                        if (data.nodeRef!!.meta.type == NodeType.COMPONENT) {
                            showComponentPane(data.nodeRef!!)
                        } else {
                            showGenericPane(data.nodeRef!!)
                        }
                    } else {
                        currentNodeRef = null
                        currentMeta = null
                        clearAllPanes()
                    }
                }
            }
        )
    }

    private fun showComponentPane(nodeRef: NodeRef) {
        // Удаляем genericPane из SplitPane и добавляем componentScrollPane
        splitPane.getItems().remove(genericPane)
        splitPane.getItems().add(componentScrollPane)
        splitPane.getDividerPositions().set(0, 0.35)
        updateComponentPane(nodeRef.meta)
    }

    private fun showGenericPane(nodeRef: NodeRef) {
        // Удаляем componentScrollPane из SplitPane и добавляем genericPane
        splitPane.getItems().remove(componentScrollPane)
        splitPane.getItems().add(genericPane)
        splitPane.getDividerPositions().set(0, 0.35)
        populateForm(nodeRef.meta)
        updateFormState()
    }

    private fun clearAllPanes() {
        nameField.text = ""
        descriptionArea.text = ""
        typeLabel.text = ""
        slugLabel.text = ""
        componentNameField.text = ""
        componentDescriptionArea.text = ""
        componentModel3dPathField.text = ""
        componentTypeLabel.text = ""
        componentSlugLabel.text = ""
        currentPortData.clear()
    }

    private fun createTreeItem(nodeRef: NodeRef, parent: javafx.scene.control.TreeItem<TreeItemData>): javafx.scene.control.TreeItem<TreeItemData> {
        val displayName = nodeRef.meta.name.ifBlank { nodeRef.meta.slug }
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
                // Right-click: select the item under mouse and show context menu
                val selectedItem = treeView.selectionModel.selectedItem
                selectedItem?.let { 
                    contextMenu.show(treeView, event.screenX, event.screenY)
                }
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

        val nameField = TextField()
        nameField.promptText = "Name (required)"
        val descArea = TextArea()
        descArea.promptText = "Description (optional)"
        descArea.prefRowCount = 3

        dialog.dialogPane.content = VBox(10.0, nameField, descArea)
        dialog.setResultConverter(Callback<ButtonType, Pair<String, String>?> { button: ButtonType ->
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
            val meta = NodeMeta(type = expectedType, name = name, description = desc)

            val task = object : Task<Unit>() {
                override fun call() {
                    when (expectedType) {
                        NodeType.CATEGORY, NodeType.MANUFACTURER -> NodeRepository.createContainer(parentDir, slug, meta)
                        NodeType.COMPONENT -> NodeRepository.createLeaf(parentDir, slug, meta)
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
        if (nodeRef.meta.type == NodeType.SECTION) {
            UiUtils.infoAlert(stage, "Info", "Cannot delete section root")
            return
        }
        if (!UiUtils.confirmAlert(stage, "Delete", "Delete \"${nodeRef.meta.name}\" and all children?")) {
            return
        }

        val parentPath = nodeRef.path.parent
        val task = object : Task<Unit>() {
            override fun call() { NodeRepository.delete(nodeRef.path) }
            override fun succeeded() {
                refreshParentChildren(parentPath)
                currentNodeRef = null
                currentMeta = null
                nameField.text = ""
                descriptionArea.text = ""
                typeLabel.text = ""
                slugLabel.text = ""
                isDirty = false
                updateFormState()
                statusLabel.text = "Deleted: ${nodeRef.meta.name}"
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
            it.children.forEach { child: javafx.scene.control.TreeItem<TreeItemData> ->
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
                populateForm(value.meta)
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
                        // Обновляем панель компонента если нужно
                        if (nr.meta.type == NodeType.COMPONENT) {
                            javafx.application.Platform.runLater {
                                updateComponentPane(nr.meta)
                                statusLabel.text = "Component refreshed: ${nr.meta.name}"
                            }
                        } else {
                            statusLabel.text = "Refreshed"
                        }
                    } else {
                        statusLabel.text = "Refreshed (selected node not found)"
                    }
                } ?: run {
                    statusLabel.text = "Refreshed"
                }
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

    // Инициализация таблицы портов
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
        portNumberCol.isSortable = false
        portPinNumberCol.isSortable = false
        portNameCol.isSortable = false
        portDescriptionCol.isSortable = false
    }

    // Создание панели компонента
    private fun createComponentPane(): ScrollPane {
        // Заголовок
        val titleBox = HBox(10.0).apply {
            children.addAll(
                Label("Name:"),
                componentNameField.apply { prefWidth = 200.0 },
                Label("  Type:"),
                componentTypeLabel,
                Label("  Slug:"),
                componentSlugLabel
            )
        }

        // Поля
        val formGrid = GridPane().apply {
            hgap = 10.0
            vgap = 8.0
            padding = Insets(10.0)
            add(Label("Description:"), 0, 0)
            add(componentDescriptionArea, 1, 0)
            add(Label("Model 3D Path:"), 0, 1)
            add(componentModel3dPathField.apply { prefWidth = 200.0 }, 1, 1)
        }

        // Таблица портов
        val portTablePane = VBox(5.0, Label("Ports:"), portTableView).apply {
            HBox.setHgrow(portTableView, Priority.ALWAYS)
            VBox.setVgrow(portTableView, Priority.ALWAYS)
            padding = Insets(0.0, 10.0, 10.0, 10.0)
        }

        // Кнопки
        val buttons = HBox(10.0, componentRefreshBtn).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(0.0, 10.0, 10.0, 10.0)
        }

        // Основная панель
        val componentPane = VBox(0.0, titleBox, formGrid, portTablePane, buttons, statusLabel).apply {
            HBox.setHgrow(componentDescriptionArea, Priority.ALWAYS)
            VBox.setVgrow(componentDescriptionArea, Priority.ALWAYS)
            style = "-fx-padding: 0;"
        }

        // ScrollPane для компонентной панели
        val resultScrollPane = ScrollPane(componentPane).apply {
            isFitToWidth = true
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        componentRefreshBtn.setOnAction { refreshComponent() }

        return resultScrollPane
    }

    // Обновление панели компонента
    private fun updateComponentPane(meta: NodeMeta) {
        componentNameField.text = meta.name.ifBlank { meta.slug }
        componentDescriptionArea.text = meta.description
        componentTypeLabel.text = meta.type.name
        componentSlugLabel.text = meta.slug.ifBlank { "(auto-generated)" }

        val model3dPath = ComponentProperties.readModel3dPath(meta.properties)
        componentModel3dPathField.text = model3dPath ?: "—"

        // Загрузка портов
        currentPortData.clear()
        val ports = ComponentProperties.readPorts(meta.properties)
        currentPortData.addAll(ports)

        // Сортировка по number
        val sortedPorts = FXCollections.observableArrayList(currentPortData.sortedWith(compareBy { it.number }))
        portTableView.items = sortedPorts
    }

    // Перечитывание компонента
    private fun refreshComponent() {
        val nodeRef = currentNodeRef ?: return

        val task = object : Task<NodeRef>() {
            override fun call(): NodeRef = NodeRepository.load(nodeRef.path)
                ?: throw IllegalStateException("Node not found: ${nodeRef.path}")
            override fun succeeded() {
                val loaded = value
                javafx.application.Platform.runLater {
                    updateComponentPane(loaded.meta)
                    statusLabel.text = "Component refreshed: ${loaded.meta.name}"
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

        // Инициализация таблицы портов
        initPortColumns()

        // Generic панель (для SECTION/CATEGORY/MANUFACTURER)
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

        // Component панель
        componentScrollPane = createComponentPane()
        componentPane = componentScrollPane

        // Изначально показываем generic панель
        genericPane.isVisible = true
        genericPane.isManaged = true
        componentPane.isVisible = false
        componentPane.isManaged = false

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
