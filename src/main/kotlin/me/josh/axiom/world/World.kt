package me.josh.axiom.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import me.josh.axiom.core.AxiomGame
import me.josh.axiom.entity.Entity
import me.josh.axiom.event.ChunkLoadEvent
import me.josh.axiom.event.ChunkUnloadEvent
import kotlin.math.floor

/**
 * Manages the game world composed of chunks.
 *
 * Responsibilities:
 * - Load/unload chunks based on player position
 * - Provide tile lookups for collision detection
 * - Track all entities in the world
 * - Delegate rendering to visible chunks
 */
class World(
    private val generator: WorldGenerator
) {
    companion object {
        // How many chunks to keep loaded around the player
        const val LOAD_RADIUS = 3
    }

    // Currently loaded chunks, keyed by "chunkX,chunkY"
    private val chunks = mutableMapOf<String, Chunk>()

    // All entities in the world
    private val entities = mutableListOf<Entity>()

    /**
     * Update chunks based on a center position (usually player position).
     * Loads nearby chunks and unloads distant ones.
     */
    fun updateLoadedChunks(centerX: Float, centerY: Float) {
        val centerChunkX = floor(centerX / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt()
        val centerChunkY = floor(centerY / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt()

        // Determine which chunks should be loaded
        val requiredChunks = mutableSetOf<String>()
        for (dx in -LOAD_RADIUS..LOAD_RADIUS) {
            for (dy in -LOAD_RADIUS..LOAD_RADIUS) {
                val key = chunkKey(centerChunkX + dx, centerChunkY + dy)
                requiredChunks.add(key)

                // Load if not already loaded
                if (!chunks.containsKey(key)) {
                    loadChunk(centerChunkX + dx, centerChunkY + dy)
                }
            }
        }

        // Unload chunks that are too far away
        val chunksToUnload = chunks.keys.filter { it !in requiredChunks }
        chunksToUnload.forEach { unloadChunk(it) }
    }

    /**
     * Load a chunk at the given chunk coordinates.
     */
    private fun loadChunk(chunkX: Int, chunkY: Int) {
        val key = chunkKey(chunkX, chunkY)
        if (chunks.containsKey(key)) return

        val chunk = Chunk(chunkX, chunkY)
        generator.generateChunk(chunk)
        chunks[key] = chunk

        AxiomGame.instance.eventBus.emit(ChunkLoadEvent(chunk))
        Gdx.app.debug("World", "Loaded chunk $key")
    }

    /**
     * Unload a chunk by its key.
     */
    private fun unloadChunk(key: String) {
        val chunk = chunks.remove(key) ?: return

        // Remove entities in this chunk
        chunk.getEntities().forEach { entity ->
            entities.remove(entity)
        }

        AxiomGame.instance.eventBus.emit(ChunkUnloadEvent(chunk))
        Gdx.app.debug("World", "Unloaded chunk $key")
    }

    /**
     * Get the tile at a world position.
     */
    fun getTileAt(worldX: Float, worldY: Float): Tile? {
        val chunkX = floor(worldX / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt()
        val chunkY = floor(worldY / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt()

        val chunk = chunks[chunkKey(chunkX, chunkY)] ?: return null
        return chunk.getTileAtWorldPos(worldX, worldY)
    }

    /**
     * Check if a world position is walkable (not solid).
     */
    fun isWalkable(worldX: Float, worldY: Float): Boolean {
        val tile = getTileAt(worldX, worldY) ?: return false
        return !tile.solid
    }

    /**
     * Get the speed modifier at a world position.
     */
    fun getSpeedModifier(worldX: Float, worldY: Float): Float {
        return getTileAt(worldX, worldY)?.speedModifier ?: 1f
    }

    /**
     * Add an entity to the world.
     */
    fun addEntity(entity: Entity) {
        entities.add(entity)
        updateEntityChunk(entity)
    }

    /**
     * Remove an entity from the world.
     */
    fun removeEntity(entity: Entity) {
        entities.remove(entity)
        // Remove from chunk tracking
        for (chunk in chunks.values) {
            chunk.removeEntity(entity)
        }
    }

    /**
     * Get all entities in the world.
     */
    fun getEntities(): List<Entity> = entities.toList()

    /**
     * Update which chunk an entity belongs to.
     */
    fun updateEntityChunk(entity: Entity) {
        val chunkX = floor(entity.x / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt()
        val chunkY = floor(entity.y / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt()
        val chunk = chunks[chunkKey(chunkX, chunkY)]

        // Remove from all chunks first
        for (c in chunks.values) {
            c.removeEntity(entity)
        }

        // Add to current chunk
        chunk?.addEntity(entity)
    }

    /**
     * Get entities near a position within a radius.
     */
    fun getEntitiesNear(x: Float, y: Float, radius: Float): List<Entity> {
        val radiusSq = radius * radius
        return entities.filter { entity ->
            val dx = entity.x - x
            val dy = entity.y - y
            dx * dx + dy * dy <= radiusSq
        }
    }

    /**
     * Render visible chunks.
     */
    fun render(shapeRenderer: ShapeRenderer, camera: OrthographicCamera) {
        // Calculate visible chunk range
        val viewLeft = camera.position.x - camera.viewportWidth / 2
        val viewRight = camera.position.x + camera.viewportWidth / 2
        val viewBottom = camera.position.y - camera.viewportHeight / 2
        val viewTop = camera.position.y + camera.viewportHeight / 2

        val minChunkX = floor(viewLeft / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt() - 1
        val maxChunkX = floor(viewRight / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt() + 1
        val minChunkY = floor(viewBottom / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt() - 1
        val maxChunkY = floor(viewTop / (Chunk.SIZE * Chunk.TILE_SIZE)).toInt() + 1

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (cx in minChunkX..maxChunkX) {
            for (cy in minChunkY..maxChunkY) {
                val chunk = chunks[chunkKey(cx, cy)] ?: continue
                renderChunk(shapeRenderer, chunk)
            }
        }

        shapeRenderer.end()
    }

    /**
     * Render a single chunk's tiles.
     */
    private fun renderChunk(shapeRenderer: ShapeRenderer, chunk: Chunk) {
        for (localX in 0 until Chunk.SIZE) {
            for (localY in 0 until Chunk.SIZE) {
                val tile = chunk.getTile(localX, localY) ?: continue
                val worldX = chunk.worldX + localX * Chunk.TILE_SIZE
                val worldY = chunk.worldY + localY * Chunk.TILE_SIZE

                shapeRenderer.color = tile.color
                shapeRenderer.rect(worldX, worldY, Chunk.TILE_SIZE, Chunk.TILE_SIZE)
            }
        }
    }

    /**
     * Generate a unique key for chunk coordinates.
     */
    private fun chunkKey(chunkX: Int, chunkY: Int): String = "$chunkX,$chunkY"

    /**
     * Clear all chunks and entities.
     */
    fun clear() {
        chunks.clear()
        entities.clear()
    }
}
