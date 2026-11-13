package ru.nodebook.ui.books

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import ru.nodebook.core.model.KnowledgeBaseInfo

class BooksView(private val controller: BooksController) {

    val root: VBox = VBox(10.0).apply {
        padding = Insets(15.0)
        style = "-fx-padding: 15;"
    }

    var onChooseWorkspace: (() -> Unit)? = null
    var onOpenSection: ((String) -> Unit)? = null

    // Workspace section
    private val workspaceLabel = Label("Workspace:")
    private val workspaceField = TextField().apply {
        isEditable = false
        prefWidth = 400.0
    }
    private val chooseWorkspaceBtn = Button("Choose workspace...")
    private val workspaceBox = HBox(10.0, workspaceField, chooseWorkspaceBtn).apply {
        alignment = Pos.CENTER_LEFT
    }

    // New knowledge base section
    private val newBookLabel = Label("New knowledge base:")
    private val newBookField = TextField().apply {
        promptText = "Book name"
        prefWidth = 200.0
    }
    private val createBtn = Button("Create")
    private val newBookBox = HBox(10.0, newBookField, createBtn).apply {
        alignment = Pos.CENTER_LEFT
    }

    // Status label
    val statusLabel = Label().apply {
        style = "-fx-text-fill: gray;"
    }

    // Section buttons (динамические)
    private val sectionBtnBox = FlowPane().apply {
        hgap = 10.0
        vgap = 10.0
        alignment = Pos.CENTER_LEFT
    }
    private var currentSectionBtns: MutableList<Button> = mutableListOf()

    // Books list
    private val booksList = ListView<KnowledgeBaseInfo>()

    init {
        // Wire button actions
        chooseWorkspaceBtn.setOnAction { onChooseWorkspace?.invoke() }
        createBtn.setOnAction { controller.createBook(newBookField.text) }

        // Cell factory for books list
        booksList.cellFactory = javafx.util.Callback {
            object : ListCell<KnowledgeBaseInfo>() {
                override fun updateItem(item: KnowledgeBaseInfo?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.name
                    graphic = null
                }
            }
        }

        // Single-click selection
        booksList.selectionModel.selectedItemProperty().addListener(
            ChangeListener { _: ObservableValue<out KnowledgeBaseInfo?>, _: KnowledgeBaseInfo?, newSel: KnowledgeBaseInfo? ->
                if (newSel != null) controller.selectBook(newSel)
                else controller.clearSelection()
            }
        )

        // Build layout
        val topSection = VBox(8.0, workspaceLabel, workspaceBox, newBookLabel, newBookBox).apply {
            padding = Insets(0.0, 0.0, 10.0, 0.0)
        }

        val middleSection = VBox(8.0, Label("Sections:"), sectionBtnBox, statusLabel).apply {
            padding = Insets(5.0, 0.0, 5.0, 0.0)
        }

        val listLabel = Label("Knowledge Bases:")
        val listScrollPane = ScrollPane(booksList).apply {
            prefHeight = 300.0
            prefWidth = 700.0
        }

        root.children.setAll(
            topSection,
            middleSection,
            listLabel,
            listScrollPane
        )
    }

    fun updateBooksList(books: List<KnowledgeBaseInfo>) {
        val selectedPath = booksList.selectionModel.selectedItem?.path
        booksList.items.setAll(books)
        if (selectedPath != null) {
            val idx = booksList.items.indexOfFirst { it.path == selectedPath }
            if (idx >= 0) booksList.selectionModel.select(idx)
        }
    }

    fun setWorkspacePath(path: String?) {
        workspaceField.text = path ?: ""
    }

    fun setStatus(msg: String) {
        statusLabel.text = msg
    }

    /**
     * Обновить кнопки разделов динамически на основе view.json.
     */
    fun updateSectionButtons(bookPath: java.nio.file.Path, sections: List<String>) {
        sectionBtnBox.children.clear()
        currentSectionBtns.clear()

        sections.forEach { sectionName ->
            val sectionDir = bookPath.resolve(sectionName)
            if (ru.nodebook.core.repo.KnowledgeBaseRepository.hasSectionView(sectionDir)) {
                val view = ru.nodebook.core.repo.KnowledgeBaseRepository.loadSectionView(sectionDir)
                val title = view?.displayName(sectionName) ?: sectionName.replaceFirstChar { it.uppercaseChar() }
                val btn = Button(title).apply {
                    setOnAction { onOpenSection?.invoke(sectionName) }
                }
                sectionBtnBox.children.add(btn)
                currentSectionBtns.add(btn)
            }
        }

        if (sectionBtnBox.children.isEmpty()) {
            statusLabel.text = "No sections available"
        } else {
            statusLabel.text = "${sectionBtnBox.children.size} section(s) available"
        }
    }

    fun setSectionButtonsEnabled(enabled: Boolean) {
        currentSectionBtns.forEach { it.isDisable = !enabled }
    }
}
