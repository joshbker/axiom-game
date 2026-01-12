package me.josh.axiom.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import me.josh.axiom.core.AxiomGame
import me.josh.axiom.event.AttackEvent
import me.josh.axiom.world.World
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * AI state for enemy behavior.
 *
 * Design Pattern: State
 * - Encapsulates behavior for each AI state
 * - Clean state transitions without messy conditionals
 * - Easily extensible with new states
 */
sealed class AIState {
    object Idle : AIState()
    object Wandering : AIState()
    data class Chasing(val target: Entity) : AIState()
    data class Attacking(val target: Entity) : AIState()
    object Fleeing : AIState()
}

/**
 * Enemy entity with basic AI behavior.
 *
 * Behaviors:
 * - Wanders randomly when no player nearby
 * - Chases player when in detection range
 * - Attacks player when in attack range
 * - Can be killed by player attacks
 */
class Enemy(
    x: Float,
    y: Float,
    enemyType: EnemyType = EnemyType.SLIME
) : Entity(x, y, enemyType.size, enemyType.size) {

    override val color: Color = enemyType.color

    // AI state
    private var state: AIState = AIState.Idle
    private var stateTimer: Float = 0f

    // Wander behavior
    private var wanderDirX: Float = 0f
    private var wanderDirY: Float = 0f
    private var wanderTime: Float = 0f

    // Combat
    private var attackCooldown: Float = 0f
    val detectionRange: Float = enemyType.detectionRange
    val attackRange: Float = enemyType.attackRange
    val attackDamage: Float = enemyType.damage
    val attackCooldownTime: Float = enemyType.attackSpeed

    init {
        maxHealth = enemyType.health
        health = maxHealth
        baseSpeed = enemyType.speed
    }

    override fun update(delta: Float, world: World) {
        if (isDead) return

        stateTimer += delta
        if (attackCooldown > 0) attackCooldown -= delta

        // Find player
        val player = world.getEntities()
            .filterIsInstance<Player>()
            .firstOrNull { !it.isDead }

        // Update AI state
        updateAIState(player)

        // Execute behavior based on state
        when (val currentState = state) {
            is AIState.Idle -> updateIdle(delta)
            is AIState.Wandering -> updateWandering(delta, world)
            is AIState.Chasing -> updateChasing(delta, world, currentState.target)
            is AIState.Attacking -> updateAttacking(delta, currentState.target)
            is AIState.Fleeing -> updateFleeing(delta, world, player)
        }

        world.updateEntityChunk(this)
    }

    private fun updateAIState(player: Player?) {
        if (player == null || player.isDead) {
            if (state !is AIState.Wandering && state !is AIState.Idle) {
                transitionTo(AIState.Idle)
            }
            return
        }

        val distanceToPlayer = distanceTo(player)

        // State transitions based on distance
        when {
            distanceToPlayer <= attackRange -> transitionTo(AIState.Attacking(player))
            distanceToPlayer <= detectionRange -> transitionTo(AIState.Chasing(player))
            state is AIState.Chasing || state is AIState.Attacking -> transitionTo(AIState.Idle)
        }
    }

    private fun transitionTo(newState: AIState) {
        if (state::class != newState::class) {
            state = newState
            stateTimer = 0f

            // Initialize state-specific behavior
            when (newState) {
                is AIState.Wandering -> {
                    // Pick random direction
                    val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
                    wanderDirX = kotlin.math.cos(angle)
                    wanderDirY = kotlin.math.sin(angle)
                    wanderTime = Random.nextFloat() * 2f + 1f
                }
                else -> {}
            }
        }
    }

    private fun updateIdle(delta: Float) {
        // After idling for a while, start wandering
        if (stateTimer > 2f) {
            transitionTo(AIState.Wandering)
        }
    }

    private fun updateWandering(delta: Float, world: World) {
        wanderTime -= delta

        if (wanderTime <= 0) {
            transitionTo(AIState.Idle)
            return
        }

        val actualSpeed = baseSpeed * 0.5f * delta
        move(wanderDirX * actualSpeed, wanderDirY * actualSpeed, world)
    }

    private fun updateChasing(delta: Float, world: World, target: Entity) {
        // Move towards target
        val dx = target.x - x
        val dy = target.y - y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > 0) {
            val normalizedDx = dx / distance
            val normalizedDy = dy / distance

            val actualSpeed = baseSpeed * delta
            move(normalizedDx * actualSpeed, normalizedDy * actualSpeed, world)
        }
    }

    private fun updateAttacking(delta: Float, target: Entity) {
        if (attackCooldown <= 0) {
            performAttack(target)
        }
    }

    private fun performAttack(target: Entity) {
        attackCooldown = attackCooldownTime

        val event = AttackEvent(this, target, attackDamage)
        AxiomGame.instance.eventBus.emit(event)

        if (!event.cancelled) {
            target.takeDamage(attackDamage, this)
        }
    }

    private fun updateFleeing(delta: Float, world: World, player: Player?) {
        if (player == null) {
            transitionTo(AIState.Idle)
            return
        }

        // Move away from player
        val dx = x - player.x
        val dy = y - player.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > 0) {
            val normalizedDx = dx / distance
            val normalizedDy = dy / distance

            val actualSpeed = baseSpeed * delta
            move(normalizedDx * actualSpeed, normalizedDy * actualSpeed, world)
        }
    }

    override fun render(shapeRenderer: ShapeRenderer) {
        // Draw enemy body
        shapeRenderer.color = color
        shapeRenderer.rect(x - width / 2, y - height / 2, width, height)

        // Draw health bar if damaged
        if (health < maxHealth) {
            val healthBarWidth = width + 8f
            val healthBarHeight = 3f
            val healthBarY = y + height / 2 + 2f

            shapeRenderer.color = Color.RED
            shapeRenderer.rect(x - healthBarWidth / 2, healthBarY, healthBarWidth, healthBarHeight)

            shapeRenderer.color = Color.GREEN
            val healthPercent = health / maxHealth
            shapeRenderer.rect(x - healthBarWidth / 2, healthBarY, healthBarWidth * healthPercent, healthBarHeight)
        }

        // Visual indicator of state (for debugging)
        when (state) {
            is AIState.Chasing -> {
                shapeRenderer.color = Color(1f, 0.5f, 0f, 0.3f)
                shapeRenderer.circle(x, y, 8f)
            }
            is AIState.Attacking -> {
                shapeRenderer.color = Color(1f, 0f, 0f, 0.5f)
                shapeRenderer.circle(x, y, 10f)
            }
            else -> {}
        }
    }
}

/**
 * Factory enum for different enemy types.
 *
 * Design Pattern: Factory (via enum)
 * - Centralizes enemy configuration
 * - Easy to add new enemy types
 * - Provides consistent enemy creation
 */
enum class EnemyType(
    val displayName: String,
    val color: Color,
    val health: Float,
    val damage: Float,
    val speed: Float,
    val size: Float,
    val detectionRange: Float,
    val attackRange: Float,
    val attackSpeed: Float
) {
    SLIME(
        displayName = "Slime",
        color = Color(0.3f, 0.8f, 0.3f, 1f),
        health = 30f,
        damage = 5f,
        speed = 60f,
        size = 20f,
        detectionRange = 150f,
        attackRange = 25f,
        attackSpeed = 1.5f
    ),

    SKELETON(
        displayName = "Skeleton",
        color = Color(0.9f, 0.9f, 0.85f, 1f),
        health = 50f,
        damage = 10f,
        speed = 80f,
        size = 24f,
        detectionRange = 200f,
        attackRange = 30f,
        attackSpeed = 1.2f
    ),

    ORC(
        displayName = "Orc",
        color = Color(0.4f, 0.6f, 0.3f, 1f),
        health = 80f,
        damage = 15f,
        speed = 70f,
        size = 30f,
        detectionRange = 180f,
        attackRange = 35f,
        attackSpeed = 1.0f
    ),

    DEMON(
        displayName = "Demon",
        color = Color(0.8f, 0.2f, 0.2f, 1f),
        health = 120f,
        damage = 20f,
        speed = 90f,
        size = 32f,
        detectionRange = 250f,
        attackRange = 40f,
        attackSpeed = 0.8f
    )
}
