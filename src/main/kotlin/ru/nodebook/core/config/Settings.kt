package ru.nodebook.core.config

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val workspace: String? = null
)
