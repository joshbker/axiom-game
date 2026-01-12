package me.josh.axiom.world

import me.josh.axiom.entity.Entity

/**
 * A chunk represents a fixed-size section of the world.
 *
 * The world is divided into chunks to enable:
 * - Efficient rendering (only draw visible chunks)
 * - Memory management (unload distant chunks)
 * - Spatial partitioning for collision detection
 *
 * Chunk coordinates are separate from world coordinates:
 * - World position = chunk position * CHUNK_SIZE + local position
 */
class Chunk(
    val chunkX: Int,
    val chunkY: Int
) {
    companion object {
        const val SIZE = 16 // 16x16 tiles per chunk
        const val TILE_SIZE = 32f // Pixels per tile
    }

    // 2D array of tiles [x][y]
    private val tiles: Array<Array<Tile>> = Array(SIZE) { x ->
        Array(SIZE) { y ->
            // Initialize with grass - will be overwritten by generator
            Tile(TileType.GRASS, chunkX * SIZE + x, chunkY * SIZE + y)
        }
    }

    // Entities currently within this chunk
    private val entities = mutableSetOf<Entity>()

    /**
     * World X coordinate of this chunk's origin (bottom-left corner).
     */
    val worldX: Float get() = chunkX * SIZE * TILE_SIZE

    /**
     * World Y coordinate of this chunk's origin (bottom-left corner).
     */
    val worldY: Float get() = chunkY * SIZE * TILE_SIZE

    /**
     * Get a tile at local chunk coordinates.
     */
    fun getTile(localX: Int, localY: Int): Tile? {
        if (localX !in 0 until SIZE || localY !in 0 until SIZE) {
            return null
        }
        return tiles[localX][localY]
    }

    /**
     * Set a tile at local chunk coordinates.
     */
    fun setTile(localX: Int, localY: Int, tile: Tile) {
        if (localX in 0 until SIZE && localY in 0 until SIZE) {
            tiles[localX][localY] = tile
        }
    }

    /**
     * Set tile by type at local coordinates.
     */
    fun setTileType(localX: Int, localY: Int, type: TileType) {
        if (localX in 0 until SIZE && localY in 0 until SIZE) {
            val worldTileX = chunkX * SIZE + localX
            val worldTileY = chunkY * SIZE + localY
            tiles[localX][localY] = Tile(type, worldTileX, worldTileY)
        }
    }

    /**
     * Add an entity to this chunk's tracking.
     */
    fun addEntity(entity: Entity) {
        entities.add(entity)
    }

    /**
     * Remove an entity from this chunk's tracking.
     */
    fun removeEntity(entity: Entity) {
        entities.remove(entity)
    }

    /**
     * Get all entities in this chunk.
     */
    fun getEntities(): Set<Entity> = entities.toSet()

    /**
     * Check if a world position is within this chunk.
     */
    fun containsWorldPosition(worldPosX: Float, worldPosY: Float): Boolean {
        return worldPosX >= worldX && worldPosX < worldX + SIZE * TILE_SIZE &&
                worldPosY >= worldY && worldPosY < worldY + SIZE * TILE_SIZE
    }

    /**
     * Get the tile at a world position.
     */
    fun getTileAtWorldPos(worldPosX: Float, worldPosY: Float): Tile? {
        val localX = ((worldPosX - worldX) / TILE_SIZE).toInt()
        val localY = ((worldPosY - worldY) / TILE_SIZE).toInt()
        return getTile(localX, localY)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Chunk) return false
        return chunkX == other.chunkX && chunkY == other.chunkY
    }

    override fun hashCode(): Int {
        return 31 * chunkX + chunkY
    }

    override fun toString(): String = "Chunk($chunkX, $chunkY)"
}
