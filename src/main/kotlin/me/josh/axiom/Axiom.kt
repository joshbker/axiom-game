package me.josh.axiom

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import me.josh.axiom.core.AxiomGame

/**
 * Application entry point.
 * Configures the LWJGL3 window and launches the game.
 */
fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Axiom")
        setWindowedMode(1280, 720)
        setForegroundFPS(60)
        useVsync(true)
        setResizable(true)
    }

    Lwjgl3Application(AxiomGame(), config)
}
