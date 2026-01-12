package me.josh.axiom.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import me.josh.axiom.api.AxiomApiClient
import me.josh.axiom.core.AxiomGame
import me.josh.axiom.core.Fonts
import me.josh.axiom.entity.Enemy
import me.josh.axiom.entity.EnemyType
import me.josh.axiom.entity.Player
import me.josh.axiom.event.GameEndEvent
import me.josh.axiom.event.GameStartEvent
import me.josh.axiom.world.NoiseWorldGenerator
import me.josh.axiom.world.World
import kotlin.random.Random

/**
 * Main gameplay screen.
 *
 * Manages:
 * - World rendering and updates
 * - Player and entity management
 * - Camera following player
 * - Game UI (health, score, etc.)
 * - Game over state
 */
class GameScreen(
    private val game: AxiomGame
) : Screen {

    // Camera
    private val camera = OrthographicCamera()
    private val uiCamera = OrthographicCamera()

    // Colors matching website design
    private val cardColor = Color(0x0f / 255f, 0x2a / 255f, 0x27 / 255f, 1f)
    private val accentColor = Color(0x10 / 255f, 0xb9 / 255f, 0x81 / 255f, 1f)
    private val textColor = Color(0.93f, 0.96f, 0.96f, 1f)
    private val subtextColor = Color(0.55f, 0.65f, 0.65f, 1f)

    // World and entities
    private lateinit var world: World
    private lateinit var player: Player

    // Game state
    private var isPaused = false
    private var isGameOver = false
    private var gameOverTimer = 0f

    // Enemy spawning
    private var spawnTimer = 0f
    private val spawnInterval = 3f
    private val maxEnemies = 20

    override fun show() {
        setupCameras()
        initializeGame()
    }

    private fun setupCameras() {
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()

        camera.setToOrtho(false, width, height)
        uiCamera.setToOrtho(false, width, height)
    }

    private fun initializeGame() {
        val seed = Random.nextLong()
        world = World(NoiseWorldGenerator(seed))

        player = Player(0f, 0f)
        world.addEntity(player)

        world.updateLoadedChunks(player.x, player.y)

        for (i in 0..5) {
            spawnEnemy()
        }

        isPaused = false
        isGameOver = false
        spawnTimer = 0f

        game.eventBus.emit(GameStartEvent())
        Gdx.app.log("Game", "Game started with seed: $seed")
    }

    override fun render(delta: Float) {
        handleInput()

        if (!isPaused && !isGameOver) {
            update(delta)
        } else if (isGameOver) {
            gameOverTimer += delta
        }

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        renderWorld()
        renderUI()
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isGameOver) {
                returnToMenu()
            } else {
                isPaused = !isPaused
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if (isGameOver || isPaused) {
                initializeGame()
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.M) && (isPaused || isGameOver)) {
            returnToMenu()
        }
    }

    private fun update(delta: Float) {
        player.update(delta, world)

        if (player.isDead && !isGameOver) {
            onGameOver()
        }

        camera.position.set(player.x, player.y, 0f)
        camera.update()

        world.updateLoadedChunks(player.x, player.y)

        for (entity in world.getEntities()) {
            if (entity != player) {
                entity.update(delta, world)
            }
        }

        world.getEntities()
            .filter { it.isDead && it != player }
            .forEach { world.removeEntity(it) }

        spawnTimer += delta
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            if (world.getEntities().count { it is Enemy } < maxEnemies) {
                spawnEnemy()
            }
        }
    }

    private fun spawnEnemy() {
        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
        val distance = 300f + Random.nextFloat() * 200f

        val spawnX = player.x + kotlin.math.cos(angle) * distance
        val spawnY = player.y + kotlin.math.sin(angle) * distance

        if (!world.isWalkable(spawnX, spawnY)) {
            return
        }

        val type = when (Random.nextFloat()) {
            in 0f..0.5f -> EnemyType.SLIME
            in 0.5f..0.75f -> EnemyType.SKELETON
            in 0.75f..0.9f -> EnemyType.ORC
            else -> EnemyType.DEMON
        }

        val enemy = Enemy(spawnX, spawnY, type)
        world.addEntity(enemy)
    }

    private fun onGameOver() {
        isGameOver = true
        gameOverTimer = 0f

        game.eventBus.emit(GameEndEvent(player.kills, player.survivalTime))

        val playerId = game.currentPlayerId
        if (playerId != null) {
            val score = calculateScore(player.kills, player.survivalTime)
            AxiomApiClient.submitScore(
                kills = player.kills,
                survivalTime = player.survivalTime,
                score = score
            ) { success ->
                if (success) {
                    Gdx.app.log("Game", "Score saved: $score")
                } else {
                    Gdx.app.log("Game", "Failed to save score (offline mode)")
                }
            }
        }
    }

    private fun calculateScore(kills: Int, survivalTime: Float): Int {
        return kills * 100 + survivalTime.toInt()
    }

    private fun returnToMenu() {
        game.showMenu()
    }

    private fun renderWorld() {
        world.render(game.shapeRenderer, camera)

        game.shapeRenderer.projectionMatrix = camera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (entity in world.getEntities()) {
            entity.render(game.shapeRenderer)
        }

        game.shapeRenderer.end()
    }

    private fun renderUI() {
        game.batch.projectionMatrix = uiCamera.combined
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()

        game.batch.begin()

        // Health (top left)
        Fonts.ui.color = textColor
        Fonts.ui.draw(game.batch, "Health: ${player.health.toInt()}/${player.maxHealth.toInt()}", 20f, screenHeight - 20)

        // Score info (top right)
        val scoreText = "Kills: ${player.kills}  Time: ${player.survivalTime.toInt()}s"
        Fonts.ui.draw(game.batch, scoreText, screenWidth - 220, screenHeight - 20)

        val currentScore = calculateScore(player.kills, player.survivalTime)
        Fonts.ui.color = accentColor
        Fonts.ui.draw(game.batch, "Score: $currentScore", screenWidth - 220, screenHeight - 45)

        // Controls hint (bottom)
        Fonts.small.color = subtextColor
        Fonts.small.draw(game.batch, "WASD: Move | Click: Attack | ESC: Pause", 20f, 30f)

        game.batch.end()

        if (isPaused) {
            drawPauseOverlay(screenWidth, screenHeight)
        }

        if (isGameOver) {
            drawGameOverOverlay(screenWidth, screenHeight)
        }
    }

    private fun drawPauseOverlay(screenWidth: Float, screenHeight: Float) {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2

        // Card dimensions
        val cardWidth = 400f
        val cardHeight = 300f
        val cardX = centerX - cardWidth / 2
        val cardY = centerY - cardHeight / 2

        // Draw semi-transparent overlay
        game.shapeRenderer.projectionMatrix = uiCamera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
        game.shapeRenderer.rect(0f, 0f, screenWidth, screenHeight)
        game.shapeRenderer.end()

        // Draw card
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = cardColor
        game.shapeRenderer.rect(cardX, cardY, cardWidth, cardHeight)
        game.shapeRenderer.end()

        game.batch.begin()

        // Title
        Fonts.title.color = accentColor
        val pauseLayout = com.badlogic.gdx.graphics.g2d.GlyphLayout(Fonts.title, "PAUSED")
        Fonts.title.draw(game.batch, "PAUSED", centerX - pauseLayout.width / 2, cardY + cardHeight - 50)

        // Options
        Fonts.body.color = textColor
        Fonts.body.draw(game.batch, "[ESC] Resume", centerX - 65, centerY + 10)
        Fonts.body.draw(game.batch, "[R] Restart", centerX - 55, centerY - 30)
        Fonts.body.draw(game.batch, "[M] Menu", centerX - 45, centerY - 70)

        game.batch.end()
    }

    private fun drawGameOverOverlay(screenWidth: Float, screenHeight: Float) {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2

        // Card dimensions
        val cardWidth = 450f
        val cardHeight = 350f
        val cardX = centerX - cardWidth / 2
        val cardY = centerY - cardHeight / 2

        // Draw semi-transparent overlay
        game.shapeRenderer.projectionMatrix = uiCamera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
        game.shapeRenderer.rect(0f, 0f, screenWidth, screenHeight)
        game.shapeRenderer.end()

        // Draw card
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = cardColor
        game.shapeRenderer.rect(cardX, cardY, cardWidth, cardHeight)
        game.shapeRenderer.end()

        game.batch.begin()

        // Title
        Fonts.title.color = Color(0.9f, 0.3f, 0.3f, 1f)
        val gameOverLayout = com.badlogic.gdx.graphics.g2d.GlyphLayout(Fonts.title, "GAME OVER")
        Fonts.title.draw(game.batch, "GAME OVER", centerX - gameOverLayout.width / 2, cardY + cardHeight - 50)

        // Stats
        Fonts.body.color = textColor
        Fonts.body.draw(game.batch, "Kills: ${player.kills}", centerX - 45, centerY + 30)
        Fonts.body.draw(game.batch, "Survival Time: ${player.survivalTime.toInt()}s", centerX - 95, centerY - 5)

        val finalScore = calculateScore(player.kills, player.survivalTime)
        Fonts.heading.color = accentColor
        val scoreLayout = com.badlogic.gdx.graphics.g2d.GlyphLayout(Fonts.heading, "Final Score: $finalScore")
        Fonts.heading.draw(game.batch, "Final Score: $finalScore", centerX - scoreLayout.width / 2, centerY - 50)

        // Options
        Fonts.small.color = subtextColor
        val optionsLayout = com.badlogic.gdx.graphics.g2d.GlyphLayout(Fonts.small, "[R] Restart  [ESC/M] Menu")
        Fonts.small.draw(game.batch, "[R] Restart  [ESC/M] Menu", centerX - optionsLayout.width / 2, cardY + 50)

        game.batch.end()
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()

        uiCamera.viewportWidth = width.toFloat()
        uiCamera.viewportHeight = height.toFloat()
        uiCamera.position.set(width / 2f, height / 2f, 0f)
        uiCamera.update()
    }

    override fun pause() {
        isPaused = true
    }

    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        world.clear()
    }
}
