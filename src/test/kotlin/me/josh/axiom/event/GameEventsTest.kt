package me.josh.axiom.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.josh.axiom.entity.EntityFactory
import me.josh.axiom.entity.EnemyType
import me.josh.axiom.world.Chunk

/**
 * Unit tests for all GameEvent types.
 *
 * Tests verify:
 * - All event types can be created
 * - Event data is stored correctly
 * - Events are instances of GameEvent
 * - Event-specific properties are accessible
 */
class GameEventsTest : FunSpec({

    test("PlayerLoginEvent should store player data") {
        // Arrange & Act
        val event = PlayerLoginEvent("user-123", "TestPlayer")

        // Assert
        event.playerId shouldBe "user-123"
        event.playerName shouldBe "TestPlayer"
        (event is GameEvent) shouldBe true
    }

    test("GameStartEvent should be created") {
        // Arrange & Act
        val event = GameStartEvent()

        // Assert
        (event is GameEvent) shouldBe true
        event.cancelled shouldBe false
    }

    test("GameEndEvent should store game stats") {
        // Arrange & Act
        val event = GameEndEvent(kills = 42, survivalTime = 123.5f)

        // Assert
        event.kills shouldBe 42
        event.survivalTime shouldBe 123.5f
        (event is GameEvent) shouldBe true
    }

    test("GamePauseEvent should be created") {
        // Arrange & Act
        val event = GamePauseEvent()

        // Assert
        (event is GameEvent) shouldBe true
    }

    test("GameResumeEvent should be created") {
        // Arrange & Act
        val event = GameResumeEvent()

        // Assert
        (event is GameEvent) shouldBe true
    }

    test("AttackEvent should store attack details") {
        // Arrange
        val attacker = EntityFactory.createPlayer(0f, 0f)
        val target = EntityFactory.createEnemy(EnemyType.SLIME, 10f, 10f)

        // Act
        val event = AttackEvent(attacker, target, 25f)

        // Assert
        event.attacker shouldBe attacker
        event.target shouldBe target
        event.damage shouldBe 25f
        (event is GameEvent) shouldBe true
    }

    test("EntityDamageEvent should store damage details") {
        // Arrange
        val entity = EntityFactory.createPlayer(0f, 0f)
        val source = EntityFactory.createEnemy(EnemyType.ORC, 5f, 5f)

        // Act
        val event = EntityDamageEvent(entity, 15f, source)

        // Assert
        event.entity shouldBe entity
        event.damage shouldBe 15f
        event.source shouldBe source
        (event is GameEvent) shouldBe true
    }

    test("EntityDeathEvent should store death details") {
        // Arrange
        val entity = EntityFactory.createEnemy(EnemyType.SKELETON, 0f, 0f)
        val killer = EntityFactory.createPlayer(10f, 10f)

        // Act
        val event = EntityDeathEvent(entity, killer)

        // Assert
        event.entity shouldBe entity
        event.killer shouldBe killer
        (event is GameEvent) shouldBe true
    }

    test("PlayerMoveEvent should store movement details") {
        // Arrange & Act
        val event = PlayerMoveEvent(100f, 200f, 110f, 220f)

        // Assert
        event.oldX shouldBe 100f
        event.oldY shouldBe 200f
        event.newX shouldBe 110f
        event.newY shouldBe 220f
        (event is GameEvent) shouldBe true
    }

    test("ChunkLoadEvent should store chunk reference") {
        // Arrange
        val chunk = Chunk(5, 10)

        // Act
        val event = ChunkLoadEvent(chunk)

        // Assert
        event.chunk shouldBe chunk
        event.chunk.chunkX shouldBe 5
        event.chunk.chunkY shouldBe 10
        (event is GameEvent) shouldBe true
    }

    test("ChunkUnloadEvent should store chunk reference") {
        // Arrange
        val chunk = Chunk(3, 7)

        // Act
        val event = ChunkUnloadEvent(chunk)

        // Assert
        event.chunk shouldBe chunk
        event.chunk.chunkX shouldBe 3
        event.chunk.chunkY shouldBe 7
        (event is GameEvent) shouldBe true
    }

    test("events should not be cancelled by default") {
        // Arrange & Act
        val events = listOf(
            GameStartEvent(),
            GameEndEvent(0, 0f),
            GamePauseEvent(),
            GameResumeEvent(),
            PlayerLoginEvent("id", "name"),
            AttackEvent(EntityFactory.createPlayer(0f, 0f), EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f), 10f),
            EntityDamageEvent(EntityFactory.createPlayer(0f, 0f), 10f, null),
            EntityDeathEvent(EntityFactory.createPlayer(0f, 0f), null),
            PlayerMoveEvent(0f, 0f, 1f, 1f),
            ChunkLoadEvent(Chunk(0, 0)),
            ChunkUnloadEvent(Chunk(0, 0))
        )

        // Assert
        for (event in events) {
            event.cancelled shouldBe false
        }
    }

    test("GameEndEvent with zero stats should be valid") {
        // Arrange & Act
        val event = GameEndEvent(kills = 0, survivalTime = 0f)

        // Assert
        event.kills shouldBe 0
        event.survivalTime shouldBe 0f
    }

    test("GameEndEvent with high stats should be valid") {
        // Arrange & Act
        val event = GameEndEvent(kills = 9999, survivalTime = 99999.9f)

        // Assert
        event.kills shouldBe 9999
        event.survivalTime shouldBe 99999.9f
    }

    test("AttackEvent with different entity types should work") {
        // Arrange
        val playerAttacksEnemy = AttackEvent(
            EntityFactory.createPlayer(0f, 0f),
            EntityFactory.createEnemy(EnemyType.DEMON, 0f, 0f),
            25f
        )

        val enemyAttacksPlayer = AttackEvent(
            EntityFactory.createEnemy(EnemyType.ORC, 0f, 0f),
            EntityFactory.createPlayer(0f, 0f),
            15f
        )

        // Assert
        playerAttacksEnemy.damage shouldBe 25f
        enemyAttacksPlayer.damage shouldBe 15f
    }

    test("EntityDamageEvent with null source should work") {
        // Arrange
        val entity = EntityFactory.createPlayer(0f, 0f)

        // Act
        val event = EntityDamageEvent(entity, 10f, null)

        // Assert
        event.source shouldBe null
        event.damage shouldBe 10f
    }

    test("EntityDeathEvent with null killer should work") {
        // Arrange
        val entity = EntityFactory.createEnemy(EnemyType.SLIME, 0f, 0f)

        // Act
        val event = EntityDeathEvent(entity, null)

        // Assert
        event.killer shouldBe null
    }

    test("PlayerMoveEvent should handle zero movement") {
        // Arrange & Act
        val event = PlayerMoveEvent(50f, 50f, 50f, 50f)

        // Assert
        event.oldX shouldBe event.newX
        event.oldY shouldBe event.newY
    }

    test("ChunkLoadEvent and ChunkUnloadEvent for same chunk should have same coordinates") {
        // Arrange
        val chunk = Chunk(12, 34)
        val loadEvent = ChunkLoadEvent(chunk)
        val unloadEvent = ChunkUnloadEvent(chunk)

        // Assert
        loadEvent.chunk.chunkX shouldBe unloadEvent.chunk.chunkX
        loadEvent.chunk.chunkY shouldBe unloadEvent.chunk.chunkY
    }
})
