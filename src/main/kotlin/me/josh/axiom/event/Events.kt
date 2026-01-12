package me.josh.axiom.event

import me.josh.axiom.entity.Entity
import me.josh.axiom.world.Chunk

/**
 * Base class for all game events.
 *
 * Events are the primary mechanism for inter-component communication.
 * They can be cancelled by listeners to prevent default behavior
 * or stop propagation to subsequent listeners.
 */
abstract class GameEvent {
    var cancelled: Boolean = false
        private set

    /**
     * Cancel this event, preventing further processing.
     */
    fun cancel() {
        cancelled = true
    }
}

// ============================================
// Player Events
// ============================================

/**
 * Fired when a player successfully logs in.
 */
data class PlayerLoginEvent(
    val playerId: String,
    val playerName: String
) : GameEvent()

/**
 * Fired when a player logs out.
 */
data class PlayerLogoutEvent(
    val playerId: String
) : GameEvent()

/**
 * Fired when the player moves.
 */
data class PlayerMoveEvent(
    val oldX: Float,
    val oldY: Float,
    val newX: Float,
    val newY: Float
) : GameEvent()

// ============================================
// Entity Events
// ============================================

/**
 * Fired when an entity is spawned into the world.
 */
data class EntitySpawnEvent(
    val entity: Entity
) : GameEvent()

/**
 * Fired when an entity is removed from the world.
 */
data class EntityDespawnEvent(
    val entity: Entity
) : GameEvent()

/**
 * Fired when an entity takes damage.
 */
data class EntityDamageEvent(
    val entity: Entity,
    val damage: Float,
    val source: Entity?
) : GameEvent()

/**
 * Fired when an entity dies.
 */
data class EntityDeathEvent(
    val entity: Entity,
    val killer: Entity?
) : GameEvent()

// ============================================
// Combat Events
// ============================================

/**
 * Fired when an attack is initiated.
 */
data class AttackEvent(
    val attacker: Entity,
    val target: Entity,
    val damage: Float
) : GameEvent()

// ============================================
// World Events
// ============================================

/**
 * Fired when a chunk is loaded.
 */
data class ChunkLoadEvent(
    val chunk: Chunk
) : GameEvent()

/**
 * Fired when a chunk is unloaded.
 */
data class ChunkUnloadEvent(
    val chunk: Chunk
) : GameEvent()

// ============================================
// Game State Events
// ============================================

/**
 * Fired when a new game session starts.
 */
class GameStartEvent : GameEvent()

/**
 * Fired when the game session ends.
 */
data class GameEndEvent(
    val kills: Int,
    val survivalTime: Float
) : GameEvent()

/**
 * Fired when the game is paused.
 */
class GamePauseEvent : GameEvent()

/**
 * Fired when the game is resumed.
 */
class GameResumeEvent : GameEvent()

// ============================================
// Score Events
// ============================================

/**
 * Fired when the player's score changes.
 */
data class ScoreUpdateEvent(
    val oldScore: Int,
    val newScore: Int
) : GameEvent()
