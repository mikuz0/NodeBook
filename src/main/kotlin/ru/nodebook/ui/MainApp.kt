package ru.nodebook.ui

import javafx.application.Application
import ru.nodebook.core.config.AppPaths
import ru.nodebook.core.config.SettingsStore
import ru.nodebook.ui.books.BooksController
import ru.nodebook.ui.books.BooksView
import ru.nodebook.ui.section.SectionWindow

class MainApp : Application() {

    private val sectionWindows = mutableListOf<SectionWindow>()

    override fun start(stage: javafx.stage.Stage) {
        AppPaths.initConfigDir()
        val settings = SettingsStore.load()

        val controller = BooksController()
        val view = BooksView(controller)
        controller.bindView(view)

        // If workspace is set and exists, set it
        settings.workspace?.let { ws ->
            val wsPath = java.nio.file.Paths.get(ws)
            if (java.nio.file.Files.isDirectory(wsPath)) {
                controller.setWorkspace(wsPath)
            }
        }

        val stage = javafx.stage.Stage()
        stage.title = "NodeBook — Knowledge Base Manager"
        stage.minWidth = 700.0
        stage.minHeight = 500.0
        stage.scene = javafx.scene.Scene(view.root, 800.0, 600.0)
        stage.show()

        // Wire section buttons to open SectionWindow
        view.onOpenSection = { sectionName ->
            controller.selectedBook?.let { book ->
                val win = SectionWindow()
                sectionWindows.add(win)
                win.onClose = { sectionWindows.remove(win) }
                win.openSection(book.path, sectionName)
            }
        }
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
