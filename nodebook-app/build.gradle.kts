plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    alias(libs.plugins.openjfx)
}

dependencies {
    implementation(project(":nodebook-core"))
    implementation(project(":nodebook-ui"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

javafx {
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("ru.nodebook.app.MainApp")
}
