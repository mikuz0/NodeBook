package ru.nodebook.ui.section

/**
 * Строка порта компонента для UI.
 */
data class PortRow(
    val number: Int,
    val pinNumber: String,
    val name: String,
    val description: String
) {
    override fun toString(): String = "PortRow(number=$number, pinNumber='$pinNumber', name='$name', description='$description')"
}
