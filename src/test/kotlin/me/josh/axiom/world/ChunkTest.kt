package me.josh.axiom.world

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for Chunk class.
 *
 * Tests verify:
 * - Chunk creation with coordinates
 * - Tile get/set operations
 * - World-to-local coordinate conversion
 * - Tile boundaries and grid structure
 * - Default tile initialization
 */
class ChunkTest : FunSpec({

    test("chunk should be created with correct coordinates") {
        // Arrange & Act
        val chunk = Chunk(5, 10)

        // Assert
        chunk.chunkX shouldBe 5
        chunk.chunkY shouldBe 10
    }

    test("chunk should have correct size constant") {
        // Assert
        Chunk.SIZE shouldBe 16
    }

    test("chunk should have correct tile size constant") {
        // Assert
        Chunk.TILE_SIZE shouldBe 32f
    }

    test("getTile should return null for out of bounds coordinates") {
        // Arrange
        val chunk = Chunk(0, 0)

        // Act & Assert
        chunk.getTile(-1, 0) shouldBe null
        chunk.getTile(0, -1) shouldBe null
        chunk.getTile(Chunk.SIZE, 0) shouldBe null
        chunk.getTile(0, Chunk.SIZE) shouldBe null
        chunk.getTile(999, 999) shouldBe null
    }

    test("setTileType should set tile at valid coordinates") {
        // Arrange
        val chunk = Chunk(0, 0)

        // Act
        chunk.setTileType(5, 5, TileType.FOREST)

        // Assert
        val tile = chunk.getTile(5, 5)
        tile shouldNotBe null
        tile?.type shouldBe TileType.FOREST
    }

    test("setTileType should not crash with out of bounds coordinates") {
        // Arrange
        val chunk = Chunk(0, 0)

        // Act & Assert - should not throw
        chunk.setTileType(-1, 0, TileType.GRASS)
        chunk.setTileType(0, -1, TileType.GRASS)
        chunk.setTileType(999, 999, TileType.GRASS)
    }

    test("getTile should return tile with correct world coordinates") {
        // Arrange
        val chunk = Chunk(2, 3)
        chunk.setTileType(5, 10, TileType.SAND)

        // Act
        val tile = chunk.getTile(5, 10)

        // Assert
        tile shouldNotBe null
        tile?.worldX shouldBe (2 * Chunk.SIZE + 5)
        tile?.worldY shouldBe (3 * Chunk.SIZE + 10)
    }

    test("all tiles in chunk should be accessible") {
        // Arrange
        val chunk = Chunk(0, 0)
        val generator = FlatWorldGenerator()
        generator.generateChunk(chunk)

        // Act & Assert
        for (x in 0 until Chunk.SIZE) {
            for (y in 0 until Chunk.SIZE) {
                chunk.getTile(x, y) shouldNotBe null
            }
        }
    }

    test("different chunks should have different world coordinates") {
        // Arrange
        val chunk1 = Chunk(0, 0)
        val chunk2 = Chunk(1, 1)
        chunk1.setTileType(0, 0, TileType.GRASS)
        chunk2.setTileType(0, 0, TileType.GRASS)

        // Act
        val tile1 = chunk1.getTile(0, 0)
        val tile2 = chunk2.getTile(0, 0)

        // Assert
        tile1?.worldX shouldNotBe tile2?.worldX
        tile1?.worldY shouldNotBe tile2?.worldY
    }

    test("chunk should store tiles independently") {
        // Arrange
        val chunk = Chunk(0, 0)

        // Act
        chunk.setTileType(0, 0, TileType.WATER)
        chunk.setTileType(1, 1, TileType.GRASS)
        chunk.setTileType(5, 10, TileType.FOREST)

        // Assert
        chunk.getTile(0, 0)?.type shouldBe TileType.WATER
        chunk.getTile(1, 1)?.type shouldBe TileType.GRASS
        chunk.getTile(5, 10)?.type shouldBe TileType.FOREST
        chunk.getTile(2, 2)?.type shouldNotBe TileType.WATER
    }

    test("getTileAtWorldPos should convert world coordinates correctly") {
        // Arrange
        val chunk = Chunk(2, 3)
        chunk.setTileType(5, 7, TileType.STONE)

        // Calculate world position of tile
        val worldX = (2 * Chunk.SIZE + 5) * Chunk.TILE_SIZE + 1f // +1 to be inside tile
        val worldY = (3 * Chunk.SIZE + 7) * Chunk.TILE_SIZE + 1f

        // Act
        val tile = chunk.getTileAtWorldPos(worldX, worldY)

        // Assert
        tile shouldNotBe null
        tile?.type shouldBe TileType.STONE
    }

    test("chunk corner coordinates should be calculated correctly") {
        // Arrange
        val chunk = Chunk(3, 4)

        // Act
        val tile00 = chunk.getTile(0, 0)
        val tileMax = chunk.getTile(Chunk.SIZE - 1, Chunk.SIZE - 1)

        // Assert
        tile00?.worldX shouldBe (3 * Chunk.SIZE)
        tile00?.worldY shouldBe (4 * Chunk.SIZE)
        tileMax?.worldX shouldBe (3 * Chunk.SIZE + Chunk.SIZE - 1)
        tileMax?.worldY shouldBe (4 * Chunk.SIZE + Chunk.SIZE - 1)
    }

    test("negative chunk coordinates should work correctly") {
        // Arrange
        val chunk = Chunk(-1, -2)
        chunk.setTileType(0, 0, TileType.MOUNTAIN)

        // Act
        val tile = chunk.getTile(0, 0)

        // Assert
        tile shouldNotBe null
        tile?.worldX shouldBe (-1 * Chunk.SIZE)
        tile?.worldY shouldBe (-2 * Chunk.SIZE)
    }

    test("chunk should handle full grid generation") {
        // Arrange
        val chunk = Chunk(0, 0)
        val generator = NoiseWorldGenerator(12345L)

        // Act
        generator.generateChunk(chunk)

        // Assert - every position should have a tile
        var tileCount = 0
        for (x in 0 until Chunk.SIZE) {
            for (y in 0 until Chunk.SIZE) {
                if (chunk.getTile(x, y) != null) {
                    tileCount++
                }
            }
        }
        tileCount shouldBe (Chunk.SIZE * Chunk.SIZE)
    }

    test("chunk size should allow 16x16 grid") {
        // Assert
        val expectedTiles = 256 // 16 * 16
        (Chunk.SIZE * Chunk.SIZE) shouldBe expectedTiles
    }

    test("tile size should be 32 pixels") {
        // Assert - matches game design
        Chunk.TILE_SIZE shouldBe 32f
    }

    test("chunk world size should be SIZE * TILE_SIZE") {
        // Arrange
        val expectedWorldSize = Chunk.SIZE * Chunk.TILE_SIZE

        // Assert
        expectedWorldSize shouldBe 512f // 16 * 32
    }
})
