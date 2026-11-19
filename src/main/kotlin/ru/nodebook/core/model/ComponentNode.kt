package ru.nodebook.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ComponentNode(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val refPrefix: String = "",
    val ports: List<ComponentPort> = emptyList(),
    val mainProperties: List<PropertyValue> = emptyList(),
    val extraProperties: List<PropertyValue> = emptyList()
)
