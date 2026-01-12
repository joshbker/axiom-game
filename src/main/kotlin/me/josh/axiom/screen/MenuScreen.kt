package me.josh.axiom.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import me.josh.axiom.api.AxiomApiClient
import me.josh.axiom.api.models.LeaderboardEntry
import me.josh.axiom.core.AxiomGame
import me.josh.axiom.core.Fonts

/**
 * Main menu screen with navigation options.
 *
 * Provides access to:
 * - Start new game
 * - View leaderboard (loaded asynchronously)
 * - Logout
 */
class MenuScreen(
    private val game: AxiomGame
) : Screen {

    private val camera = OrthographicCamera()
    private val layout = GlyphLayout()

    // Colors matching website design
    private val bgColor = Color(0x0a / 255f, 0x1a / 255f, 0x1a / 255f, 1f)
    private val cardColor = Color(0x0f / 255f, 0x2a / 255f, 0x27 / 255f, 1f)
    private val accentColor = Color(0x10 / 255f, 0xb9 / 255f, 0x81 / 255f, 1f)
    private val textColor = Color(0.93f, 0.96f, 0.96f, 1f)
    private val subtextColor = Color(0.55f, 0.65f, 0.65f, 1f)

    private var hoveredOption = -1
    private val menuOptions = listOf("Play Game", "Leaderboard", "Sign Out")

    // Menu item positions (updated during render)
    private data class MenuItemBounds(val x: Float, val y: Float, val width: Float, val height: Float)
    private val menuItemBounds = mutableListOf<MenuItemBounds>()

    // Leaderboard display
    private var showingLeaderboard = false
    private var leaderboardEntries: List<LeaderboardEntry> = emptyList()
    private var isLoadingLeaderboard = false
    private var leaderboardError = ""

    // Back button bounds
    private var backButtonBounds: MenuItemBounds? = null

    override fun show() {
        showingLeaderboard = false
        isLoadingLeaderboard = false
        leaderboardError = ""
        setupCamera()
    }

    private fun setupCamera() {
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        camera.setToOrtho(false, width, height)
        camera.position.set(width / 2f, height / 2f, 0f)
        camera.update()
    }

    private fun loadLeaderboard() {
        if (isLoadingLeaderboard) return

        isLoadingLeaderboard = true
        leaderboardError = ""

        AxiomApiClient.getTopScores(
            limit = 10,
            onSuccess = { scores ->
                leaderboardEntries = scores
                isLoadingLeaderboard = false
            },
            onFailure = { error ->
                leaderboardError = error
                isLoadingLeaderboard = false
            }
        )
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        handleInput()

        if (showingLeaderboard) {
            drawLeaderboard()
        } else {
            drawMenu()
        }
    }

    private fun handleInput() {
        val mouseX = Gdx.input.x.toFloat()
        val mouseY = Gdx.graphics.height - Gdx.input.y.toFloat()

        if (showingLeaderboard) {
            // Update cursor for back button
            val isOverBack = backButtonBounds?.let { isMouseOver(mouseX, mouseY, it) } == true
            Gdx.graphics.setSystemCursor(if (isOverBack) Cursor.SystemCursor.Hand else Cursor.SystemCursor.Arrow)

            // Check back button hover/click
            backButtonBounds?.let { bounds ->
                if (isMouseOver(mouseX, mouseY, bounds)) {
                    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                        showingLeaderboard = false
                    }
                }
            }

            // Keyboard back
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                showingLeaderboard = false
            }
            return
        }

        // Check menu item hover
        hoveredOption = -1
        var isOverMenuItem = false
        for ((index, bounds) in menuItemBounds.withIndex()) {
            if (isMouseOver(mouseX, mouseY, bounds)) {
                hoveredOption = index
                isOverMenuItem = true

                // Click handling
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    when (index) {
                        0 -> game.startGame()
                        1 -> {
                            showingLeaderboard = true
                            loadLeaderboard()
                        }
                        2 -> game.logout()
                    }
                }
                break
            }
        }

        // Update cursor for menu items
        Gdx.graphics.setSystemCursor(if (isOverMenuItem) Cursor.SystemCursor.Hand else Cursor.SystemCursor.Arrow)
    }

    private fun isMouseOver(mouseX: Float, mouseY: Float, bounds: MenuItemBounds): Boolean {
        return mouseX >= bounds.x &&
               mouseX <= bounds.x + bounds.width &&
               mouseY >= bounds.y &&
               mouseY <= bounds.y + bounds.height
    }

    private fun drawMenu() {
        val screenWidth = camera.viewportWidth
        val screenHeight = camera.viewportHeight
        val centerX = screenWidth / 2

        menuItemBounds.clear()

        // Card dimensions
        val cardWidth = 450f
        val cardHeight = 450f
        val cardX = centerX - cardWidth / 2
        val cardY = screenHeight / 2 - cardHeight / 2

        // Draw card background
        game.shapeRenderer.projectionMatrix = camera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = cardColor
        game.shapeRenderer.rect(cardX, cardY, cardWidth, cardHeight)
        game.shapeRenderer.end()

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()

        // Title
        Fonts.title.color = accentColor
        layout.setText(Fonts.title, "AXIOM")
        Fonts.title.draw(game.batch, "AXIOM", centerX - layout.width / 2, cardY + cardHeight - 50)

        // Welcome message
        Fonts.body.color = subtextColor
        val welcome = "Welcome back, ${game.currentPlayerName ?: "Player"}"
        layout.setText(Fonts.body, welcome)
        Fonts.body.draw(game.batch, welcome, centerX - layout.width / 2, cardY + cardHeight - 105)

        game.batch.end()

        // Menu buttons
        val buttonWidth = 340f
        val buttonHeight = 52f
        val buttonX = centerX - buttonWidth / 2
        var buttonY = cardY + cardHeight - 190

        val mouseX = Gdx.input.x.toFloat()
        val mouseY = Gdx.graphics.height - Gdx.input.y.toFloat()

        for ((index, option) in menuOptions.withIndex()) {
            val isHovered = index == hoveredOption
            val buttonBounds = MenuItemBounds(buttonX, buttonY, buttonWidth, buttonHeight)

            drawMenuButton(buttonX, buttonY, buttonWidth, buttonHeight, option, isHovered)
            menuItemBounds.add(buttonBounds)

            buttonY -= 70
        }

        game.batch.begin()

        // Instructions
        Fonts.small.color = subtextColor
        layout.setText(Fonts.small, "Click a button to continue")
        Fonts.small.draw(game.batch, "Click a button to continue", centerX - layout.width / 2, cardY + 40)

        game.batch.end()
    }

    private fun drawMenuButton(x: Float, y: Float, width: Float, height: Float, text: String, isHovered: Boolean) {
        val buttonColor = if (isHovered) accentColor.cpy().lerp(Color.WHITE, 0.15f) else accentColor

        game.shapeRenderer.projectionMatrix = camera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = buttonColor
        game.shapeRenderer.rect(x, y, width, height)
        game.shapeRenderer.end()

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        Fonts.heading.color = Color.WHITE
        layout.setText(Fonts.heading, text.uppercase())
        Fonts.heading.draw(game.batch, text.uppercase(), x + width / 2 - layout.width / 2, y + height / 2 + layout.height / 2 - 2)
        game.batch.end()
    }

    private fun drawLeaderboard() {
        val screenWidth = camera.viewportWidth
        val screenHeight = camera.viewportHeight
        val centerX = screenWidth / 2

        // Card dimensions (wider for table)
        val cardWidth = 650f
        val cardHeight = 550f
        val cardX = centerX - cardWidth / 2
        val cardY = screenHeight / 2 - cardHeight / 2

        // Draw card background
        game.shapeRenderer.projectionMatrix = camera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = cardColor
        game.shapeRenderer.rect(cardX, cardY, cardWidth, cardHeight)
        game.shapeRenderer.end()

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()

        // Title
        Fonts.title.color = accentColor
        layout.setText(Fonts.title, "LEADERBOARD")
        Fonts.title.draw(game.batch, "LEADERBOARD", centerX - layout.width / 2, cardY + cardHeight - 50)

        // Loading state
        if (isLoadingLeaderboard) {
            Fonts.body.color = accentColor
            layout.setText(Fonts.body, "Loading...")
            Fonts.body.draw(game.batch, "Loading...", centerX - layout.width / 2, cardY + cardHeight - 150)
        } else if (leaderboardError.isNotEmpty()) {
            Fonts.body.color = Color(0.9f, 0.3f, 0.3f, 1f)
            layout.setText(Fonts.body, leaderboardError)
            Fonts.body.draw(game.batch, leaderboardError, centerX - layout.width / 2, cardY + cardHeight - 150)
        } else {
            // Column headers
            Fonts.ui.color = subtextColor
            Fonts.ui.draw(game.batch, "RANK", cardX + 50, cardY + cardHeight - 130)
            Fonts.ui.draw(game.batch, "PLAYER", cardX + 150, cardY + cardHeight - 130)
            Fonts.ui.draw(game.batch, "KILLS", cardX + 400, cardY + cardHeight - 130)
            Fonts.ui.draw(game.batch, "SCORE", cardX + 510, cardY + cardHeight - 130)

            // Separator line
            game.batch.end()
            game.shapeRenderer.projectionMatrix = camera.combined
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.shapeRenderer.color = Color(0x1a / 255f, 0x3d / 255f, 0x39 / 255f, 1f)
            game.shapeRenderer.rect(cardX + 40, cardY + cardHeight - 145, cardWidth - 80, 1f)
            game.shapeRenderer.end()
            game.batch.projectionMatrix = camera.combined
            game.batch.begin()

            // Entries
            var yPos = cardY + cardHeight - 175

            if (leaderboardEntries.isEmpty()) {
                Fonts.body.color = subtextColor
                layout.setText(Fonts.body, "No scores yet - be the first!")
                Fonts.body.draw(game.batch, "No scores yet - be the first!", centerX - layout.width / 2, yPos)
            } else {
                for ((index, entry) in leaderboardEntries.withIndex()) {
                    val entryColor = when (index) {
                        0 -> accentColor
                        1 -> Color(0.7f, 0.8f, 0.8f, 1f)
                        2 -> Color(0.6f, 0.7f, 0.7f, 1f)
                        else -> textColor
                    }
                    Fonts.ui.color = entryColor

                    val rank = "${index + 1}."
                    Fonts.ui.draw(game.batch, rank, cardX + 50, yPos)
                    Fonts.ui.draw(game.batch, entry.playerName, cardX + 150, yPos)
                    Fonts.ui.draw(game.batch, entry.kills.toString(), cardX + 400, yPos)
                    Fonts.ui.draw(game.batch, entry.score.toString(), cardX + 510, yPos)

                    yPos -= 38
                }
            }
        }

        // Back button
        val backText = "< Back to Menu"
        layout.setText(Fonts.body, backText)
        val backX = centerX - layout.width / 2
        val backY = cardY + 50

        backButtonBounds = MenuItemBounds(backX - 10, backY - 5, layout.width + 20, layout.height + 10)

        val mouseX = Gdx.input.x.toFloat()
        val mouseY = Gdx.graphics.height - Gdx.input.y.toFloat()
        val isBackHovered = backButtonBounds?.let { isMouseOver(mouseX, mouseY, it) } ?: false

        Fonts.body.color = if (isBackHovered) accentColor else subtextColor
        Fonts.body.draw(game.batch, backText, backX, backY)

        // ESC hint
        Fonts.small.color = Color(0.3f, 0.4f, 0.4f, 1f)
        layout.setText(Fonts.small, "Press ESC to go back")
        Fonts.small.draw(game.batch, "Press ESC to go back", centerX - layout.width / 2, cardY + 25)

        game.batch.end()
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.position.set(width / 2f, height / 2f, 0f)
        camera.update()
    }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {}
}
