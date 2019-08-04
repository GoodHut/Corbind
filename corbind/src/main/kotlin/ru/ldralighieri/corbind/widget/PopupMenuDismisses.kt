@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.PopupMenu
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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on `view` dismiss events.
 */
fun PopupMenu.dismisses(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnDismissListener(listener(scope, events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}

/**
 * Perform an action on `view` dismiss events inside new CoroutineScope.
 */
suspend fun PopupMenu.dismisses(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnDismissListener(listener(this, events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits on `view` dismiss events
 */
@CheckResult
fun PopupMenu.dismisses(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnDismissListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnDismissListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits on `view` dismiss events
 */
@CheckResult
fun PopupMenu.dismisses(): Flow<Unit> = channelFlow {
    setOnDismissListener(listener(this, ::offer))
    awaitClose { setOnDismissListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `view` dismiss
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = PopupMenu.OnDismissListener {
    if (scope.isActive) { emitter(Unit) }
}
