package me.josh.axiom.world

import com.badlogic.gdx.graphics.Color

/**
 * Represents different types of terrain tiles.
 *
 * Each tile type has properties affecting gameplay:
 * - color: Visual representation
 * - solid: Whether entities can pass through
 * - speedModifier: Movement speed multiplier on this tile
 */
enum class TileType(
    val color: Color,
    val solid: Boolean,
    val speedModifier: Float
) {
    WATER(Color(0.2f, 0.4f, 0.8f, 1f), true, 0f),        // Impassable water
    SAND(Color(0.9f, 0.85f, 0.6f, 1f), false, 0.8f),     // Slows movement
    GRASS(Color(0.3f, 0.7f, 0.3f, 1f), false, 1f),       // Normal movement
    FOREST(Color(0.2f, 0.5f, 0.2f, 1f), false, 0.9f),    // Slightly slower
    STONE(Color(0.5f, 0.5f, 0.55f, 1f), false, 1f),      // Normal movement
    MOUNTAIN(Color(0.4f, 0.35f, 0.3f, 1f), true, 0f);    // Impassable

    companion object {
        /**
         * Determine tile type from noise values.
         *
         * Uses layered noise interpretation:
         * - elevation: Primary terrain shape
         * - moisture: Secondary biome variation
         */
        fun fromNoise(elevation: Float, moisture: Float): TileType {
            return when {
                elevation < 0.3f -> WATER
                elevation < 0.4f -> SAND
                elevation < 0.7f -> {
                    // Mid-elevation: moisture determines grass vs forest
                    if (moisture > 0.5f) FOREST else GRASS
                }
                elevation < 0.85f -> STONE
                else -> MOUNTAIN
            }
        }
    }
}

/**
 * Represents a single tile in the world grid.
 *
 * Tiles are immutable value objects - changing terrain
 * requires replacing the tile rather than mutating it.
 */
data class Tile(
    val type: TileType,
    val worldX: Int,
    val worldY: Int
) {
    val solid: Boolean get() = type.solid
    val speedModifier: Float get() = type.speedModifier
    val color: Color get() = type.color
}
