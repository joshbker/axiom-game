package me.josh.axiom.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for Entity base class.
 *
 * Tests verify:
 * - Entity creation with correct initial state
 * - Entity properties are accessible
 * - Unique ID generation
 *
 * Note: Tests requiring LibGDX runtime (takeDamage/heal) are integration tests.
 */
class EntityTest : FunSpec({

    test("new entity should start with full health and alive") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.health shouldBe player.maxHealth
        player.isDead shouldBe false
    }

    test("entity should have unique ID") {
        // Arrange & Act
        val player1 = EntityFactory.createPlayer(0f, 0f)
        val player2 = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player1.id shouldNotBe player2.id
    }

    test("enemy types should have different health values") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert
        slime.maxHealth shouldBe 30f
        skeleton.maxHealth shouldBe 50f
        orc.maxHealth shouldBe 80f
        demon.maxHealth shouldBe 120f
    }

    test("player should have correct stats") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.maxHealth shouldBe 100f
        player.health shouldBe 100f
        player.baseSpeed shouldBe 200f
        player.attackDamage shouldBe 25f
        player.attackRange shouldBe 50f
        player.kills shouldBe 0
        player.survivalTime shouldBe 0f
    }

    test("entity position should be set correctly") {
        // Arrange
        val x = 123.45f
        val y = 678.90f

        // Act
        val entity = EntityFactory.createPlayer(x, y)

        // Assert
        entity.x shouldBe x
        entity.y shouldBe y
    }

    test("entity should have dimensions") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        (player.width > 0f) shouldBe true
        (player.height > 0f) shouldBe true
    }

    test("isDead should be false when health is positive") {
        // Arrange
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        (player.health > 0f) shouldBe true
        player.isDead shouldBe false
    }
})
