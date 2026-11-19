package ru.nodebook.core.samples

import ru.nodebook.core.model.ComponentNode
import ru.nodebook.core.model.ComponentPort
import ru.nodebook.core.model.PropertyValue

object ComponentSamples {
    fun all(): List<ComponentNode> = listOf(
        resistor(), capacitor(), atmega328p(), stm32f103c8t6(), phy6222qcW04(), cht8310(), cht8320()
    )

    private fun resistor() = ComponentNode(
        name = "Resistor", description = "Generic resistor, 0603 package", refPrefix = "R",
        ports = listOf(ComponentPort(1, "1", "Pin1", "Terminal 1"), ComponentPort(2, "2", "Pin2", "Terminal 2")),
        mainProperties = listOf(
            PropertyValue("Номинал", "10", "кОм"), PropertyValue("Мощность", "0.1", "Вт"),
            PropertyValue("Допуск", "±1", "%"), PropertyValue("Корпус", "0603")
        ),
        extraProperties = listOf(
            PropertyValue("Температурный диапазон", "-55..+125", "°C"), PropertyValue("ТКР", "±100", "ppm/°C")
        )
    )

    private fun capacitor() = ComponentNode(
        name = "Capacitor", description = "Generic ceramic capacitor, 0603 package", refPrefix = "C",
        ports = listOf(ComponentPort(1, "1", "Pin1", "Terminal 1"), ComponentPort(2, "2", "Pin2", "Terminal 2")),
        mainProperties = listOf(
            PropertyValue("Ёмкость", "100", "нФ"), PropertyValue("Корпус", "0603"),
            PropertyValue("Напряжение", "50", "В"), PropertyValue("Диэлектрик", "X7R")
        ),
        extraProperties = listOf(
            PropertyValue("Допуск", "±10", "%"), PropertyValue("Температурный диапазон", "-55..+125", "°C"), PropertyValue("ТКЕ", "±15", "%/°C")
        )
    )

    private fun atmega328p() = ComponentNode(
        name = "ATmega328P", description = "8-bit AVR MCU, 32KB Flash, DIP-28", refPrefix = "DD",
        ports = listOf(
            ComponentPort(1, "1", "PC6/RESET", "Reset"), ComponentPort(7, "7", "VCC", "Power supply"),
            ComponentPort(8, "8", "GND", "Ground"), ComponentPort(27, "27", "PC4/SDA", "I2C data"),
            ComponentPort(28, "28", "PC5/SCL", "I2C clock")
        ),
        mainProperties = listOf(
            PropertyValue("Ядро", "AVR 8-bit"), PropertyValue("Частота", "20", "МГц"),
            PropertyValue("Flash", "32", "KB"), PropertyValue("RAM", "2", "KB"), PropertyValue("Корпус", "DIP-28")
        ),
        extraProperties = listOf(
            PropertyValue("Напряжение питания", "1.8..5.5", "В"), PropertyValue("Температурный диапазон", "-40..+85", "°C"), PropertyValue("Производитель", "Microchip")
        )
    )

    private fun stm32f103c8t6() = ComponentNode(
        name = "STM32F103C8T6", description = "ARM Cortex-M3 MCU, 64KB Flash, 20KB RAM, LQFP-48", refPrefix = "DD",
        ports = listOf(
            ComponentPort(1, "1", "VBAT", "Battery supply"), ComponentPort(7, "7", "NRST", "Reset"),
            ComponentPort(23, "23", "VSS", "Ground"), ComponentPort(24, "24", "VDD", "Power supply")
        ),
        mainProperties = listOf(
            PropertyValue("Ядро", "ARM Cortex-M3"), PropertyValue("Частота", "72", "МГц"),
            PropertyValue("Flash", "64", "KB"), PropertyValue("RAM", "20", "KB"), PropertyValue("Корпус", "LQFP-48")
        ),
        extraProperties = listOf(
            PropertyValue("Напряжение питания", "2.0..3.6", "В"), PropertyValue("Температурный диапазон", "-40..+85", "°C"), PropertyValue("Производитель", "STMicroelectronics")
        )
    )

    private fun phy6222qcW04() = ComponentNode(
        name = "PHY6222QC-W04", description = "BLE 5.2 SoC, ARM Cortex-M0, QFN32", refPrefix = "DD",
        ports = listOf(
            ComponentPort(1, "P0", "UART0_RX", "UART0 receive"), ComponentPort(2, "P2", "UART0_TX", "UART0 transmit"),
            ComponentPort(5, "P23", "I2C0_SDA", "I2C0 data"), ComponentPort(6, "P24", "I2C0_SCL", "I2C0 clock")
        ),
        mainProperties = listOf(
            PropertyValue("Ядро", "ARM Cortex-M0"), PropertyValue("Частота", "48", "МГц"),
            PropertyValue("Flash", "256", "KB"), PropertyValue("RAM", "64", "KB"), PropertyValue("Корпус", "QFN32")
        ),
        extraProperties = listOf(
            PropertyValue("Напряжение питания", "1.7..3.6", "В"), PropertyValue("Температурный диапазон", "-40..+85", "°C"), PropertyValue("Производитель", "Puya")
        )
    )

    private fun cht8310() = ComponentNode(
        name = "CHT8310", description = "Digital temperature and humidity sensor, I2C, DFN-3x3-6", refPrefix = "U",
        ports = listOf(
            ComponentPort(1, "1", "SDA", "I2C data"), ComponentPort(2, "2", "GND", "Ground"),
            ComponentPort(5, "5", "VCC", "Power supply"), ComponentPort(6, "6", "SCL", "I2C clock")
        ),
        mainProperties = listOf(
            PropertyValue("Тип", "Температура + влажность"), PropertyValue("Интерфейс", "I2C"), PropertyValue("Корпус", "DFN-3x3-6")
        ),
        extraProperties = listOf(
            PropertyValue("Напряжение питания", "1.6..3.6", "В"), PropertyValue("Температурный диапазон", "-40..+125", "°C"), PropertyValue("Производитель", "Sensirion")
        )
    )

    private fun cht8320() = ComponentNode(
        name = "CHT8320", description = "Digital temperature and humidity sensor, I2C, DFN-3x3-6", refPrefix = "U",
        ports = listOf(
            ComponentPort(1, "1", "SDA", "I2C data"), ComponentPort(2, "2", "GND", "Ground"),
            ComponentPort(5, "5", "VCC", "Power supply"), ComponentPort(6, "6", "SCL", "I2C clock")
        ),
        mainProperties = listOf(
            PropertyValue("Тип", "Температура + влажность"), PropertyValue("Интерфейс", "I2C"), PropertyValue("Корпус", "DFN-3x3-6")
        ),
        extraProperties = listOf(
            PropertyValue("Напряжение питания", "1.6..3.6", "В"), PropertyValue("Температурный диапазон", "-40..+125", "°C"), PropertyValue("Производитель", "Sensirion")
        )
    )
}
