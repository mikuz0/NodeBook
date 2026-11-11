package ru.nodebook.ui

import javafx.stage.Stage
import java.nio.file.Path

interface BookAware {
    fun showWindow()
    fun openSection(bookPath: Path, section: String)
    fun getStage(): Stage?
}
