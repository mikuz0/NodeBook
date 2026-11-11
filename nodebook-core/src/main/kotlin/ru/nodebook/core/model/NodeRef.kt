package ru.nodebook.core.model

import java.nio.file.Path

data class NodeRef(
    val path: Path,
    val meta: NodeMeta,
    val isContainer: Boolean
)
