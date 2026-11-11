package ru.nodebook.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.UUID

@Serializable
data class NodeMeta(
    @SerialName("schemaVersion")
    val schemaVersion: Int = 1,
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerialName("slug")
    val slug: String = "",
    @SerialName("type")
    val type: NodeType = NodeType.UNKNOWN,
    @SerialName("name")
    val name: String = "",
    @SerialName("description")
    val description: String = "",
    @SerialName("properties")
    val properties: JsonObject = JsonObject(emptyMap()),
    @SerialName("links")
    val links: List<NodeLink> = emptyList(),
    @SerialName("childrenOrder")
    val childrenOrder: List<String> = emptyList()
)
