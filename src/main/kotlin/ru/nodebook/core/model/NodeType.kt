package ru.nodebook.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class NodeType {
    SECTION,
    MANUFACTURER,
    CATEGORY,
    COMPONENT,
    MODULE,
    STANDARD,
    UNKNOWN
}
