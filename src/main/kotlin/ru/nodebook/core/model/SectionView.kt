package ru.nodebook.core.model

import kotlinx.serialization.Serializable

/**
 * Шаблон отображения раздела/категории.
 * Описывает, как показывать содержимое папки.
 */
@Serializable
data class SectionView(
    val schemaVersion: Int = 1,
    val kind: String = "tree",
    val title: String = "",
    val description: String = "",
    val icon: String? = null,

    // Настройки списка (таблица дочерних элементов)
    val listMode: String = "tree", // tree, table, cards
    val listColumns: List<String> = emptyList(), // ключи из mainProperties для колонок
    val showUnits: Boolean = true, // показывать единицы в значениях
    val sortBy: String = "name",
    val groupBy: String? = null,

    // Настройки карточки (выбранный элемент)
    val cardBlocks: List<String> = listOf("header", "mainProperties", "ports", "extraProperties"),
    val portColumns: List<String> = listOf("number", "pinNumber", "name", "description"),

    // Справочники единиц (для редактирования)
    val unitDictionaries: Map<String, List<String>> = emptyMap()
) {
    /**
     * Человекочитаемое название раздела для кнопок.
     */
    fun displayName(sectionName: String): String =
        if (title.isNotBlank()) title else sectionName.replaceFirstChar { it.uppercaseChar() }

    /**
     * Получить описание раздела.
     */
    fun displayDescription(): String = description.ifBlank { "" }

    /**
     * Получить иконку раздела.
     */
    fun displayIcon(): String? = icon
}
