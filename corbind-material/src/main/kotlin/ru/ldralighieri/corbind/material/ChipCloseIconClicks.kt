package ru.ldralighieri.corbind.material

import android.view.View
import androidx.annotation.CheckResult
import com.google.android.material.chip.Chip
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




/**
 * Perform an action on [Chip] close icon click events.
 *
 * *Warning:* The created actor uses [Chip.setOnCloseIconClickListener] to emmit clicks. Only one
 * actor can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Chip.closeIconClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnCloseIconClickListener(listener(scope, events::offer))
    events.invokeOnClose { setOnCloseIconClickListener(null) }
}

/**
 * Perform an action on [Chip] close icon click events inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [Chip.setOnCloseIconClickListener] to emmit clicks. Only one
 * actor can be used for a view at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Chip.closeIconClicks(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnCloseIconClickListener(listener(this, events::offer))
    events.invokeOnClose { setOnCloseIconClickListener(null) }
}




/**
 * Create a channel which emits on [Chip] close icon click events.
 *
 * *Warning:* The created channel uses [Chip.setOnCloseIconClickListener] to emmit clicks. Only
 * one channel can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Chip.clicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnCloseIconClickListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnClickListener(null) }
}





/**
 * Create a flow which emits on [Chip] close icon click events.
 *
 * *Warning:* The created flow uses [Chip.setOnCloseIconClickListener] to emmit clicks. Only one
 * flow can be used for a view at a time.
 */
@CheckResult
fun Chip.clicks(): Flow<Unit> = channelFlow {
    setOnCloseIconClickListener(listener(this, ::offer))
    awaitClose { setOnClickListener(null) }
}





@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = View.OnClickListener {
    if (scope.isActive) { emitter(Unit) }
}
