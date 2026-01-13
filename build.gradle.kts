plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.runtime)
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

tasks.shadowJar {
    archiveBaseName.set("axiom-game")
    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
        attributes["Main-Class"] = "me.josh.axiom.AxiomKt"
    }

    // Merge service files from dependencies
    mergeServiceFiles()
}

runtime {
    options.addAll(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))

    modules.addAll(listOf(
        "java.base",
        "java.desktop",
        "java.logging",
        "java.management",
        "java.naming",
        "java.sql",
        "jdk.unsupported"
    ))

    jpackage {
        jpackageHome = System.getProperty("java.home")

        // Application icon and metadata for the executable
        imageOptions.addAll(listOf(
            "--icon", "src/main/resources/icon.ico",
            "--vendor", "Josh Baker",
            "--copyright", "Copyright 2026 Josh Baker",
            "--description", "Axiom - 2D Top-Down Sandbox Game",
            "--app-version", project.version.toString()
        ))

        installerOptions.addAll(listOf(
            "--win-dir-chooser",
            "--win-menu",
            "--win-shortcut",
            "--win-per-user-install",
            "--vendor", "Josh Baker",
            "--copyright", "Copyright 2026"
        ))

        installerType = "msi"
        installerName = "Axiom-Installer"
        appVersion = project.version.toString()
    }

    targetPlatform("win") {
        jdkHome.set(System.getProperty("java.home"))
    }
}

// Rename shadowJar output for runtime plugin
tasks.installShadowDist {
    doLast {
        val libDir = file("${destinationDir}/lib")
        val sourceJar = file("${libDir}/axiom-game.jar")
        val targetJar = file("${libDir}/${project.name}-${project.version}-all.jar")
        if (sourceJar.exists() && !targetJar.exists()) {
            sourceJar.renameTo(targetJar)
        }
    }
}

// Override installDist to use shadowJar
tasks.installDist {
    dependsOn(tasks.installShadowDist)
    doLast {
        delete(destinationDir)
        copy {
            from(tasks.installShadowDist.get().destinationDir)
            into(destinationDir)
        }
    }
}

// Create distributable ZIP of portable app with bundled JRE
tasks.register<Zip>("packagePortable") {
    dependsOn("jpackageImage")

    archiveBaseName.set("Axiom")
    archiveVersion.set("")
    destinationDirectory.set(file("${buildDir}/distributions"))

    // Put files directly in ZIP root (no nested folder)
    from("${buildDir}/jpackage/axiom-game")

    doLast {
        println("Created distributable ZIP: ${archiveFile.get().asFile}")
        println("Size: ${archiveFile.get().asFile.length() / 1024 / 1024}MB")
    }
}