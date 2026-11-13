package ru.nodebook.core.model

import kotlinx.serialization.Serializable

/**
 * Модель отображения раздела.
 * Создаётся в папке каждого раздела при создании книги.
 */
@Serializable
data class SectionView(
    val schemaVersion: Int = 1,
    val kind: String = "tree",
    val title: String = ""
) {
    /**
     * Человекочитаемое название раздела для кнопок.
     * Если title пустое — используется секция с заглавной буквы.
     */
    fun displayName(sectionName: String): String =
        if (title.isNotBlank()) title else sectionName.replaceFirstChar { it.uppercaseChar() }
}
