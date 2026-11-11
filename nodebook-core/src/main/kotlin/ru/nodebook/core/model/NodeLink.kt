package ru.nodebook.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NodeLink(
    @SerialName("relation")
    val relation: String = "",
    @SerialName("targetSlug")
    val targetSlug: String = ""
)
