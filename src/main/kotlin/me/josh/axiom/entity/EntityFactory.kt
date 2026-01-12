package me.josh.axiom.entity

/**
 * Factory for creating game entities.
 *
 * Design Pattern: Factory
 * - Centralises entity creation logic
 * - Provides consistent interface for spawning entities
 * - Hides construction complexity from client code
 * - Makes adding new entity types straightforward
 *
 * Benefits:
 * - Single responsibility: creation logic in one place
 * - Easy to modify creation behaviour (e.g., add initialization, pooling)
 * - Client code doesn't need to know concrete entity classes
 *
 * Trade-offs:
 * - Adds indirection (one more layer between client and object)
 * - For simple cases like this, might seem like over-engineering
 * - However, provides extensibility for future entity types
 */
object EntityFactory {

    /**
     * Create an enemy of the specified type at the given position.
     *
     * @param type The enemy type to create
     * @param x World X coordinate
     * @param y World Y coordinate
     * @return A new Enemy instance
     */
    fun createEnemy(type: EnemyType, x: Float, y: Float): Enemy {
        return Enemy(x, y, type)
    }

    /**
     * Create a player at the given position.
     *
     * @param x World X coordinate
     * @param y World Y coordinate
     * @return A new Player instance
     */
    fun createPlayer(x: Float, y: Float): Player {
        return Player(x, y)
    }

    /**
     * Create a random enemy type.
     * Uses weighted probabilities for varied encounters.
     *
     * @param x World X coordinate
     * @param y World Y coordinate
     * @return A new Enemy of random type
     */
    fun createRandomEnemy(x: Float, y: Float): Enemy {
        val type = when (kotlin.random.Random.nextFloat()) {
            in 0f..0.5f -> EnemyType.SLIME      // 50% - Common
            in 0.5f..0.75f -> EnemyType.SKELETON // 25% - Uncommon
            in 0.75f..0.9f -> EnemyType.ORC      // 15% - Rare
            else -> EnemyType.DEMON              // 10% - Very rare
        }
        return createEnemy(type, x, y)
    }

    /**
     * Future extension point: Create entities from configuration data.
     * This could load entity stats from JSON/database for data-driven design.
     */
    // fun createFromData(entityData: EntityData): Entity { ... }
}
