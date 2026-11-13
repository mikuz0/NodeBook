package ru.nodebook.ui.section

import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/**
 * Утилита для чтения/записи свойств компонента (ports, model3dPath).
 * Сейчас только чтение — запись в следующем шаге.
 */
object ComponentProperties {

    private val log = LoggerFactory.getLogger(ComponentProperties::class.java)

    /**
     * Прочитать список портов из NodeMeta.properties.
     * Возвращает пустой список, если ключ отсутствует или некорректен.
     */
    fun readPorts(properties: JsonObject): List<PortRow> {
        val portsJson = properties["ports"] ?: return emptyList()

        if (portsJson !is JsonArray) {
            log.warn("properties['ports'] is not a JsonArray, got: ${portsJson::class.java.simpleName}")
            return emptyList()
        }

        return portsJson.mapIndexedNotNull { index, jsonElement ->
            try {
                if (jsonElement !is JsonObject) {
                    log.warn("properties['ports'][$index] is not an object, skipping")
                    return@mapIndexedNotNull null
                }

                val number = jsonElement["number"]?.let {
                    when (it) {
                        is JsonPrimitive -> it.int
                        else -> null
                    }
                } ?: run {
                    log.warn("properties['ports'][$index].number is missing or invalid")
                    return@mapIndexedNotNull null
                }

                val pinNumber = jsonElement["pinNumber"]?.toString() ?: ""
                val name = jsonElement["name"]?.toString() ?: ""
                val description = jsonElement["description"]?.toString() ?: ""

                PortRow(number, pinNumber, name, description)
            } catch (e: Exception) {
                log.warn("Failed to read port at index $index: ${e.message}")
                null
            }
        }
    }

    /**
     * Прочитать model3dPath из NodeMeta.properties.
     * Возвращает null, если ключ отсутствует или равен JsonNull.
     */
    fun readModel3dPath(properties: JsonObject): String? {
        val model3dPath = properties["model3dPath"] ?: return null
        return when (model3dPath) {
            is JsonNull -> null
            is JsonPrimitive -> model3dPath.contentOrNull
            else -> null
        }
    }
}
