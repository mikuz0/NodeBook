package ru.nodebook.app

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import ru.nodebook.core.model.KnowledgeBaseInfo

class BooksView(private val controller: BooksController) {

    val root: VBox = VBox(10.0).apply {
        padding = Insets(15.0)
        style = "-fx-padding: 15;"
    }

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

    // Section buttons
    private val sectionNames = listOf("components", "modules", "standards")
    private val sectionBtns = sectionNames.map { section ->
        Button(section.replaceFirstChar { it.uppercaseChar() })
    }
    private val sectionBtnBox = FlowPane().apply {
        hgap = 10.0
        vgap = 10.0
        alignment = Pos.CENTER_LEFT
    }

    // Books list
    private val booksList = ListView<KnowledgeBaseInfo>()

    init {
        // Wire button actions
        chooseWorkspaceBtn.setOnAction { controller.chooseWorkspace() }
        createBtn.setOnAction { controller.createBook(newBookField.text) }
        sectionNames.forEachIndexed { i, section ->
            sectionBtns[i].setOnAction { onOpenSection?.invoke(section) }
        }
        sectionBtnBox.children.setAll(sectionBtns)

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

        // Double-click on list item
        booksList.setOnMouseClicked { event ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                val idx = booksList.getSelectionModel().selectedItemProperty().value
                if (idx != null) {
                    controller.selectBook(idx)
                }
            }
        }

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
        booksList.items.clear()
        booksList.items.setAll(books)
    }

    fun setWorkspacePath(path: String?) {
        workspaceField.text = path ?: ""
    }

    fun setStatus(msg: String) {
        statusLabel.text = msg
    }

    fun setSectionButtonsEnabled(enabled: Boolean) {
        sectionBtns.forEach { it.isDisable = !enabled }
    }
}
