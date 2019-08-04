@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.RatingBar
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
 * Perform an action on rating changes on `view`.
 */
fun RatingBar.ratingChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Float) -> Unit
) {

    val events = scope.actor<Float>(Dispatchers.Main, capacity) {
        for (rating in channel) action(rating)
    }

    events.offer(rating)
    onRatingBarChangeListener = listener(scope, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}

/**
 * Perform an action on rating changes on `view` inside new CoroutineScope.
 */
suspend fun RatingBar.ratingChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Float) -> Unit
) = coroutineScope {

    val events = actor<Float>(Dispatchers.Main, capacity) {
        for (rating in channel) action(rating)
    }

    events.offer(rating)
    onRatingBarChangeListener = listener(this, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a change of the rating changes on `view`.
 */
@CheckResult
fun RatingBar.ratingChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Float> = corbindReceiveChannel(capacity) {
    safeOffer(rating)
    onRatingBarChangeListener = listener(scope, ::safeOffer)
    invokeOnClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of the rating changes on `view`.
 */
@CheckResult
fun RatingBar.ratingChanges(): Flow<Float> = channelFlow {
    offer(rating)
    onRatingBarChangeListener = listener(this, ::offer)
    awaitClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `view` rating changes
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Float) -> Boolean
) = RatingBar.OnRatingBarChangeListener { _, rating, _ ->
    if (scope.isActive) { emitter(rating) }
}
