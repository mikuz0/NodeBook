package ru.nodebook.core.samples

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Порт компонента (для примеров).
 */
data class PortSample(
    val number: Int,
    val pinNumber: String,
    val name: String,
    val description: String
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("number", number)
        put("pinNumber", pinNumber)
        put("name", name)
        put("description", description)
    }
}

/**
 * Пример листа-компонента.
 */
data class ComponentSample(
    val slug: String,
    val name: String,
    val description: String,
    val model3dPath: String?,
    val ports: List<PortSample>
) {
    fun toProperties(): JsonObject = buildJsonObject {
        model3dPath?.let { put("model3dPath", it) } ?: put("model3dPath", JsonNull)
        if (ports.isNotEmpty()) {
            put("ports", JsonArray(ports.map(PortSample::toJson)))
        }
    }
}

/**
 * Примеры компонентов для начальной структуры книги.
 * Данные основаны на реальных файлах из components/ директории.
 */
object ComponentSamples {

    fun all(): List<ComponentSample> = listOf(
        resistor(),
        capacitor(),
        atmega328p(),
        stm32f103c8t6(),
        phy6222qcW04(),
        cht8310(),
        cht8320()
    )

    /**
     * Резистор — Generic resistor, 0603 package.
     */
    private fun resistor() = ComponentSample(
        slug = "resistor",
        name = "Resistor",
        description = "Generic resistor, 0603 package",
        model3dPath = null,
        ports = listOf(
            PortSample(1, "1", "Pin1", "Terminal 1"),
            PortSample(2, "2", "Pin2", "Terminal 2")
        )
    )

    /**
     * Конденсатор — Generic ceramic capacitor, 0603 package.
     */
    private fun capacitor() = ComponentSample(
        slug = "capacitor",
        name = "Capacitor",
        description = "Generic ceramic capacitor, 0603 package",
        model3dPath = null,
        ports = listOf(
            PortSample(1, "1", "Pin1", "Terminal 1"),
            PortSample(2, "2", "Pin2", "Terminal 2")
        )
    )

    /**
     * ATmega328P — 8-bit AVR MCU, 32KB Flash, DIP-28.
     * На основе реального файла atmega328p.cfcomp.
     */
    private fun atmega328p() = ComponentSample(
        slug = "atmega328p",
        name = "ATmega328P",
        description = "8-bit AVR MCU, 32KB Flash, DIP-28",
        model3dPath = null,
        ports = listOf(
            PortSample(1, "1", "PC6/RESET", "Reset"),
            PortSample(2, "2", "PD0/RXD", "UART RX"),
            PortSample(3, "3", "PD1/TXD", "UART TX"),
            PortSample(4, "4", "PD2/INT0", "External interrupt 0"),
            PortSample(5, "5", "PD3/INT1", "External interrupt 1"),
            PortSample(6, "6", "PD4", "GPIO"),
            PortSample(7, "7", "VCC", "Power supply"),
            PortSample(8, "8", "GND", "Ground"),
            PortSample(9, "9", "PB6/XTAL1", "Crystal oscillator"),
            PortSample(10, "10", "PB7/XTAL2", "Crystal oscillator"),
            PortSample(11, "11", "PD5", "GPIO / PWM"),
            PortSample(12, "12", "PD6", "GPIO / PWM"),
            PortSample(13, "13", "PD7", "GPIO"),
            PortSample(14, "14", "PB0", "GPIO / ICP1"),
            PortSample(15, "15", "PB1", "GPIO / OC1A"),
            PortSample(16, "16", "PB2", "GPIO / SS"),
            PortSample(17, "17", "PB3/MOSI", "SPI MOSI"),
            PortSample(18, "18", "PB4/MISO", "SPI MISO"),
            PortSample(19, "19", "PB5/SCK", "SPI clock"),
            PortSample(20, "20", "AVCC", "Analog supply"),
            PortSample(21, "21", "AREF", "Analog reference"),
            PortSample(22, "22", "GND", "Ground"),
            PortSample(23, "23", "PC0/ADC0", "Analog input 0"),
            PortSample(24, "24", "PC1/ADC1", "Analog input 1"),
            PortSample(25, "25", "PC2/ADC2", "Analog input 2"),
            PortSample(26, "26", "PC3/ADC3", "Analog input 3"),
            PortSample(27, "27", "PC4/SDA", "I2C data"),
            PortSample(28, "28", "PC5/SCL", "I2C clock")
        )
    )

    /**
     * STM32F103C8T6 — ARM Cortex-M3 MCU, 64KB Flash, LQFP-48.
     * На основе реального файла stm32f103c8t6.cfcomp.
     */
    private fun stm32f103c8t6() = ComponentSample(
        slug = "stm32f103c8t6",
        name = "STM32F103C8T6",
        description = "ARM Cortex-M3 MCU, 64KB Flash, 20KB RAM, LQFP-48",
        model3dPath = null,
        ports = listOf(
            PortSample(1, "1", "VBAT", "Battery supply"),
            PortSample(2, "2", "PC13", "GPIO / TAMPER-RTC"),
            PortSample(3, "3", "PC14", "GPIO / OSC32_IN"),
            PortSample(4, "4", "PC15", "GPIO / OSC32_OUT"),
            PortSample(5, "5", "PD0", "GPIO / OSC_IN"),
            PortSample(6, "6", "PD1", "GPIO / OSC_OUT"),
            PortSample(7, "7", "NRST", "Reset"),
            PortSample(8, "8", "VSSA", "Analog ground"),
            PortSample(9, "9", "VDDA", "Analog supply"),
            PortSample(10, "10", "PA0", "GPIO / ADC0 / TIM2_CH1"),
            PortSample(11, "11", "PA1", "GPIO / ADC1 / TIM2_CH2"),
            PortSample(12, "12", "PA2", "GPIO / USART2_TX"),
            PortSample(13, "13", "PA3", "GPIO / USART2_RX"),
            PortSample(14, "14", "PA4", "GPIO / SPI1_NSS"),
            PortSample(15, "15", "PA5", "GPIO / SPI1_SCK"),
            PortSample(16, "16", "PA6", "GPIO / SPI1_MISO"),
            PortSample(17, "17", "PA7", "GPIO / SPI1_MOSI"),
            PortSample(18, "18", "PB0", "GPIO / ADC8"),
            PortSample(19, "19", "PB1", "GPIO / ADC9"),
            PortSample(20, "20", "PB2", "GPIO / BOOT1"),
            PortSample(21, "21", "PB10", "GPIO / I2C2_SCL / USART3_TX"),
            PortSample(22, "22", "PB11", "GPIO / I2C2_SDA / USART3_RX"),
            PortSample(23, "23", "VSS", "Ground"),
            PortSample(24, "24", "VDD", "Power supply"),
            PortSample(25, "25", "PB12", "GPIO / SPI2_NSS"),
            PortSample(26, "26", "PB13", "GPIO / SPI2_SCK"),
            PortSample(27, "27", "PB14", "GPIO / SPI2_MISO"),
            PortSample(28, "28", "PB15", "GPIO / SPI2_MOSI"),
            PortSample(29, "29", "PA8", "GPIO / TIM1_CH1"),
            PortSample(30, "30", "PA9", "GPIO / USART1_TX"),
            PortSample(31, "31", "PA10", "GPIO / USART1_RX"),
            PortSample(32, "32", "PA11", "GPIO / USB_DM"),
            PortSample(33, "33", "PA12", "GPIO / USB_DP"),
            PortSample(34, "34", "PA13", "GPIO / SWDIO"),
            PortSample(35, "35", "VSS", "Ground"),
            PortSample(36, "36", "VDD", "Power supply"),
            PortSample(37, "37", "PA14", "GPIO / SWCLK"),
            PortSample(38, "38", "PA15", "GPIO / JTDI"),
            PortSample(39, "39", "PB3", "GPIO / JTDO"),
            PortSample(40, "40", "PB4", "GPIO / JNTRST"),
            PortSample(41, "41", "PB5", "GPIO"),
            PortSample(42, "42", "PB6", "GPIO / I2C1_SCL"),
            PortSample(43, "43", "PB7", "GPIO / I2C1_SDA"),
            PortSample(44, "44", "BOOT0", "Boot mode select"),
            PortSample(45, "45", "PB8", "GPIO"),
            PortSample(46, "46", "PB9", "GPIO"),
            PortSample(47, "47", "VSS", "Ground"),
            PortSample(48, "48", "VDD", "Power supply")
        )
    )

    /**
     * PHY6222QC-W04 — BLE 5.2 SoC, ARM Cortex-M0, QFN32.
     * На основе реального файла phy6222qc-w04.cfcomp.
     */
    private fun phy6222qcW04() = ComponentSample(
        slug = "phy6222qc-w04",
        name = "PHY6222QC-W04",
        description = "BLE 5.2 SoC, ARM Cortex-M0, QFN32. Pins per TuyaOS reference.",
        model3dPath = null,
        ports = listOf(
            PortSample(1, "P0", "UART0_RX", "UART0 receive (log)"),
            PortSample(2, "P2", "UART0_TX", "UART0 transmit (log)"),
            PortSample(3, "P9", "UART1_TX", "UART1 transmit (flash/debug)"),
            PortSample(4, "P10", "UART1_RX", "UART1 receive (flash/debug)"),
            PortSample(5, "P23", "I2C0_SDA", "I2C0 data"),
            PortSample(6, "P24", "I2C0_SCL", "I2C0 clock"),
            PortSample(7, "P31", "SPI0_CLK", "SPI0 clock"),
            PortSample(8, "P32", "SPI0_CS", "SPI0 chip select"),
            PortSample(9, "P18", "SPI0_SDI", "SPI0 data in"),
            PortSample(10, "P20", "SPI0_SDO", "SPI0 data out"),
            PortSample(11, "P1", "BOOT_DET", "Power-on detection"),
            PortSample(12, "P7", "KEY", "Wake-up / factory reset button")
        )
    )

    /**
     * CHT8310 — Digital temperature and humidity sensor, I2C.
     * На основе реального файла cht8310.cfcomp.
     */
    private fun cht8310() = ComponentSample(
        slug = "cht8310",
        name = "CHT8310",
        description = "Digital temperature and humidity sensor, I2C, DFN-3x3-6",
        model3dPath = null,
        ports = listOf(
            PortSample(1, "1", "SDA", "I2C data"),
            PortSample(2, "2", "GND", "Ground"),
            PortSample(3, "3", "ALERT", "Alert output (open-drain)"),
            PortSample(4, "4", "AD0", "I2C address select"),
            PortSample(5, "5", "VCC", "Power supply"),
            PortSample(6, "6", "SCL", "I2C clock")
        )
    )

    /**
     * CHT8320 — Digital temperature and humidity sensor, I2C.
     * На основе реального файла cht8320.cfcomp.
     */
    private fun cht8320() = ComponentSample(
        slug = "cht8320",
        name = "CHT8320",
        description = "Digital temperature and humidity sensor, I2C, DFN-3x3-6",
        model3dPath = null,
        ports = listOf(
            PortSample(1, "1", "SDA", "I2C data"),
            PortSample(2, "2", "GND", "Ground"),
            PortSample(3, "3", "ALERT", "Alert output (open-drain)"),
            PortSample(4, "4", "AD0", "I2C address select"),
            PortSample(5, "5", "VCC", "Power supply"),
            PortSample(6, "6", "SCL", "I2C clock")
        )
    )
}
