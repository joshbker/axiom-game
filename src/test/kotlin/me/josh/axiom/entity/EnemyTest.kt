package me.josh.axiom.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for Enemy class combat mechanics.
 *
 * Tests verify:
 * - Enemy attack damage values
 * - Enemy attack ranges
 * - Enemy detection ranges
 * - Enemy attack cooldowns
 * - Enemy movement speeds
 * - Enemy size differences
 */
class EnemyTest : FunSpec({

    test("SLIME should have correct combat stats") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)

        // Assert
        slime.attackDamage shouldBe 5f
        slime.attackRange shouldBe 25f
        slime.detectionRange shouldBe 150f
        slime.attackCooldownTime shouldBe 1.5f
    }

    test("SKELETON should have correct combat stats") {
        // Arrange & Act
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)

        // Assert
        skeleton.attackDamage shouldBe 10f
        skeleton.attackRange shouldBe 30f
        skeleton.detectionRange shouldBe 200f
        skeleton.attackCooldownTime shouldBe 1.2f
    }

    test("ORC should have correct combat stats") {
        // Arrange & Act
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)

        // Assert
        orc.attackDamage shouldBe 15f
        orc.attackRange shouldBe 35f
        orc.detectionRange shouldBe 180f
        orc.attackCooldownTime shouldBe 1.0f
    }

    test("DEMON should have correct combat stats") {
        // Arrange & Act
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert
        demon.attackDamage shouldBe 20f
        demon.attackRange shouldBe 40f
        demon.detectionRange shouldBe 250f
        demon.attackCooldownTime shouldBe 0.8f
    }

    test("all enemies should have detection range greater than attack range") {
        // Arrange & Act
        val enemies = listOf(
            EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f),
            EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f),
            EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f),
            EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)
        )

        // Assert
        for (enemy in enemies) {
            (enemy.detectionRange > enemy.attackRange) shouldBe true
        }
    }

    test("enemy damage should scale with difficulty") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert - damage increases
        (skeleton.attackDamage > slime.attackDamage) shouldBe true
        (orc.attackDamage > skeleton.attackDamage) shouldBe true
        (demon.attackDamage > orc.attackDamage) shouldBe true
    }

    test("enemy attack range should scale with difficulty") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert - attack range increases
        (skeleton.attackRange > slime.attackRange) shouldBe true
        (orc.attackRange > skeleton.attackRange) shouldBe true
        (demon.attackRange > orc.attackRange) shouldBe true
    }

    test("harder enemies should have faster attack cooldowns") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert - lower cooldown = faster attacks
        (skeleton.attackCooldownTime < slime.attackCooldownTime) shouldBe true
        (orc.attackCooldownTime < skeleton.attackCooldownTime) shouldBe true
        (demon.attackCooldownTime < orc.attackCooldownTime) shouldBe true
    }

    test("SLIME should be slowest enemy") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert
        (slime.baseSpeed < skeleton.baseSpeed) shouldBe true
        (slime.baseSpeed < orc.baseSpeed) shouldBe true
        (slime.baseSpeed < demon.baseSpeed) shouldBe true
    }

    test("DEMON should be fastest enemy") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert
        (demon.baseSpeed > slime.baseSpeed) shouldBe true
        (demon.baseSpeed > skeleton.baseSpeed) shouldBe true
        (demon.baseSpeed > orc.baseSpeed) shouldBe true
    }

    test("enemy size should increase with difficulty") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert
        slime.width shouldBe 20f
        skeleton.width shouldBe 24f
        orc.width shouldBe 30f
        demon.width shouldBe 32f

        (skeleton.width > slime.width) shouldBe true
        (orc.width > skeleton.width) shouldBe true
        (demon.width > orc.width) shouldBe true
    }

    test("enemy should be created at specified position") {
        // Arrange
        val x = 123.45f
        val y = 678.90f

        // Act
        val enemy = EntityFactory.createEnemy(EnemyType.ORC, x, y)

        // Assert
        enemy.x shouldBe x
        enemy.y shouldBe y
    }

    test("enemy should start alive") {
        // Arrange & Act
        val enemy = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)

        // Assert
        enemy.isDead shouldBe false
        enemy.health shouldBe enemy.maxHealth
    }

    test("enemy colors should be distinct") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert - each should have a unique color
        val colors = listOf(slime.color, skeleton.color, orc.color, demon.color)
        val uniqueColors = colors.toSet()
        uniqueColors.size shouldBe 4
    }

    test("DEMON should have longest detection range") {
        // Arrange & Act
        val slime = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)
        val skeleton = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val orc = EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f)
        val demon = EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f)

        // Assert
        demon.detectionRange shouldBe 250f
        (demon.detectionRange > slime.detectionRange) shouldBe true
        (demon.detectionRange > skeleton.detectionRange) shouldBe true
        (demon.detectionRange > orc.detectionRange) shouldBe true
    }

    test("all enemies should have positive dimensions") {
        // Arrange & Act
        val enemies = EnemyType.values().map {
            EntityFactory.createEnemy(it, 0f, 0f)
        }

        // Assert
        for (enemy in enemies) {
            (enemy.width > 0f) shouldBe true
            (enemy.height > 0f) shouldBe true
        }
    }
})
