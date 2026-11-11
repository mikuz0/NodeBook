package ru.nodebook.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Instant

@Serializable
data class KnowledgeBase(
    @SerialName("schemaVersion")
    val schemaVersion: Int = 1,
    @SerialName("name")
    val name: String = "",
    @SerialName("version")
    val version: Int = 1,
    @SerialName("sections")
    val sections: List<String> = listOf("components", "modules", "standards"),
    @SerialName("createdAt")
    val createdAt: String = Instant.now().toString(),
    @SerialName("updatedAt")
    val updatedAt: String = Instant.now().toString()
)

data class KnowledgeBaseInfo(
    val name: String,
    val path: Path
) : Comparable<KnowledgeBaseInfo> {
    override fun compareTo(other: KnowledgeBaseInfo): Int {
        return name.compareTo(other.name)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KnowledgeBaseInfo) return false
        return path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}
