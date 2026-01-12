package me.josh.axiom.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for EnemyType enum (Factory Pattern via enum).
 *
 * Tests verify:
 * - All enemy types are defined with correct stats
 * - Enemy balance progression (harder enemies have more stats)
 * - Enemy configuration is accessible and consistent
 */
class EnemyTypeTest : FunSpec({

    test("SLIME should be weakest enemy type") {
        // Assert
        EnemyType.SLIME.health shouldBe 30f
        EnemyType.SLIME.damage shouldBe 5f
        EnemyType.SLIME.speed shouldBe 60f
        EnemyType.SLIME.detectionRange shouldBe 150f
        EnemyType.SLIME.attackRange shouldBe 25f
        EnemyType.SLIME.displayName shouldBe "Slime"
    }

    test("SKELETON should be stronger than SLIME") {
        // Assert
        EnemyType.SKELETON.health shouldBe 50f
        EnemyType.SKELETON.damage shouldBe 10f
        EnemyType.SKELETON.speed shouldBe 80f
        (EnemyType.SKELETON.health > EnemyType.SLIME.health) shouldBe true
        (EnemyType.SKELETON.damage > EnemyType.SLIME.damage) shouldBe true
    }

    test("ORC should be stronger than SKELETON") {
        // Assert
        EnemyType.ORC.health shouldBe 80f
        EnemyType.ORC.damage shouldBe 15f
        EnemyType.ORC.speed shouldBe 70f
        (EnemyType.ORC.health > EnemyType.SKELETON.health) shouldBe true
        (EnemyType.ORC.damage > EnemyType.SKELETON.damage) shouldBe true
    }

    test("DEMON should be strongest enemy type") {
        // Assert
        EnemyType.DEMON.health shouldBe 120f
        EnemyType.DEMON.damage shouldBe 20f
        EnemyType.DEMON.speed shouldBe 90f
        EnemyType.DEMON.detectionRange shouldBe 250f
        EnemyType.DEMON.attackRange shouldBe 40f
        (EnemyType.DEMON.health > EnemyType.ORC.health) shouldBe true
        (EnemyType.DEMON.damage > EnemyType.ORC.damage) shouldBe true
    }

    test("all enemy types should have positive stats") {
        // Assert
        for (type in EnemyType.values()) {
            (type.health > 0f) shouldBe true
            (type.damage > 0f) shouldBe true
            (type.speed > 0f) shouldBe true
            (type.size > 0f) shouldBe true
            (type.detectionRange > 0f) shouldBe true
            (type.attackRange > 0f) shouldBe true
            (type.attackSpeed > 0f) shouldBe true
        }
    }

    test("detection range should be greater than attack range for all types") {
        // Assert - enemies should detect before they can attack
        for (type in EnemyType.values()) {
            (type.detectionRange > type.attackRange) shouldBe true
        }
    }

    test("all enemy types should have unique colors") {
        // Assert
        val colors = EnemyType.values().map { it.color }.toSet()
        colors.size shouldBe EnemyType.values().size
    }

    test("all enemy types should have display names") {
        // Assert
        for (type in EnemyType.values()) {
            type.displayName.isNotBlank() shouldBe true
        }
    }

    test("enemy types should be ordered by difficulty") {
        // Arrange
        val types = EnemyType.values()

        // Assert - verify health progression
        types[0] shouldBe EnemyType.SLIME
        types[1] shouldBe EnemyType.SKELETON
        types[2] shouldBe EnemyType.ORC
        types[3] shouldBe EnemyType.DEMON

        // Verify health increases
        (types[1].health > types[0].health) shouldBe true
        (types[2].health > types[1].health) shouldBe true
        (types[3].health > types[2].health) shouldBe true
    }

    test("all enemy types have different sizes") {
        // Assert
        EnemyType.SLIME.size shouldBe 20f
        EnemyType.SKELETON.size shouldBe 24f
        EnemyType.ORC.size shouldBe 30f
        EnemyType.DEMON.size shouldBe 32f
    }

    test("enemy type values should be accessible") {
        // Arrange & Act
        val allTypes = EnemyType.values()

        // Assert
        allTypes.size shouldBe 4
        allTypes.contains(EnemyType.SLIME) shouldBe true
        allTypes.contains(EnemyType.SKELETON) shouldBe true
        allTypes.contains(EnemyType.ORC) shouldBe true
        allTypes.contains(EnemyType.DEMON) shouldBe true
    }
})
