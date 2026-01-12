package me.josh.axiom.world

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for WorldGenerator (Strategy Pattern).
 *
 * Tests verify:
 * - Different generator strategies produce different terrain
 * - NoiseWorldGenerator produces organic-looking terrain
 * - FlatWorldGenerator produces predictable patterns
 * - Same seed produces same terrain (reproducibility)
 * - Generators properly implement strategy interface
 */
class WorldGeneratorTest : FunSpec({

    test("NoiseWorldGenerator should implement WorldGenerator interface") {
        // Arrange & Act
        val generator = NoiseWorldGenerator(12345L)

        // Assert
        (generator is WorldGenerator) shouldBe true
        generator.seed shouldBe 12345L
    }

    test("FlatWorldGenerator should implement WorldGenerator interface") {
        // Arrange & Act
        val generator = FlatWorldGenerator(0L)

        // Assert
        (generator is WorldGenerator) shouldBe true
        generator.seed shouldBe 0L
    }

    test("NoiseWorldGenerator should generate chunk with tiles") {
        // Arrange
        val generator = NoiseWorldGenerator(12345L)
        val chunk = Chunk(0, 0)

        // Act
        generator.generateChunk(chunk)

        // Assert
        val centerTile = chunk.getTile(Chunk.SIZE / 2, Chunk.SIZE / 2)
        centerTile shouldNotBe null
        centerTile!!.type shouldNotBe null
    }

    test("FlatWorldGenerator should generate chunk with tiles") {
        // Arrange
        val generator = FlatWorldGenerator()
        val chunk = Chunk(0, 0)

        // Act
        generator.generateChunk(chunk)

        // Assert
        val centerTile = chunk.getTile(Chunk.SIZE / 2, Chunk.SIZE / 2)
        centerTile shouldNotBe null
        centerTile!!.type shouldNotBe null
    }

    test("NoiseWorldGenerator with same seed should produce same terrain") {
        // Arrange
        val seed = 99999L
        val generator1 = NoiseWorldGenerator(seed)
        val generator2 = NoiseWorldGenerator(seed)

        val chunk1 = Chunk(0, 0)
        val chunk2 = Chunk(0, 0)

        // Act
        generator1.generateChunk(chunk1)
        generator2.generateChunk(chunk2)

        // Assert - sample several tiles to verify consistency
        for (x in 0 until Chunk.SIZE step 4) {
            for (y in 0 until Chunk.SIZE step 4) {
                val tile1 = chunk1.getTile(x, y)
                val tile2 = chunk2.getTile(x, y)
                tile1?.type shouldBe tile2?.type
            }
        }
    }

    test("NoiseWorldGenerator with different seeds should produce different terrain") {
        // Arrange
        val generator1 = NoiseWorldGenerator(111L)
        val generator2 = NoiseWorldGenerator(222L)

        val chunk1 = Chunk(0, 0)
        val chunk2 = Chunk(0, 0)

        // Act
        generator1.generateChunk(chunk1)
        generator2.generateChunk(chunk2)

        // Assert - at least some tiles should differ
        var differenceCount = 0
        for (x in 0 until Chunk.SIZE) {
            for (y in 0 until Chunk.SIZE) {
                val tile1 = chunk1.getTile(x, y)
                val tile2 = chunk2.getTile(x, y)
                if (tile1?.type != tile2?.type) {
                    differenceCount++
                }
            }
        }

        (differenceCount > 0) shouldBe true
    }

    test("FlatWorldGenerator should produce predictable pattern") {
        // Arrange
        val generator = FlatWorldGenerator()
        val chunk = Chunk(0, 0)

        // Act
        generator.generateChunk(chunk)

        // Assert - check the pattern matches expectation (0,0) should be FOREST based on (0+0)%4==0
        val tile00 = chunk.getTile(0, 0)
        tile00?.type shouldBe TileType.FOREST

        // (1,0) -> (1+0)%4=1, should be GRASS
        val tile10 = chunk.getTile(1, 0)
        tile10?.type shouldBe TileType.GRASS
    }

    test("NoiseWorldGenerator should produce variety of tile types") {
        // Arrange
        val generator = NoiseWorldGenerator(54321L)
        val chunk = Chunk(0, 0)

        // Act
        generator.generateChunk(chunk)

        // Assert - collect all tile types generated
        val tileTypes = mutableSetOf<TileType>()
        for (x in 0 until Chunk.SIZE) {
            for (y in 0 until Chunk.SIZE) {
                chunk.getTile(x, y)?.let { tileTypes.add(it.type) }
            }
        }

        // Should have at least 2 different tile types in a chunk
        (tileTypes.size >= 2) shouldBe true
    }

    test("SimplexNoise should produce values in expected range") {
        // Arrange
        val noise = SimplexNoise(12345L)

        // Act & Assert - sample multiple points
        for (i in 0..100) {
            val x = i * 0.1f
            val y = i * 0.1f
            val value = noise.sample(x, y)

            // Noise should be in range [-1, 1]
            (value >= -1f) shouldBe true
            (value <= 1f) shouldBe true
        }
    }

    test("SimplexNoise with same seed should produce consistent values") {
        // Arrange
        val noise1 = SimplexNoise(777L)
        val noise2 = SimplexNoise(777L)

        // Act & Assert
        for (i in 0..50) {
            val x = i * 0.5f
            val y = i * 0.3f
            noise1.sample(x, y) shouldBe noise2.sample(x, y)
        }
    }

    test("SimplexNoise with different seeds should produce different values") {
        // Arrange
        val noise1 = SimplexNoise(111L)
        val noise2 = SimplexNoise(222L)

        // Act
        val value1 = noise1.sample(5f, 10f)
        val value2 = noise2.sample(5f, 10f)

        // Assert
        value1 shouldNotBe value2
    }

    test("generators should fill entire chunk with tiles") {
        // Arrange
        val noiseGen = NoiseWorldGenerator(12345L)
        val flatGen = FlatWorldGenerator()

        val chunk1 = Chunk(0, 0)
        val chunk2 = Chunk(0, 0)

        // Act
        noiseGen.generateChunk(chunk1)
        flatGen.generateChunk(chunk2)

        // Assert - every tile position should be filled
        for (x in 0 until Chunk.SIZE) {
            for (y in 0 until Chunk.SIZE) {
                chunk1.getTile(x, y) shouldNotBe null
                chunk2.getTile(x, y) shouldNotBe null
            }
        }
    }

    test("different chunk coordinates should generate different terrain with NoiseGenerator") {
        // Arrange
        val generator = NoiseWorldGenerator(42L)
        val chunk1 = Chunk(0, 0)
        val chunk2 = Chunk(1, 1)

        // Act
        generator.generateChunk(chunk1)
        generator.generateChunk(chunk2)

        // Assert - at least some tiles should differ
        val tile1 = chunk1.getTile(Chunk.SIZE / 2, Chunk.SIZE / 2)
        val tile2 = chunk2.getTile(Chunk.SIZE / 2, Chunk.SIZE / 2)

        // Not a guarantee they'll differ at center, but check overall variety
        var matchCount = 0
        for (x in 0 until Chunk.SIZE) {
            for (y in 0 until Chunk.SIZE) {
                if (chunk1.getTile(x, y)?.type == chunk2.getTile(x, y)?.type) {
                    matchCount++
                }
            }
        }

        // Chunks should not be identical
        val totalTiles = Chunk.SIZE * Chunk.SIZE
        (matchCount < totalTiles) shouldBe true
    }
})
