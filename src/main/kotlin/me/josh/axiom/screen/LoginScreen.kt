package me.josh.axiom.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import me.josh.axiom.api.AxiomApiClient
import me.josh.axiom.core.AxiomGame
import me.josh.axiom.core.Fonts
import me.josh.axiom.event.PlayerLoginEvent

/**
 * Sign In / Sign Up screen for player authentication.
 * Styled to match the web interface.
 */
class LoginScreen(
    private val game: AxiomGame
) : Screen {

    private val layout = GlyphLayout()

    // Colors matching website
    private val bgColor = Color(0x0a / 255f, 0x1a / 255f, 0x1a / 255f, 1f) // Dark background
    private val cardColor = Color(0x0f / 255f, 0x2a / 255f, 0x27 / 255f, 1f) // Card background
    private val accentColor = Color(0x10 / 255f, 0xb9 / 255f, 0x81 / 255f, 1f) // Bright green
    private val inputBgColor = Color(0x0d / 255f, 0x24 / 255f, 0x22 / 255f, 1f) // Input background (slightly lighter)
    private val inputBorderColor = Color(0x1a / 255f, 0x3d / 255f, 0x39 / 255f, 1f) // Input border
    private val textColor = Color(0.93f, 0.96f, 0.96f, 1f) // Light text
    private val subtextColor = Color(0.55f, 0.65f, 0.65f, 1f) // Muted text

    // Input state
    private var username = ""
    private var password = ""
    private var activeField: InputField? = null
    private var errorMessage = ""
    private var successMessage = ""

    // UI state
    private var isSignUp = false
    private var isLoading = false

    // Clickable bounds
    private data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)
    private var usernameFieldBounds: Bounds? = null
    private var passwordFieldBounds: Bounds? = null
    private var submitButtonBounds: Bounds? = null
    private var toggleLinkBounds: Bounds? = null
    private var skipButtonBounds: Bounds? = null

    private enum class InputField {
        USERNAME, PASSWORD
    }

    override fun show() {
        Gdx.input.inputProcessor = null
        errorMessage = ""
        successMessage = ""
        isLoading = false
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (!isLoading) {
            handleInput()
        }

        drawUI()
    }

    private fun handleInput() {
        val mouseX = Gdx.input.x.toFloat()
        val mouseY = Gdx.graphics.height - Gdx.input.y.toFloat()

        // Update cursor based on hover state
        val isOverClickable = usernameFieldBounds?.let { isMouseOver(mouseX, mouseY, it) } == true ||
                              passwordFieldBounds?.let { isMouseOver(mouseX, mouseY, it) } == true ||
                              submitButtonBounds?.let { isMouseOver(mouseX, mouseY, it) } == true ||
                              toggleLinkBounds?.let { isMouseOver(mouseX, mouseY, it) } == true ||
                              skipButtonBounds?.let { isMouseOver(mouseX, mouseY, it) } == true

        Gdx.graphics.setSystemCursor(if (isOverClickable) Cursor.SystemCursor.Hand else Cursor.SystemCursor.Arrow)

        // Check field clicks
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            activeField = when {
                usernameFieldBounds?.let { isMouseOver(mouseX, mouseY, it) } == true -> InputField.USERNAME
                passwordFieldBounds?.let { isMouseOver(mouseX, mouseY, it) } == true -> InputField.PASSWORD
                else -> {
                    // Check button clicks
                    when {
                        submitButtonBounds?.let { isMouseOver(mouseX, mouseY, it) } == true -> {
                            submitForm()
                            null
                        }
                        toggleLinkBounds?.let { isMouseOver(mouseX, mouseY, it) } == true -> {
                            isSignUp = !isSignUp
                            errorMessage = ""
                            successMessage = ""
                            null
                        }
                        skipButtonBounds?.let { isMouseOver(mouseX, mouseY, it) } == true -> {
                            game.currentPlayerId = "dev"
                            game.currentPlayerName = "Developer"
                            game.showMenu()
                            null
                        }
                        else -> null
                    }
                }
            }
        }

        // Tab to switch fields
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            activeField = when (activeField) {
                InputField.USERNAME -> InputField.PASSWORD
                InputField.PASSWORD -> InputField.USERNAME
                null -> InputField.USERNAME
            }
        }

        // Enter to submit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            submitForm()
        }

        handleTextInput()
    }

    private fun submitForm() {
        if (isSignUp) {
            attemptSignUp()
        } else {
            attemptSignIn()
        }
    }

    private fun isMouseOver(mouseX: Float, mouseY: Float, bounds: Bounds): Boolean {
        return mouseX >= bounds.x &&
               mouseX <= bounds.x + bounds.width &&
               mouseY >= bounds.y &&
               mouseY <= bounds.y + bounds.height
    }

    private fun handleTextInput() {
        if (activeField == null) return

        // Backspace
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            when (activeField) {
                InputField.USERNAME -> if (username.isNotEmpty()) username = username.dropLast(1)
                InputField.PASSWORD -> if (password.isNotEmpty()) password = password.dropLast(1)
                null -> {}
            }
        }

        // Character input
        for (i in Input.Keys.A..Input.Keys.Z) {
            if (Gdx.input.isKeyJustPressed(i)) {
                val char = ('a' + (i - Input.Keys.A))
                appendToActiveField(char.toString())
            }
        }
        for (i in Input.Keys.NUM_0..Input.Keys.NUM_9) {
            if (Gdx.input.isKeyJustPressed(i)) {
                val char = ('0' + (i - Input.Keys.NUM_0))
                appendToActiveField(char.toString())
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            appendToActiveField(" ")
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
            appendToActiveField(".")
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            appendToActiveField("-")
        }
    }

    private fun appendToActiveField(char: String) {
        when (activeField) {
            InputField.USERNAME -> if (username.length < 20) username += char
            InputField.PASSWORD -> if (password.length < 30) password += char
            null -> {}
        }
    }

    private fun drawUI() {
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        val centerX = screenWidth / 2

        // Card dimensions
        val cardWidth = 400f
        val cardHeight = 500f
        val cardX = centerX - cardWidth / 2
        val cardY = screenHeight / 2 - cardHeight / 2

        // Draw card background
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = cardColor
        game.shapeRenderer.rect(cardX, cardY, cardWidth, cardHeight)
        game.shapeRenderer.end()

        game.batch.begin()

        // Title
        Fonts.title.color = accentColor
        layout.setText(Fonts.title, "AXIOM")
        Fonts.title.draw(game.batch, "AXIOM", centerX - layout.width / 2, cardY + cardHeight - 50)

        // Heading
        val heading = if (isSignUp) "JOIN THE BATTLE" else "WELCOME BACK"
        Fonts.heading.color = textColor
        layout.setText(Fonts.heading, heading)
        Fonts.heading.draw(game.batch, heading, centerX - layout.width / 2, cardY + cardHeight - 105)

        // Subheading
        val subheading = if (isSignUp) "Create your account" else "Sign in to continue"
        Fonts.small.color = subtextColor
        layout.setText(Fonts.small, subheading)
        Fonts.small.draw(game.batch, subheading, centerX - layout.width / 2, cardY + cardHeight - 135)

        game.batch.end()

        // Input fields
        val fieldWidth = 320f
        val fieldHeight = 48f
        val fieldX = centerX - fieldWidth / 2

        // Username field
        var fieldY = cardY + cardHeight - 230
        drawInputField(fieldX, fieldY, fieldWidth, fieldHeight, "Username", username, activeField == InputField.USERNAME)
        usernameFieldBounds = Bounds(fieldX, fieldY, fieldWidth, fieldHeight)

        // Password field
        fieldY = cardY + cardHeight - 315
        drawInputField(fieldX, fieldY, fieldWidth, fieldHeight, "Password", "*".repeat(password.length), activeField == InputField.PASSWORD)
        passwordFieldBounds = Bounds(fieldX, fieldY, fieldWidth, fieldHeight)

        game.batch.begin()

        // Loading/Error/Success messages
        val messageY = cardY + cardHeight - 355
        if (isLoading) {
            Fonts.small.color = accentColor
            layout.setText(Fonts.small, "Loading...")
            Fonts.small.draw(game.batch, "Loading...", centerX - layout.width / 2, messageY)
        } else {
            if (errorMessage.isNotEmpty()) {
                Fonts.small.color = Color(0.9f, 0.3f, 0.3f, 1f)
                layout.setText(Fonts.small, errorMessage)
                Fonts.small.draw(game.batch, errorMessage, centerX - layout.width / 2, messageY)
            }
            if (successMessage.isNotEmpty()) {
                Fonts.small.color = accentColor
                layout.setText(Fonts.small, successMessage)
                Fonts.small.draw(game.batch, successMessage, centerX - layout.width / 2, messageY)
            }
        }

        game.batch.end()

        // Submit button
        val buttonY = cardY + cardHeight - 405
        val buttonWidth = 320f
        val buttonHeight = 52f
        val buttonX = centerX - buttonWidth / 2

        val mouseX = Gdx.input.x.toFloat()
        val mouseY = Gdx.graphics.height - Gdx.input.y.toFloat()
        val isButtonHovered = isMouseOver(mouseX, mouseY, Bounds(buttonX, buttonY, buttonWidth, buttonHeight))

        drawButton(buttonX, buttonY, buttonWidth, buttonHeight, if (isSignUp) "SIGN UP" else "SIGN IN", isButtonHovered)
        submitButtonBounds = Bounds(buttonX, buttonY, buttonWidth, buttonHeight)

        // Toggle link
        game.batch.begin()
        val toggleText = if (isSignUp) "Already have an account? Sign in" else "Don't have an account? Join the battle"
        layout.setText(Fonts.small, toggleText)
        val toggleX = centerX - layout.width / 2
        val toggleY = cardY + 60

        val isToggleHovered = isMouseOver(mouseX, mouseY, Bounds(toggleX - 10, toggleY - 20, layout.width + 20, 30f))
        Fonts.small.color = if (isToggleHovered) accentColor.cpy().lerp(Color.WHITE, 0.2f) else accentColor
        Fonts.small.draw(game.batch, toggleText, toggleX, toggleY)
        toggleLinkBounds = Bounds(toggleX - 10, toggleY - 20, layout.width + 20, 30f)

        // Skip link (dev mode)
        val skipText = "Skip (dev mode)"
        layout.setText(Fonts.small, skipText)
        val skipX = centerX - layout.width / 2
        val skipY = cardY + 30

        val isSkipHovered = isMouseOver(mouseX, mouseY, Bounds(skipX - 10, skipY - 20, layout.width + 20, 30f))
        Fonts.small.color = if (isSkipHovered) Color.GRAY.cpy().lerp(Color.WHITE, 0.3f) else Color.DARK_GRAY
        Fonts.small.draw(game.batch, skipText, skipX, skipY)
        skipButtonBounds = Bounds(skipX - 10, skipY - 20, layout.width + 20, 30f)

        game.batch.end()
    }

    private fun drawInputField(x: Float, y: Float, width: Float, height: Float, label: String, value: String, isActive: Boolean) {
        // Background
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = inputBgColor
        game.shapeRenderer.rect(x, y, width, height)
        game.shapeRenderer.end()

        // Border (always visible)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        game.shapeRenderer.color = if (isActive) accentColor else inputBorderColor
        Gdx.gl.glLineWidth(if (isActive) 2f else 1f)
        game.shapeRenderer.rect(x, y, width, height)
        game.shapeRenderer.end()
        Gdx.gl.glLineWidth(1f)

        game.batch.begin()

        // Label above field
        Fonts.small.color = if (isActive) accentColor else subtextColor
        Fonts.small.draw(game.batch, label.uppercase(), x + 2, y + height + 20)

        // Value with cursor
        val displayValue = if (value.isEmpty() && !isActive) {
            Fonts.body.color = Color(0.35f, 0.45f, 0.45f, 1f)
            "Enter your ${label.lowercase()}"
        } else {
            Fonts.body.color = textColor
            value + if (isActive) "|" else ""
        }

        Fonts.body.draw(game.batch, displayValue, x + 15, y + height / 2 + 7)

        game.batch.end()
    }

    private fun drawButton(x: Float, y: Float, width: Float, height: Float, text: String, isHovered: Boolean) {
        val buttonColor = if (isHovered) accentColor.cpy().lerp(Color.WHITE, 0.15f) else accentColor

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = buttonColor
        game.shapeRenderer.rect(x, y, width, height)
        game.shapeRenderer.end()

        game.batch.begin()
        Fonts.heading.color = Color.WHITE
        layout.setText(Fonts.heading, text)
        Fonts.heading.draw(game.batch, text, x + width / 2 - layout.width / 2, y + height / 2 + layout.height / 2)
        game.batch.end()
    }

    private fun attemptSignIn() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Please enter username and password"
            return
        }

        isLoading = true
        errorMessage = ""
        successMessage = ""

        AxiomApiClient.login(
            username = username,
            password = password,
            onSuccess = { userId, username ->
                isLoading = false
                game.currentPlayerId = userId
                game.currentPlayerName = username
                game.eventBus.emit(PlayerLoginEvent(userId, username))
                successMessage = "Sign in successful!"
                game.showMenu()
            },
            onFailure = { error ->
                isLoading = false
                if (error.contains("Connection error")) {
                    errorMessage = "Connection error - using offline mode"
                    game.currentPlayerId = "offline_${username}"
                    game.currentPlayerName = username
                    game.showMenu()
                } else {
                    errorMessage = error
                }
            }
        )
    }

    private fun attemptSignUp() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Please enter username and password"
            return
        }

        if (username.length < 3) {
            errorMessage = "Username must be at least 3 characters"
            return
        }

        if (password.length < 4) {
            errorMessage = "Password must be at least 4 characters"
            return
        }

        isLoading = true
        errorMessage = ""
        successMessage = ""

        AxiomApiClient.register(
            username = username,
            password = password,
            onSuccess = { userId, username ->
                isLoading = false
                game.currentPlayerId = userId
                game.currentPlayerName = username
                game.eventBus.emit(PlayerLoginEvent(userId, username))
                successMessage = "Account created! Welcome!"
                game.showMenu()
            },
            onFailure = { error ->
                isLoading = false
                errorMessage = error
            }
        )
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {}
}
