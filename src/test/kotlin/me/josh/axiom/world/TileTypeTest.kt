package me.josh.axiom.world

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for TileType enum.
 *
 * Tests verify:
 * - Tile properties (solid, speedModifier) are correct
 * - Noise-to-tile mapping produces expected terrain
 * - Impassable tiles block movement
 * - Speed modifiers affect gameplay appropriately
 */
class TileTypeTest : FunSpec({

    test("WATER should be impassable") {
        // Assert
        TileType.WATER.solid shouldBe true
        TileType.WATER.speedModifier shouldBe 0f
    }

    test("MOUNTAIN should be impassable") {
        // Assert
        TileType.MOUNTAIN.solid shouldBe true
        TileType.MOUNTAIN.speedModifier shouldBe 0f
    }

    test("GRASS should be fully walkable at normal speed") {
        // Assert
        TileType.GRASS.solid shouldBe false
        TileType.GRASS.speedModifier shouldBe 1f
    }

    test("SAND should slow movement") {
        // Assert
        TileType.SAND.solid shouldBe false
        TileType.SAND.speedModifier shouldBe 0.8f
    }

    test("FOREST should slightly slow movement") {
        // Assert
        TileType.FOREST.solid shouldBe false
        TileType.FOREST.speedModifier shouldBe 0.9f
    }

    test("STONE should be walkable at normal speed") {
        // Assert
        TileType.STONE.solid shouldBe false
        TileType.STONE.speedModifier shouldBe 1f
    }

    test("fromNoise with low elevation should produce WATER") {
        // Arrange
        val elevation = 0.2f
        val moisture = 0.5f

        // Act
        val tileType = TileType.fromNoise(elevation, moisture)

        // Assert
        tileType shouldBe TileType.WATER
    }

    test("fromNoise with slightly higher elevation should produce SAND") {
        // Arrange
        val elevation = 0.35f
        val moisture = 0.5f

        // Act
        val tileType = TileType.fromNoise(elevation, moisture)

        // Assert
        tileType shouldBe TileType.SAND
    }

    test("fromNoise with mid elevation and low moisture should produce GRASS") {
        // Arrange
        val elevation = 0.5f
        val moisture = 0.4f

        // Act
        val tileType = TileType.fromNoise(elevation, moisture)

        // Assert
        tileType shouldBe TileType.GRASS
    }

    test("fromNoise with mid elevation and high moisture should produce FOREST") {
        // Arrange
        val elevation = 0.5f
        val moisture = 0.6f

        // Act
        val tileType = TileType.fromNoise(elevation, moisture)

        // Assert
        tileType shouldBe TileType.FOREST
    }

    test("fromNoise with high elevation should produce STONE") {
        // Arrange
        val elevation = 0.8f
        val moisture = 0.5f

        // Act
        val tileType = TileType.fromNoise(elevation, moisture)

        // Assert
        tileType shouldBe TileType.STONE
    }

    test("fromNoise with very high elevation should produce MOUNTAIN") {
        // Arrange
        val elevation = 0.9f
        val moisture = 0.5f

        // Act
        val tileType = TileType.fromNoise(elevation, moisture)

        // Assert
        tileType shouldBe TileType.MOUNTAIN
    }

    test("all tile types should have defined colors") {
        // Assert
        for (type in TileType.values()) {
            type.color shouldNotBe null
        }
    }

    test("only WATER and MOUNTAIN should be solid") {
        // Assert
        val solidTiles = TileType.values().filter { it.solid }
        solidTiles.size shouldBe 2
        solidTiles.contains(TileType.WATER) shouldBe true
        solidTiles.contains(TileType.MOUNTAIN) shouldBe true
    }

    test("walkable tiles should have positive speed modifiers") {
        // Assert
        val walkableTiles = TileType.values().filter { !it.solid }
        for (tile in walkableTiles) {
            (tile.speedModifier > 0f) shouldBe true
        }
    }

    test("Tile data class should store world position correctly") {
        // Arrange & Act
        val tile = Tile(TileType.GRASS, 10, 20)

        // Assert
        tile.worldX shouldBe 10
        tile.worldY shouldBe 20
        tile.type shouldBe TileType.GRASS
    }

    test("Tile should delegate properties to TileType") {
        // Arrange
        val waterTile = Tile(TileType.WATER, 0, 0)
        val grassTile = Tile(TileType.GRASS, 0, 0)

        // Assert
        waterTile.solid shouldBe TileType.WATER.solid
        waterTile.speedModifier shouldBe TileType.WATER.speedModifier
        grassTile.solid shouldBe TileType.GRASS.solid
        grassTile.speedModifier shouldBe TileType.GRASS.speedModifier
    }
})
