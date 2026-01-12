package me.josh.axiom.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import me.josh.axiom.core.AxiomGame
import me.josh.axiom.event.EntityDamageEvent
import me.josh.axiom.event.EntityDeathEvent
import me.josh.axiom.world.World

/**
 * Base class for all game entities (players, enemies, NPCs).
 *
 * Provides common functionality:
 * - Position and movement
 * - Health and damage
 * - Collision bounds
 * - Basic rendering
 *
 * Subclasses implement specific behavior via update() and render() methods.
 */
abstract class Entity(
    var x: Float,
    var y: Float,
    val width: Float = 24f,
    val height: Float = 24f
) {
    // Unique identifier
    val id: String = java.util.UUID.randomUUID().toString()

    // Health system
    var maxHealth: Float = 100f
    var health: Float = maxHealth
        protected set

    val isDead: Boolean get() = health <= 0

    // Movement
    var velocityX: Float = 0f
    var velocityY: Float = 0f
    var baseSpeed: Float = 150f

    // Visual
    open val color: Color = Color.WHITE

    /**
     * Update entity state. Called every frame.
     */
    abstract fun update(delta: Float, world: World)

    /**
     * Render the entity.
     */
    open fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color
        shapeRenderer.rect(x - width / 2, y - height / 2, width, height)
    }

    /**
     * Apply damage to this entity.
     * Emits EntityDamageEvent and EntityDeathEvent if fatal.
     */
    open fun takeDamage(amount: Float, source: Entity?) {
        if (isDead) return

        val event = EntityDamageEvent(this, amount, source)
        AxiomGame.instance.eventBus.emit(event)

        if (event.cancelled) return

        health = (health - amount).coerceAtLeast(0f)

        if (isDead) {
            onDeath(source)
        }
    }

    /**
     * Heal this entity.
     */
    fun heal(amount: Float) {
        if (isDead) return
        health = (health + amount).coerceAtMost(maxHealth)
    }

    /**
     * Called when entity dies.
     */
    protected open fun onDeath(killer: Entity?) {
        AxiomGame.instance.eventBus.emit(EntityDeathEvent(this, killer))
    }

    /**
     * Move the entity, respecting collision.
     */
    protected fun move(dx: Float, dy: Float, world: World) {
        // Try horizontal movement
        val newX = x + dx
        if (world.isWalkable(newX - width / 2, y) &&
            world.isWalkable(newX + width / 2, y)) {
            x = newX
        }

        // Try vertical movement
        val newY = y + dy
        if (world.isWalkable(x, newY - height / 2) &&
            world.isWalkable(x, newY + height / 2)) {
            y = newY
        }
    }

    /**
     * Check if this entity overlaps with another.
     */
    fun overlaps(other: Entity): Boolean {
        return x - width / 2 < other.x + other.width / 2 &&
                x + width / 2 > other.x - other.width / 2 &&
                y - height / 2 < other.y + other.height / 2 &&
                y + height / 2 > other.y - other.height / 2
    }

    /**
     * Get distance to another entity.
     */
    fun distanceTo(other: Entity): Float {
        val dx = other.x - x
        val dy = other.y - y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
