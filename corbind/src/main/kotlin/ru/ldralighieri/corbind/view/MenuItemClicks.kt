@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.MenuItem
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on [MenuItem] click events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 * @param action An action to perform
 */
fun MenuItem.clicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItem) -> Boolean = AlwaysTrue,
        action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Perform an action on [MenuItem] click events inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 * @param action An action to perform
 */
suspend fun MenuItem.clicks(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItem) -> Boolean = AlwaysTrue,
        action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits on [MenuItem] click events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 */
@CheckResult
fun MenuItem.clicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItem) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setOnMenuItemClickListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits on [MenuItem] click events.
 *
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 */
@CheckResult
fun MenuItem.clicks(
    handled: (MenuItem) -> Boolean = AlwaysTrue
): Flow<MenuItem> = channelFlow {
    setOnMenuItemClickListener(listener(this, handled, ::offer))
    awaitClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of [MenuItem] click events.
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MenuItem) -> Boolean,
        emitter: (MenuItem) -> Boolean
) = MenuItem.OnMenuItemClickListener { item ->

    if (scope.isActive) {
        if (handled(item)) {
            emitter(item)
            return@OnMenuItemClickListener true
        }
    }

    return@OnMenuItemClickListener false
}
