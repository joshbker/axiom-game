package me.josh.axiom.event

import com.badlogic.gdx.Gdx
import me.josh.axiom.api.AxiomApiClient
import me.josh.axiom.core.AxiomGame
import me.josh.axiom.entity.Player

/**
 * Central event handler that subscribes to game events and coordinates responses.
 *
 * This class demonstrates the Observer Pattern by decoupling game logic from event sources.
 * Instead of entities directly modifying game state, they emit events that this handler
 * processes, allowing for:
 * - Loose coupling between components
 * - Easy addition of new event listeners
 * - Centralized game logic coordination
 * - Better testability and maintainability
 */
class GameEventHandler(private val game: AxiomGame) {

    /**
     * Initializes all event subscriptions.
     * Call this once during game initialization.
     */
    fun initialize() {
        subscribeToEntityEvents()
        subscribeToGameStateEvents()
        subscribeToChunkEvents()
    }

    /**
     * Subscribes to entity-related events (death, damage, attacks).
     */
    private fun subscribeToEntityEvents() {
        // Handle entity deaths - track kills for players
        game.eventBus.subscribe<EntityDeathEvent> { event ->
            val killer = event.killer
            if (killer is Player) {
                killer.kills++
                Gdx.app.log("GameEventHandler", "${killer.kills} kills - killed ${event.entity::class.simpleName}")
            }
        }

        // Log combat damage for debugging
        game.eventBus.subscribe<EntityDamageEvent> { event ->
            val sourceName = event.source?.let { it::class.simpleName } ?: "Environment"
            Gdx.app.log("Combat", "${event.entity::class.simpleName} took ${event.damage} damage from $sourceName")
        }

        // Log attacks
        game.eventBus.subscribe<AttackEvent> { event ->
            if (!event.cancelled) {
                Gdx.app.log("Combat", "${event.attacker::class.simpleName} attacked ${event.target::class.simpleName} for ${event.damage} damage")
            }
        }
    }

    /**
     * Subscribes to game state events (start, end, pause, resume).
     */
    private fun subscribeToGameStateEvents() {
        // Handle game start
        game.eventBus.subscribe<GameStartEvent> { event ->
            Gdx.app.log("GameEventHandler", "Game started")
        }

        // Handle game end - save score to leaderboard
        game.eventBus.subscribe<GameEndEvent> { event ->
            Gdx.app.log("GameEventHandler", "Game ended - Kills: ${event.kills}, Time: ${event.survivalTime}s")

            val playerId = game.currentPlayerId
            if (playerId != null) {
                val score = calculateScore(event.kills, event.survivalTime)
                AxiomApiClient.submitScore(
                    kills = event.kills,
                    survivalTime = event.survivalTime,
                    score = score
                ) { success ->
                    if (success) {
                        Gdx.app.log("GameEventHandler", "Score saved to leaderboard: $score")
                    } else {
                        Gdx.app.log("GameEventHandler", "Failed to save score (offline mode)")
                    }
                }
            } else {
                Gdx.app.log("GameEventHandler", "No player logged in - score not saved")
            }
        }

        // Handle pause/resume
        game.eventBus.subscribe<GamePauseEvent> { event ->
            Gdx.app.log("GameEventHandler", "Game paused")
        }

        game.eventBus.subscribe<GameResumeEvent> { event ->
            Gdx.app.log("GameEventHandler", "Game resumed")
        }
    }

    /**
     * Subscribes to world chunk events (load, unload).
     */
    private fun subscribeToChunkEvents() {
        game.eventBus.subscribe<ChunkLoadEvent> { event ->
            Gdx.app.log("Chunks", "Loaded chunk (${event.chunk.chunkX}, ${event.chunk.chunkY})")
        }

        game.eventBus.subscribe<ChunkUnloadEvent> { event ->
            Gdx.app.log("Chunks", "Unloaded chunk (${event.chunk.chunkX}, ${event.chunk.chunkY})")
        }
    }

    /**
     * Subscribes to player-specific events.
     */
    private fun subscribeToPlayerEvents() {
        game.eventBus.subscribe<PlayerLoginEvent> { event ->
            Gdx.app.log("GameEventHandler", "Player logged in: ${event.playerName} (${event.playerId})")
        }

        game.eventBus.subscribe<PlayerMoveEvent> { event ->
            // Could track movement distance, detect teleportation, etc.
            // Currently disabled to reduce log spam
        }
    }

    /**
     * Calculates score based on kills and survival time.
     * Formula: kills * 100 + survivalTime * 10
     */
    private fun calculateScore(kills: Int, survivalTime: Float): Int {
        return (kills * 100) + (survivalTime * 10).toInt()
    }

    /**
     * Cleanup method to unsubscribe from events if needed.
     */
    fun dispose() {
        // EventBus.clear() is called in AxiomGame.dispose()
        Gdx.app.log("GameEventHandler", "Event handler disposed")
    }
}
