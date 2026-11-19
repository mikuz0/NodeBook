package ru.nodebook.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ComponentPort(
    val number: Int,
    val pinNumber: String,
    val name: String,
    val description: String = ""
)
