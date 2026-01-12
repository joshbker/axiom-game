package me.josh.axiom.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for EntityFactory (Factory Pattern).
 *
 * Tests verify:
 * - Player creation with correct initial state
 * - Enemy creation for each type with proper stats
 * - Random enemy generation produces valid enemy types
 * - Factory centralises entity creation logic
 */
class EntityFactoryTest : FunSpec({

    test("createPlayer should create player at specified position") {
        // Arrange
        val x = 100f
        val y = 200f

        // Act
        val player = EntityFactory.createPlayer(x, y)

        // Assert
        player.shouldBeInstanceOf<Player>()
        player.x shouldBe x
        player.y shouldBe y
        player.isDead shouldBe false
        player.health shouldBe player.maxHealth
    }

    test("createEnemy should create slime with correct stats") {
        // Arrange
        val x = 50f
        val y = 75f

        // Act
        val enemy = EntityFactory.createEnemy(EnemyType.SLIME, x, y)

        // Assert
        enemy.shouldBeInstanceOf<Enemy>()
        enemy.x shouldBe x
        enemy.y shouldBe y
        enemy.maxHealth shouldBe 30f
        enemy.health shouldBe 30f
        enemy.attackDamage shouldBe 5f
        enemy.detectionRange shouldBe 150f
        enemy.attackRange shouldBe 25f
        enemy.isDead shouldBe false
    }

    test("createEnemy should create skeleton with correct stats") {
        // Arrange
        val x = 50f
        val y = 75f

        // Act
        val enemy = EntityFactory.createEnemy(EnemyType.SKELETON, x, y)

        // Assert
        enemy.maxHealth shouldBe 50f
        enemy.health shouldBe 50f
        enemy.attackDamage shouldBe 10f
        enemy.detectionRange shouldBe 200f
        enemy.attackRange shouldBe 30f
    }

    test("createEnemy should create orc with correct stats") {
        // Arrange
        val x = 50f
        val y = 75f

        // Act
        val enemy = EntityFactory.createEnemy(EnemyType.ORC, x, y)

        // Assert
        enemy.maxHealth shouldBe 80f
        enemy.health shouldBe 80f
        enemy.attackDamage shouldBe 15f
        enemy.detectionRange shouldBe 180f
        enemy.attackRange shouldBe 35f
    }

    test("createEnemy should create demon with correct stats") {
        // Arrange
        val x = 50f
        val y = 75f

        // Act
        val enemy = EntityFactory.createEnemy(EnemyType.DEMON, x, y)

        // Assert
        enemy.maxHealth shouldBe 120f
        enemy.health shouldBe 120f
        enemy.attackDamage shouldBe 20f
        enemy.detectionRange shouldBe 250f
        enemy.attackRange shouldBe 40f
    }

    test("createRandomEnemy should create valid enemy at specified position") {
        // Arrange
        val x = 150f
        val y = 250f

        // Act
        val enemy = EntityFactory.createRandomEnemy(x, y)

        // Assert
        enemy.shouldBeInstanceOf<Enemy>()
        enemy.x shouldBe x
        enemy.y shouldBe y
        enemy.isDead shouldBe false
        enemy.health shouldBe enemy.maxHealth
    }

    test("createRandomEnemy should produce different enemy types over multiple calls") {
        // Arrange
        val sampleSize = 100
        val enemies = mutableListOf<Enemy>()

        // Act - Create many random enemies
        repeat(sampleSize) {
            enemies.add(EntityFactory.createRandomEnemy(0f, 0f))
        }

        // Assert - Should have variety of enemy types (very unlikely to get only one type)
        val uniqueHealthValues = enemies.map { it.maxHealth }.toSet()
        (uniqueHealthValues.size > 1) shouldBe true
    }

    test("createRandomEnemy should respect weighted probabilities for slimes") {
        // Arrange
        val sampleSize = 1000
        val enemies = mutableListOf<Enemy>()

        // Act
        repeat(sampleSize) {
            enemies.add(EntityFactory.createRandomEnemy(0f, 0f))
        }

        // Assert - Count enemy types (slimes should be ~50%)
        val slimeCount = enemies.count { it.maxHealth == 30f } // Slime health
        val slimePercentage = slimeCount.toDouble() / sampleSize

        // Allow 10% margin of error (40%-60% range for 50% expected)
        (slimePercentage > 0.40) shouldBe true
        (slimePercentage < 0.60) shouldBe true
    }

    test("multiple createPlayer calls should produce independent instances") {
        // Arrange & Act
        val player1 = EntityFactory.createPlayer(0f, 0f)
        val player2 = EntityFactory.createPlayer(100f, 100f)

        // Assert
        (player1 !== player2) shouldBe true
        player1.x shouldNotBe player2.x
        player1.y shouldNotBe player2.y
    }

    test("multiple createEnemy calls should produce independent instances") {
        // Arrange & Act
        val enemy1 = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val enemy2 = EntityFactory.createEnemy(EnemyType.SLIME, 50f, 50f)

        // Assert
        (enemy1 !== enemy2) shouldBe true
        enemy1.x shouldNotBe enemy2.x
        enemy1.y shouldNotBe enemy2.y
    }

})
