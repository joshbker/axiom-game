package me.josh.axiom.entity

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import me.josh.axiom.core.AxiomGame
import me.josh.axiom.event.AttackEvent
import me.josh.axiom.event.PlayerMoveEvent
import me.josh.axiom.world.World

/**
 * The player entity controlled by keyboard/mouse input.
 *
 * Handles:
 * - WASD movement with terrain speed modifiers
 * - Mouse-based attacking
 * - Score tracking
 */
class Player(
    x: Float,
    y: Float
) : Entity(x, y, 28f, 28f) {

    override val color: Color = Color(0.2f, 0.6f, 1f, 1f) // Blue

    // Combat
    var kills: Int = 0
        private set
    var attackCooldown: Float = 0f
    val attackRange: Float = 50f
    val attackDamage: Float = 25f
    val attackCooldownTime: Float = 0.5f

    // Game stats
    var survivalTime: Float = 0f
        private set

    init {
        maxHealth = 100f
        health = maxHealth
        baseSpeed = 200f
    }

    override fun update(delta: Float, world: World) {
        if (isDead) return

        survivalTime += delta

        // Handle movement input
        handleMovement(delta, world)

        // Handle attack input
        handleAttack(delta, world)

        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown -= delta
        }
    }

    private fun handleMovement(delta: Float, world: World) {
        val oldX = x
        val oldY = y

        // Get input direction
        var dx = 0f
        var dy = 0f

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            dy += 1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            dy -= 1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            dx -= 1f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            dx += 1f
        }

        // Normalize diagonal movement
        if (dx != 0f && dy != 0f) {
            val length = kotlin.math.sqrt(dx * dx + dy * dy)
            dx /= length
            dy /= length
        }

        // Apply terrain speed modifier
        val speedMod = world.getSpeedModifier(x, y)
        val actualSpeed = baseSpeed * speedMod * delta

        // Move
        if (dx != 0f || dy != 0f) {
            move(dx * actualSpeed, dy * actualSpeed, world)
            world.updateEntityChunk(this)

            // Emit move event if position changed
            if (x != oldX || y != oldY) {
                AxiomGame.instance.eventBus.emit(PlayerMoveEvent(oldX, oldY, x, y))
            }
        }
    }

    private fun handleAttack(delta: Float, world: World) {
        // Left click to attack
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && attackCooldown <= 0) {
            performAttack(world)
        }
    }

    private fun performAttack(world: World) {
        attackCooldown = attackCooldownTime

        // Find enemies in range
        val nearbyEntities = world.getEntitiesNear(x, y, attackRange)
        val target = nearbyEntities
            .filterIsInstance<Enemy>()
            .filter { !it.isDead }
            .minByOrNull { distanceTo(it) }

        if (target != null) {
            val event = AttackEvent(this, target, attackDamage)
            AxiomGame.instance.eventBus.emit(event)

            if (!event.cancelled) {
                target.takeDamage(attackDamage, this)

                if (target.isDead) {
                    kills++
                }
            }
        }
    }

    override fun render(shapeRenderer: ShapeRenderer) {
        // Draw player body
        shapeRenderer.color = color
        shapeRenderer.rect(x - width / 2, y - height / 2, width, height)

        // Draw health bar above player
        val healthBarWidth = 32f
        val healthBarHeight = 4f
        val healthBarY = y + height / 2 + 4f

        // Background (red)
        shapeRenderer.color = Color.RED
        shapeRenderer.rect(x - healthBarWidth / 2, healthBarY, healthBarWidth, healthBarHeight)

        // Health (green)
        shapeRenderer.color = Color.GREEN
        val healthPercent = health / maxHealth
        shapeRenderer.rect(x - healthBarWidth / 2, healthBarY, healthBarWidth * healthPercent, healthBarHeight)

        // Draw attack range indicator when attacking
        if (attackCooldown > attackCooldownTime * 0.5f) {
            shapeRenderer.color = Color(1f, 1f, 1f, 0.2f)
            shapeRenderer.circle(x, y, attackRange)
        }
    }

    override fun onDeath(killer: Entity?) {
        super.onDeath(killer)
        Gdx.app.log("Player", "Player died! Kills: $kills, Survival time: ${survivalTime}s")
    }

    /**
     * Reset player state for a new game.
     */
    fun reset(spawnX: Float, spawnY: Float) {
        x = spawnX
        y = spawnY
        health = maxHealth
        kills = 0
        survivalTime = 0f
        attackCooldown = 0f
    }
}
