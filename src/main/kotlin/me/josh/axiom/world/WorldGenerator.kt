package me.josh.axiom.world

import kotlin.math.floor
import kotlin.random.Random

/**
 * Strategy interface for world generation algorithms.
 *
 * Design Pattern: Strategy
 * - Allows swapping generation algorithms without changing World class
 * - Enables different world types (islands, continents, caves, etc.)
 * - Facilitates testing with deterministic generators
 */
interface WorldGenerator {
    /**
     * Generate terrain for a chunk.
     */
    fun generateChunk(chunk: Chunk)

    /**
     * The seed used for generation (for reproducibility).
     */
    val seed: Long
}

/**
 * Procedural world generator using layered Perlin-style noise.
 *
 * Generates organic-looking terrain by combining:
 * - Elevation noise: Determines base terrain height
 * - Moisture noise: Adds biome variation
 *
 * Uses a simple noise implementation to avoid external dependencies
 * while still producing visually interesting results.
 */
class NoiseWorldGenerator(
    override val seed: Long = Random.nextLong()
) : WorldGenerator {

    private val elevationNoise = SimplexNoise(seed)
    private val moistureNoise = SimplexNoise(seed + 1)

    // Noise parameters
    private val elevationScale = 0.02f  // Lower = larger features
    private val moistureScale = 0.03f

    override fun generateChunk(chunk: Chunk) {
        for (localX in 0 until Chunk.SIZE) {
            for (localY in 0 until Chunk.SIZE) {
                val worldTileX = chunk.chunkX * Chunk.SIZE + localX
                val worldTileY = chunk.chunkY * Chunk.SIZE + localY

                // Sample noise at world position
                val elevation = elevationNoise.sample(
                    worldTileX * elevationScale,
                    worldTileY * elevationScale
                )

                val moisture = moistureNoise.sample(
                    worldTileX * moistureScale,
                    worldTileY * moistureScale
                )

                // Convert noise (-1 to 1) to 0-1 range
                val normalizedElevation = (elevation + 1f) / 2f
                val normalizedMoisture = (moisture + 1f) / 2f

                // Determine tile type from noise values
                val tileType = TileType.fromNoise(normalizedElevation, normalizedMoisture)
                chunk.setTileType(localX, localY, tileType)
            }
        }
    }
}

/**
 * Simple Simplex-like noise implementation.
 *
 * Provides smooth, continuous noise values for procedural generation.
 * Based on gradient noise principles but simplified for this use case.
 */
class SimplexNoise(private val seed: Long) {

    private val random = Random(seed)
    private val permutation = IntArray(512)

    init {
        // Generate permutation table
        val p = IntArray(256) { it }
        p.shuffle(random)
        for (i in 0 until 512) {
            permutation[i] = p[i and 255]
        }
    }

    /**
     * Sample noise at 2D coordinates.
     * Returns value in range [-1, 1].
     */
    fun sample(x: Float, y: Float): Float {
        // Find grid cell
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()

        // Relative position in cell
        val xf = x - xi
        val yf = y - yi

        // Smooth interpolation weights
        val u = fade(xf)
        val v = fade(yf)

        // Hash coordinates of cube corners
        val aa = permutation[(permutation[xi and 255] + yi) and 255]
        val ab = permutation[(permutation[xi and 255] + yi + 1) and 255]
        val ba = permutation[(permutation[(xi + 1) and 255] + yi) and 255]
        val bb = permutation[(permutation[(xi + 1) and 255] + yi + 1) and 255]

        // Gradient values at corners
        val gradAA = grad(aa, xf, yf)
        val gradBA = grad(ba, xf - 1, yf)
        val gradAB = grad(ab, xf, yf - 1)
        val gradBB = grad(bb, xf - 1, yf - 1)

        // Interpolate
        val lerpX1 = lerp(gradAA, gradBA, u)
        val lerpX2 = lerp(gradAB, gradBB, u)

        return lerp(lerpX1, lerpX2, v)
    }

    private fun fade(t: Float): Float {
        // 6t^5 - 15t^4 + 10t^3 (smooth step)
        return t * t * t * (t * (t * 6 - 15) + 10)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + t * (b - a)
    }

    private fun grad(hash: Int, x: Float, y: Float): Float {
        // Convert low 2 bits of hash to gradient direction
        return when (hash and 3) {
            0 -> x + y
            1 -> -x + y
            2 -> x - y
            else -> -x - y
        }
    }
}

/**
 * Flat world generator for testing.
 * Generates a simple pattern of tiles.
 */
class FlatWorldGenerator(
    override val seed: Long = 0
) : WorldGenerator {

    override fun generateChunk(chunk: Chunk) {
        for (localX in 0 until Chunk.SIZE) {
            for (localY in 0 until Chunk.SIZE) {
                // Simple checkerboard-ish pattern for visibility
                val tileType = if ((localX + localY) % 4 == 0) {
                    TileType.FOREST
                } else {
                    TileType.GRASS
                }
                chunk.setTileType(localX, localY, tileType)
            }
        }
    }
}
