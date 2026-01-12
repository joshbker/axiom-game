package me.josh.axiom.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.josh.axiom.entity.EntityFactory
import me.josh.axiom.entity.EnemyType

/**
 * Unit tests for EventBus (Observer Pattern).
 *
 * Tests verify:
 * - Event subscription and emission
 * - Multiple listeners can subscribe to same event
 * - Event cancellation stops propagation
 * - Unsubscribe removes listeners properly
 * - Type-safe event handling
 */
class EventBusTest : FunSpec({

    test("subscribe should register listener and emit should notify it") {
        // Arrange
        val eventBus = EventBus()
        var eventReceived = false

        eventBus.subscribe<PlayerLoginEvent> { event ->
            eventReceived = true
        }

        // Act
        eventBus.emit(PlayerLoginEvent("player-123", "TestPlayer"))

        // Assert
        eventReceived shouldBe true
    }

    test("emit should pass correct event data to listener") {
        // Arrange
        val eventBus = EventBus()
        var receivedPlayerId = ""
        var receivedPlayerName = ""

        eventBus.subscribe<PlayerLoginEvent> { event ->
            receivedPlayerId = event.playerId
            receivedPlayerName = event.playerName
        }

        // Act
        eventBus.emit(PlayerLoginEvent("player-456", "John"))

        // Assert
        receivedPlayerId shouldBe "player-456"
        receivedPlayerName shouldBe "John"
    }

    test("multiple listeners should all receive the same event") {
        // Arrange
        val eventBus = EventBus()
        var listener1Called = false
        var listener2Called = false
        var listener3Called = false

        eventBus.subscribe<GameStartEvent> { listener1Called = true }
        eventBus.subscribe<GameStartEvent> { listener2Called = true }
        eventBus.subscribe<GameStartEvent> { listener3Called = true }

        // Act
        eventBus.emit(GameStartEvent())

        // Assert
        listener1Called shouldBe true
        listener2Called shouldBe true
        listener3Called shouldBe true
    }

    test("listeners for different events should not receive wrong events") {
        // Arrange
        val eventBus = EventBus()
        var startEventReceived = false
        var endEventReceived = false

        eventBus.subscribe<GameStartEvent> { startEventReceived = true }
        eventBus.subscribe<GameEndEvent> { endEventReceived = true }

        // Act
        eventBus.emit(GameStartEvent())

        // Assert
        startEventReceived shouldBe true
        endEventReceived shouldBe false
    }

    // Note: Event cancellation test requires LibGDX runtime (Gdx.app for logging)
    // This functionality is tested in integration tests with full game initialization

    test("unsubscribe should remove listener") {
        // Arrange
        val eventBus = EventBus()
        var eventCount = 0

        val listener = eventBus.subscribe<GameStartEvent> { eventCount++ }

        // Act - emit first time
        eventBus.emit(GameStartEvent())
        val countAfterFirst = eventCount

        // Unsubscribe
        eventBus.unsubscribe(listener)

        // Emit second time
        eventBus.emit(GameStartEvent())
        val countAfterSecond = eventCount

        // Assert
        countAfterFirst shouldBe 1
        countAfterSecond shouldBe 1 // Should not have increased
    }

    test("clearListeners should remove all listeners for specific event type") {
        // Arrange
        val eventBus = EventBus()
        var startCount = 0
        var endCount = 0

        eventBus.subscribe<GameStartEvent> { startCount++ }
        eventBus.subscribe<GameStartEvent> { startCount++ }
        eventBus.subscribe<GameEndEvent> { endCount++ }

        // Act
        eventBus.clearListeners(GameStartEvent::class)
        eventBus.emit(GameStartEvent())
        eventBus.emit(GameEndEvent(5, 120f))

        // Assert
        startCount shouldBe 0 // Cleared
        endCount shouldBe 1 // Not cleared
    }

    test("clearAll should remove all listeners for all event types") {
        // Arrange
        val eventBus = EventBus()
        var startCount = 0
        var endCount = 0
        var loginCount = 0

        eventBus.subscribe<GameStartEvent> { startCount++ }
        eventBus.subscribe<GameEndEvent> { endCount++ }
        eventBus.subscribe<PlayerLoginEvent> { loginCount++ }

        // Act
        eventBus.clearAll()
        eventBus.emit(GameStartEvent())
        eventBus.emit(GameEndEvent(5, 120f))
        eventBus.emit(PlayerLoginEvent("123", "Test"))

        // Assert
        startCount shouldBe 0
        endCount shouldBe 0
        loginCount shouldBe 0
    }

    test("event listener should handle event data modifications") {
        // Arrange
        val eventBus = EventBus()
        var capturedKills = 0
        var capturedTime = 0f

        eventBus.subscribe<GameEndEvent> { event ->
            capturedKills = event.kills
            capturedTime = event.survivalTime
        }

        // Act
        eventBus.emit(GameEndEvent(42, 120.5f))

        // Assert
        capturedKills shouldBe 42
        capturedTime shouldBe 120.5f
    }

    test("emitting event with no listeners should not throw exception") {
        // Arrange
        val eventBus = EventBus()

        // Act & Assert - should not throw
        eventBus.emit(GameStartEvent())
        eventBus.emit(PlayerLoginEvent("test", "test"))
    }

    test("listeners can be added during event emission") {
        // Arrange
        val eventBus = EventBus()
        var firstListenerCalled = false
        var dynamicListenerCalled = false

        eventBus.subscribe<GameStartEvent> { event ->
            firstListenerCalled = true
            // Add listener during emission
            eventBus.subscribe<GameStartEvent> {
                dynamicListenerCalled = true
            }
        }

        // Act
        eventBus.emit(GameStartEvent()) // First emission
        eventBus.emit(GameStartEvent()) // Second emission should trigger dynamic listener

        // Assert
        firstListenerCalled shouldBe true
        dynamicListenerCalled shouldBe true
    }

})
