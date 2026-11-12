package ru.nodebook.ui

import javafx.application.Application
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import ru.nodebook.core.config.AppPaths
import ru.nodebook.core.config.SettingsStore
import ru.nodebook.ui.books.BooksController
import ru.nodebook.ui.books.BooksView
import ru.nodebook.ui.section.SectionWindow

class MainApp : Application() {

    private val sectionWindows = mutableListOf<SectionWindow>()
    private lateinit var controller: BooksController

    override fun start(primaryStage: Stage) {
        AppPaths.initConfigDir()
        val settings = SettingsStore.load()

        controller = BooksController()
        val view = BooksView(controller)
        controller.bindView(view)

        // If workspace is set and exists, set it
        settings.workspace?.let { ws ->
            val wsPath = java.nio.file.Paths.get(ws)
            if (java.nio.file.Files.isDirectory(wsPath)) {
                controller.setWorkspace(wsPath)
            }
        }

        primaryStage.title = "NodeBook — Knowledge Base Manager"
        primaryStage.minWidth = 700.0
        primaryStage.minHeight = 500.0
        primaryStage.scene = javafx.scene.Scene(view.root, 800.0, 600.0)
        primaryStage.show()

        // Wire callbacks
        view.onChooseWorkspace = { chooseWorkspace(primaryStage) }
        view.onOpenSection = { sectionName ->
            controller.selectedBook?.let { book ->
                val win = SectionWindow()
                sectionWindows.add(win)
                win.onClose = { sectionWindows.remove(win) }
                win.openSection(book.path, sectionName)
            }
        }
    }

    private fun chooseWorkspace(stage: Stage) {
        val chooser = DirectoryChooser()
        chooser.title = "Choose Workspace"
        val selectedDir = chooser.showDialog(stage)
        if (selectedDir != null) {
            controller.setWorkspace(selectedDir.toPath())
        }
    }

    override fun stop() {
        if (::controller.isInitialized) controller.shutdown()
        super.stop()
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
