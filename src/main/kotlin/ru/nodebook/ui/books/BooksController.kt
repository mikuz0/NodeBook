package ru.nodebook.ui.books

import javafx.concurrent.Task
import javafx.stage.FileChooser
import ru.nodebook.core.config.Settings
import ru.nodebook.core.config.SettingsStore
import ru.nodebook.core.model.KnowledgeBaseInfo
import ru.nodebook.core.repo.KnowledgeBaseRepository
import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

class BooksController {

    private val log = LoggerFactory.getLogger(BooksController::class.java)
    private val executor = java.util.concurrent.Executors.newFixedThreadPool(2)

    var selectedBook: KnowledgeBaseInfo? = null
        private set

    private lateinit var currentWorkspace: Path

    private lateinit var view: BooksView

    fun bindView(v: BooksView) {
        view = v
    }

    fun chooseWorkspace() {
        val chooser = FileChooser().apply {
            title = "Select Workspace Directory"
            initialDirectory = java.nio.file.Paths.get(System.getProperty("user.home")).toFile()
        }
        val dir = chooser.showOpenDialog(null) ?: return
        setWorkspace(Paths.get(dir.path))
    }

    fun setWorkspace(path: Path) {
        currentWorkspace = path
        view.setWorkspacePath(path.toString())
        view.setStatus("Loading books...")
        view.setSectionButtonsEnabled(false)

        // Save settings
        SettingsStore.save(Settings(workspace = path.toString()))

        // Load books in background
        loadBooksInBackground(path)
    }

    private fun loadBooksInBackground(path: Path) {
        val task = object : Task<List<KnowledgeBaseInfo>>() {
            override fun call(): List<KnowledgeBaseInfo> {
                return KnowledgeBaseRepository.scan(path)
            }

            override fun succeeded() {
                val books = value
                view.updateBooksList(books)
                view.setStatus("${books.size} knowledge base(s) found")
                if (books.isNotEmpty()) {
                    selectedBook = books[0]
                    view.setSectionButtonsEnabled(true)
                } else {
                    selectedBook = null
                    view.setSectionButtonsEnabled(false)
                }
            }

            override fun failed() {
                view.setStatus("Error loading books: ${exception?.message}")
                log.error("Failed to load books", exception)
            }
        }
        executor.submit(task)
    }

    fun selectBook(book: KnowledgeBaseInfo) {
        selectedBook = book
        view.setSectionButtonsEnabled(true)
        view.setStatus("Selected: ${book.name}")
    }

    fun createBook(name: String) {
        val bookName = name.trim()
        if (bookName.isEmpty()) {
            view.setStatus("Error: Book name cannot be empty")
            return
        }

        if (!::currentWorkspace.isInitialized) {
            view.setStatus("Error: No workspace selected")
            return
        }

        val task = object : Task<Path>() {
            override fun call(): Path {
                return KnowledgeBaseRepository.create(bookName, currentWorkspace)
            }

            override fun succeeded() {
                val bookDir = value
                view.setStatus("Created book: $bookName")
                // Reload books
                loadBooksInBackground(currentWorkspace)
            }

            override fun failed() {
                view.setStatus("Error creating book: ${exception?.message}")
                log.error("Failed to create book", exception)
            }
        }
        executor.submit(task)
    }
}
