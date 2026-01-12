plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = "me.josh"
version = "1.0-SNAPSHOT"

val gdxVersion = "1.12.1"
val ktxVersion = "1.12.1-rc1"
val coroutinesVersion = "1.8.1"

repositories {
    mavenCentral()
}

dependencies {
    // LibGDX Core
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")

    // LibGDX Desktop Backend (LWJGL3)
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    // FreeType font rendering
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")

    // KTX - Kotlin extensions for LibGDX
    implementation("io.github.libktx:ktx-app:$ktxVersion")
    implementation("io.github.libktx:ktx-graphics:$ktxVersion")
    implementation("io.github.libktx:ktx-assets:$ktxVersion")
    implementation("io.github.libktx:ktx-async:$ktxVersion")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Noise generation for procedural terrain
    implementation("com.sudoplay.joise:joise:1.1.0")

    testImplementation(kotlin("test"))
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