import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.10"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.codepaste"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("ch.qos.logback:logback-core:1.4.11")

    // Configuration
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

application {
    mainClass.set("com.codepaste.CodePasteApp")
}

// Create a fat JAR with all dependencies
tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")

    manifest {
        attributes["Main-Class"] = "com.codepaste.CodePasteApp"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}