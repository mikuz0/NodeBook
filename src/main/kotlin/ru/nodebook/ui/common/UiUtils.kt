package ru.nodebook.ui.common

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.text.Font
import javafx.stage.Stage

object UiUtils {

    fun errorAlert(owner: Stage?, title: String, message: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }
        alert.showAndWait()
    }

    fun confirmAlert(owner: Stage?, title: String, message: String): Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }
        val result = alert.showAndWait()
        return result == ButtonType.OK
    }

    fun infoAlert(owner: Stage?, title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }
        alert.showAndWait()
    }

    fun stylePathLabel(label: Label, path: String) {
        label.text = path
        label.font = Font.font(label.font.family, 12.0)
        label.style = "-fx-text-fill: #0066cc; -fx-underline: true;"
    }
}
