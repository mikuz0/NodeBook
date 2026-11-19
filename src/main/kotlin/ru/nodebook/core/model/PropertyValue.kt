package ru.nodebook.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PropertyValue(
    val key: String,
    val value: String,
    val unit: String = ""
) {
    fun display(): String = if (unit.isNotBlank()) "$value $unit" else value
}
