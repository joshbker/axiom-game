package me.josh.axiom.event

import com.badlogic.gdx.Gdx
import kotlin.reflect.KClass

/**
 * Central event bus implementing the Observer pattern.
 *
 * Provides decoupled communication between game components.
 * Components can emit events without knowing who listens,
 * and listeners can react without knowing the event source.
 *
 * Design Pattern: Observer
 * - Promotes loose coupling between event producers and consumers
 * - Enables extensibility (new listeners can be added without modifying emitters)
 * - Foundation for potential plugin/API system
 */
class EventBus {

    // Map of event types to their registered listeners
    private val listeners = mutableMapOf<KClass<out GameEvent>, MutableList<EventListener<*>>>()

    /**
     * Register a listener for a specific event type.
     * Uses Kotlin reified types for type-safe event subscription.
     */
    inline fun <reified T : GameEvent> subscribe(noinline handler: (T) -> Unit): EventListener<T> {
        return subscribe(T::class, handler)
    }

    /**
     * Register a listener with explicit type parameter.
     */
    fun <T : GameEvent> subscribe(eventType: KClass<T>, handler: (T) -> Unit): EventListener<T> {
        val listener = EventListener(eventType, handler)
        listeners.getOrPut(eventType) { mutableListOf() }.add(listener)
        return listener
    }

    /**
     * Remove a previously registered listener.
     */
    fun <T : GameEvent> unsubscribe(listener: EventListener<T>) {
        listeners[listener.eventType]?.remove(listener)
    }

    /**
     * Emit an event to all registered listeners.
     * Events can be cancelled by listeners to prevent further propagation.
     */
    fun <T : GameEvent> emit(event: T) {
        val eventListeners = listeners[event::class] ?: return

        for (listener in eventListeners.toList()) { // toList() to avoid ConcurrentModification
            if (event.cancelled) {
                Gdx.app.debug("EventBus", "Event ${event::class.simpleName} was cancelled")
                break
            }

            @Suppress("UNCHECKED_CAST")
            (listener as EventListener<T>).handler(event)
        }
    }

    /**
     * Remove all listeners for a specific event type.
     */
    fun <T : GameEvent> clearListeners(eventType: KClass<T>) {
        listeners.remove(eventType)
    }

    /**
     * Remove all listeners.
     */
    fun clearAll() {
        listeners.clear()
    }
}

/**
 * Wrapper for event handler functions.
 * Stores the event type for proper unsubscription.
 */
data class EventListener<T : GameEvent>(
    val eventType: KClass<T>,
    val handler: (T) -> Unit
)
