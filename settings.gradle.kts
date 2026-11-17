pluginManagement {
    val quarkusPluginVersion: String by settings
    val kotlinVersion: String by settings
    val openApiGeneratorVersion: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusPluginVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        id("org.openapi.generator") version openApiGeneratorVersion
    }
}
rootProject.name = "onboarding-bff"
