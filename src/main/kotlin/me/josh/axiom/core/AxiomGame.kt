package me.josh.axiom.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import me.josh.axiom.api.AxiomApiClient
import me.josh.axiom.event.EventBus
import me.josh.axiom.screen.GameScreen
import me.josh.axiom.screen.LoginScreen
import me.josh.axiom.screen.MenuScreen

/**
 * Main game class - the central hub for the application.
 *
 * Uses LibGDX's Game class which provides screen management.
 * Implements Singleton pattern via companion object for global access
 * to shared resources like SpriteBatch and EventBus.
 */
class AxiomGame : Game() {

    lateinit var batch: SpriteBatch
        private set

    lateinit var shapeRenderer: ShapeRenderer
        private set

    val eventBus = EventBus()

    // Currently logged-in player (null if not authenticated)
    var currentPlayerId: String? = null
    var currentPlayerName: String? = null

    override fun create() {
        instance = this

        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        // Initialize fonts
        Fonts.initialize()

        // Initialize API client
        AxiomApiClient.initialize()

        Gdx.app.log("Axiom", "Game initialized")

        // Check for saved session
        val savedUser = AxiomApiClient.getSavedUser()
        if (savedUser != null) {
            currentPlayerId = savedUser.first
            currentPlayerName = savedUser.second
            Gdx.app.log("Axiom", "Restored session for ${savedUser.second}")
            setScreen(MenuScreen(this))
        } else {
            // Start at the login screen
            setScreen(LoginScreen(this))
        }
    }

    override fun render() {
        super.render()
    }

    override fun dispose() {
        AxiomApiClient.shutdown()
        Fonts.dispose()
        batch.dispose()
        shapeRenderer.dispose()
        screen?.dispose()

        Gdx.app.log("Axiom", "Game disposed")
    }

    /**
     * Navigate to the main menu screen
     */
    fun showMenu() {
        setScreen(MenuScreen(this))
    }

    /**
     * Start a new game session
     */
    fun startGame() {
        setScreen(GameScreen(this))
    }

    /**
     * Log out and return to login screen
     */
    fun logout() {
        currentPlayerId = null
        currentPlayerName = null
        AxiomApiClient.clearSession()
        setScreen(LoginScreen(this))
    }

    companion object {
        /**
         * Singleton instance for global access.
         * Use sparingly - prefer dependency injection where possible.
         */
        lateinit var instance: AxiomGame
            private set
    }
}
