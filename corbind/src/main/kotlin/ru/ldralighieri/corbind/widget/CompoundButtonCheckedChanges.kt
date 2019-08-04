@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.CompoundButton
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
 * Perform an action on checked state of `view`.
 */
fun CompoundButton.checkedChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Boolean) -> Unit
) {

    val events = scope.actor<Boolean>(Dispatchers.Main, capacity) {
        for (checked in channel) action(checked)
    }

    events.offer(isChecked)
    setOnCheckedChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}

/**
 * Perform an action on checked state of `view` inside new CoroutineScope.
 */
suspend fun CompoundButton.checkedChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Boolean) -> Unit
) = coroutineScope {

    val events = actor<Boolean>(Dispatchers.Main, capacity) {
        for (checked in channel) action(checked)
    }

    events.offer(isChecked)
    setOnCheckedChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of booleans representing the checked state of `view`.
 */
@CheckResult
fun CompoundButton.checkedChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    offer(isChecked)
    setOnCheckedChangeListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnCheckedChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of booleans representing the checked state of `view`.
 */
@CheckResult
fun CompoundButton.checkedChanges(): Flow<Boolean> = channelFlow {
    offer(isChecked)
    setOnCheckedChangeListener(listener(this, ::offer))
    awaitClose { setOnCheckedChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `view` checked state change
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Boolean) -> Boolean
) = CompoundButton.OnCheckedChangeListener { _, isChecked ->
    if (scope.isActive) { emitter(isChecked) }
}
