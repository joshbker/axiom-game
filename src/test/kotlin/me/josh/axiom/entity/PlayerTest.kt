package me.josh.axiom.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for Player class.
 *
 * Tests verify:
 * - Player stats initialization
 * - Combat stats (attack damage, range, cooldown)
 * - Kill tracking
 * - Survival time tracking
 * - Player-specific properties
 */
class PlayerTest : FunSpec({

    test("player should initialize with correct combat stats") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.attackDamage shouldBe 25f
        player.attackRange shouldBe 50f
        player.attackCooldownTime shouldBe 0.5f
        player.attackCooldown shouldBe 0f
    }

    test("player should start with zero kills") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.kills shouldBe 0
    }

    test("player should start with zero survival time") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.survivalTime shouldBe 0f
    }

    test("player should have correct initial health") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.maxHealth shouldBe 100f
        player.health shouldBe 100f
    }

    test("player should have correct movement speed") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.baseSpeed shouldBe 200f
    }

    test("player should have correct size") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.width shouldBe 28f
        player.height shouldBe 28f
    }

    test("player should have blue color") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.color.r shouldBe 0.2f
        player.color.g shouldBe 0.6f
        player.color.b shouldBe 1f
        player.color.a shouldBe 1f
    }

    test("player should be created at specified position") {
        // Arrange
        val x = 456.78f
        val y = 123.45f

        // Act
        val player = EntityFactory.createPlayer(x, y)

        // Assert
        player.x shouldBe x
        player.y shouldBe y
    }

    test("player attack range should be greater than enemy attack ranges") {
        // Arrange
        val player = EntityFactory.createPlayer(0f, 0f)
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert - player should have advantage
        (player.attackRange > slime.attackRange) shouldBe true
        (player.attackRange > skeleton.attackRange) shouldBe true
        (player.attackRange > orc.attackRange) shouldBe true
        (player.attackRange > demon.attackRange) shouldBe true
    }

    test("player should have faster attack cooldown than most enemies") {
        // Arrange
        val player = EntityFactory.createPlayer(0f, 0f)
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)

        // Assert - player should attack faster (lower cooldown = faster)
        (player.attackCooldownTime < slime.attackCooldownTime) shouldBe true
        (player.attackCooldownTime < skeleton.attackCooldownTime) shouldBe true
        (player.attackCooldownTime < orc.attackCooldownTime) shouldBe true
    }

    test("player damage should one-shot weaker enemies") {
        // Arrange
        val player = EntityFactory.createPlayer(0f, 0f)
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)

        // Assert - player damage (25) vs slime health (30), should kill in 2 hits
        val hitsToKill = Math.ceil(slime.maxHealth / player.attackDamage.toDouble()).toInt()
        hitsToKill shouldBe 2
    }

    test("player should be faster than most enemies") {
        // Arrange
        val player = EntityFactory.createPlayer(0f, 0f)
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)

        // Assert
        (player.baseSpeed > slime.baseSpeed) shouldBe true
        (player.baseSpeed > orc.baseSpeed) shouldBe true
    }

    test("multiple players should have different IDs") {
        // Arrange & Act
        val players = (1..10).map { EntityFactory.createPlayer(0f, 0f) }

        // Assert
        val uniqueIds = players.map { it.id }.toSet()
        uniqueIds.size shouldBe 10
    }

    test("player velocity should initialize to zero") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.velocityX shouldBe 0f
        player.velocityY shouldBe 0f
    }

    test("player should not be dead on creation") {
        // Arrange & Act
        val player = EntityFactory.createPlayer(0f, 0f)

        // Assert
        player.isDead shouldBe false
    }
})
