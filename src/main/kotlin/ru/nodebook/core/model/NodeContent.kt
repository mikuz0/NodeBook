package ru.nodebook.core.model

/**
 * Содержимое узла: либо контейнер (NodeMeta), либо лист-компонент (ComponentNode).
 */
sealed class NodeContent {
    data class Container(val meta: NodeMeta) : NodeContent()
    data class Leaf(val component: ComponentNode) : NodeContent()
}
