package me.josh.axiom.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

/**
 * Centralized font management using FreeType for crisp text rendering.
 *
 * Uses Orbitron for titles/headings (futuristic tech aesthetic)
 * and Rajdhani for body/UI text (clean readability).
 */
object Fonts {

    lateinit var title: BitmapFont
        private set
    lateinit var heading: BitmapFont
        private set
    lateinit var body: BitmapFont
        private set
    lateinit var small: BitmapFont
        private set
    lateinit var ui: BitmapFont
        private set

    private val generators = mutableListOf<FreeTypeFontGenerator>()

    fun initialize() {
        val orbitronBold = FreeTypeFontGenerator(Gdx.files.internal("fonts/Orbitron/static/Orbitron-Bold.ttf"))
        val orbitronMedium = FreeTypeFontGenerator(Gdx.files.internal("fonts/Orbitron/static/Orbitron-Medium.ttf"))
        val rajdhaniMedium = FreeTypeFontGenerator(Gdx.files.internal("fonts/Rajdhani/Rajdhani-Medium.ttf"))
        val rajdhaniRegular = FreeTypeFontGenerator(Gdx.files.internal("fonts/Rajdhani/Rajdhani-Regular.ttf"))

        generators.addAll(listOf(orbitronBold, orbitronMedium, rajdhaniMedium, rajdhaniRegular))

        title = orbitronBold.generateFont(FreeTypeFontParameter().apply {
            size = 48
            color = Color.WHITE
        })

        heading = orbitronMedium.generateFont(FreeTypeFontParameter().apply {
            size = 32
            color = Color.WHITE
        })

        body = rajdhaniMedium.generateFont(FreeTypeFontParameter().apply {
            size = 22
            color = Color.WHITE
        })

        small = rajdhaniRegular.generateFont(FreeTypeFontParameter().apply {
            size = 18
            color = Color.WHITE
        })

        ui = rajdhaniMedium.generateFont(FreeTypeFontParameter().apply {
            size = 20
            color = Color.WHITE
        })

        Gdx.app.log("Fonts", "Website fonts initialized (Orbitron + Rajdhani)")
    }

    fun dispose() {
        title.dispose()
        heading.dispose()
        body.dispose()
        small.dispose()
        ui.dispose()
        generators.forEach { it.dispose() }
        generators.clear()
    }
}
