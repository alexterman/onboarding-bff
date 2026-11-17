plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
    id("org.openapi.generator")
}

repositories {
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.networknt:json-schema-validator:2.0.8")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-liquibase")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.2")
}

group = "io.altera"
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    // Ryuk (Testcontainers reaper) fails to start on Podman; test resources stop their own containers.
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}

// API-first: the contract in META-INF/openapi.yaml is the source of truth; JAX-RS interfaces and models are generated.
openApiGenerate {
    generatorName.set("kotlin-server")
    library.set("jaxrs-spec")
    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.path)
    apiPackage.set("onboarding.bff.api.generated")
    modelPackage.set("onboarding.bff.api.generated.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useJakartaEe" to "true",
            "useCoroutines" to "true",
            "returnResponse" to "true",
            "useBeanValidation" to "true",
            "serializationLibrary" to "jackson",
        )
    )
}

sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin"))

tasks.named("compileKotlin") { dependsOn("openApiGenerate") }

// Fails the build when the contract is not a valid OpenAPI document.
openApiValidate {
    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    recommend.set(true)
}

tasks.named("check") { dependsOn("openApiValidate") }
