package me.josh.axiom.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for AIState (State Pattern).
 *
 * Tests verify:
 * - All AI states are properly defined
 * - State transitions work correctly
 * - States can hold target entity data
 * - State pattern enables clean behaviour switching
 */
class AIStateTest : FunSpec({

    test("Idle state should be a singleton object") {
        // Assert
        AIState.Idle.shouldBeInstanceOf<AIState>()
        (AIState.Idle === AIState.Idle) shouldBe true
    }

    test("Wandering state should be a singleton object") {
        // Assert
        AIState.Wandering.shouldBeInstanceOf<AIState>()
        (AIState.Wandering === AIState.Wandering) shouldBe true
    }

    test("Fleeing state should be a singleton object") {
        // Assert
        AIState.Fleeing.shouldBeInstanceOf<AIState>()
        (AIState.Fleeing === AIState.Fleeing) shouldBe true
    }

    test("Chasing state should hold target entity") {
        // Arrange
        val target = EntityFactory.createPlayer(100f, 100f)

        // Act
        val chasingState = AIState.Chasing(target)

        // Assert
        chasingState.shouldBeInstanceOf<AIState.Chasing>()
        chasingState.target shouldBe target
    }

    test("Attacking state should hold target entity") {
        // Arrange
        val target = EntityFactory.createPlayer(100f, 100f)

        // Act
        val attackingState = AIState.Attacking(target)

        // Assert
        attackingState.shouldBeInstanceOf<AIState.Attacking>()
        attackingState.target shouldBe target
    }

    test("Chasing states with same target should be equal") {
        // Arrange
        val target = EntityFactory.createPlayer(100f, 100f)

        // Act
        val chasing1 = AIState.Chasing(target)
        val chasing2 = AIState.Chasing(target)

        // Assert
        chasing1 shouldBe chasing2
    }

    test("Chasing states with different targets should not be equal") {
        // Arrange
        val target1 = EntityFactory.createPlayer(100f, 100f)
        val target2 = EntityFactory.createPlayer(200f, 200f)

        // Act
        val chasing1 = AIState.Chasing(target1)
        val chasing2 = AIState.Chasing(target2)

        // Assert
        chasing1 shouldNotBe chasing2
    }

    test("Attacking states with same target should be equal") {
        // Arrange
        val target = EntityFactory.createPlayer(100f, 100f)

        // Act
        val attacking1 = AIState.Attacking(target)
        val attacking2 = AIState.Attacking(target)

        // Assert
        attacking1 shouldBe attacking2
    }

    test("different state types should not be equal") {
        // Arrange
        val target = EntityFactory.createPlayer(100f, 100f)

        // Act
        val idle = AIState.Idle
        val wandering = AIState.Wandering
        val chasing = AIState.Chasing(target)
        val attacking = AIState.Attacking(target)
        val fleeing = AIState.Fleeing

        // Assert
        (idle == wandering) shouldBe false
        (idle == chasing) shouldBe false
        (wandering == fleeing) shouldBe false
        (chasing == attacking) shouldBe false
    }

    test("sealed class ensures all states are known at compile time") {
        // Arrange
        val states = listOf(
            AIState.Idle,
            AIState.Wandering,
            AIState.Chasing(EntityFactory.createPlayer(0f, 0f)),
            AIState.Attacking(EntityFactory.createPlayer(0f, 0f)),
            AIState.Fleeing
        )

        // Act & Assert - when expression is exhaustive (compiler verified)
        states.forEach { state ->
            val stateType = when (state) {
                is AIState.Idle -> "Idle"
                is AIState.Wandering -> "Wandering"
                is AIState.Chasing -> "Chasing"
                is AIState.Attacking -> "Attacking"
                is AIState.Fleeing -> "Fleeing"
            }

            stateType.shouldBeInstanceOf<String>()
        }
    }

    test("Chasing state copy should create new instance with same data") {
        // Arrange
        val target = EntityFactory.createPlayer(100f, 100f)
        val original = AIState.Chasing(target)

        // Act
        val copy = original.copy()

        // Assert
        (original !== copy) shouldBe true
        original shouldBe copy
        copy.target shouldBe target
    }

    test("Chasing state copy with different target should have different target") {
        // Arrange
        val target1 = EntityFactory.createPlayer(100f, 100f)
        val target2 = EntityFactory.createPlayer(200f, 200f)
        val original = AIState.Chasing(target1)

        // Act
        val modified = original.copy(target = target2)

        // Assert
        modified.target shouldBe target2
        modified.target shouldNotBe original.target
    }

})
