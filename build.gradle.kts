plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "me.josh"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // LibGDX Core
    implementation(libs.gdx.core)

    // LibGDX Desktop Backend (LWJGL3)
    implementation(libs.gdx.backend.lwjgl3)
    implementation(libs.gdx.platform) {
        artifact {
            classifier = "natives-desktop"
        }
    }

    // FreeType font rendering
    implementation(libs.gdx.freetype)
    implementation(libs.gdx.freetype.platform) {
        artifact {
            classifier = "natives-desktop"
        }
    }

    // KTX - Kotlin extensions for LibGDX
    implementation(libs.ktx.app)
    implementation(libs.ktx.graphics)
    implementation(libs.ktx.assets)
    implementation(libs.ktx.async)

    // Kotlin Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // HTTP Client
    implementation(libs.okhttp)

    // Noise generation for procedural terrain
    implementation(libs.joise)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("me.josh.axiom.AxiomKt")
}