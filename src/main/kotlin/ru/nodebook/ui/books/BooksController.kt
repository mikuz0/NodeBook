package ru.nodebook.ui.books

import javafx.concurrent.Task
import ru.nodebook.core.config.Settings
import ru.nodebook.core.config.SettingsStore
import ru.nodebook.core.model.KnowledgeBaseInfo
import ru.nodebook.core.repo.KnowledgeBaseRepository
import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.LoggerFactory

class BooksController {

    private val log = LoggerFactory.getLogger(BooksController::class.java)
    private val executor = java.util.concurrent.Executors.newFixedThreadPool(2) { r ->
        Thread(r, "books-io").apply { isDaemon = true }
    }

    var selectedBook: KnowledgeBaseInfo? = null
        private set

    private lateinit var currentWorkspace: Path

    private lateinit var view: BooksView

    fun bindView(v: BooksView) {
        view = v
    }

    fun setWorkspace(path: Path) {
        if (!Files.isDirectory(path)) {
            view.setStatus("Error: not a directory: $path")
            return
        }
        if (!Files.isReadable(path) || !Files.isWritable(path)) {
            view.setStatus("Error: no read/write access: $path")
            return
        }

        currentWorkspace = path
        SettingsStore.save(Settings(workspace = path.toString()))
        view.updateBooksList(emptyList())
        selectedBook = null
        view.setSectionButtonsEnabled(false)
        view.setWorkspacePath(path.toString())
        view.setStatus("Loading books...")
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
                if (books.isEmpty()) {
                    view.setStatus("No knowledge bases in workspace: $path")
                } else {
                    view.setStatus("${books.size} knowledge base(s) found")
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

    fun clearSelection() {
        selectedBook = null
        view.setSectionButtonsEnabled(false)
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

    fun shutdown() {
        log.info("BooksController shut down")
        executor.shutdownNow()
    }
}
