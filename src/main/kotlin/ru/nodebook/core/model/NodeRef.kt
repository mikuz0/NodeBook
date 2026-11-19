package ru.nodebook.core.model

import java.nio.file.Path

/**
 * Ссылка на узел: путь + содержимое + флаг контейнера.
 */
data class NodeRef(
    val path: Path,
    val content: NodeContent,
    val isContainer: Boolean
) {
    val name: String
        get() = when (content) {
            is NodeContent.Container -> content.meta.name
            is NodeContent.Leaf -> content.component.name
        }

    val slug: String
        get() = when (content) {
            is NodeContent.Container -> content.meta.slug
            is NodeContent.Leaf -> path.fileName.toString().removeSuffix(".node")
        }

    val type: NodeType
        get() = when (content) {
            is NodeContent.Container -> content.meta.type
            is NodeContent.Leaf -> NodeType.COMPONENT
        }
}
