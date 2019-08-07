package ru.ldralighieri.corbind.widget

import android.widget.RadioGroup
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




/**
 * Perform an action on checked view ID changes in [RadioGroup].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RadioGroup.checkedChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (checkedId in channel) action(checkedId)
    }

    events.offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}

/**
 * Perform an action on checked view ID changes in [RadioGroup] inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RadioGroup.checkedChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (checkedId in channel) action(checkedId)
    }

    events.offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}





/**
 * Create a channel of the checked view ID changes in [RadioGroup].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RadioGroup.checkedChanges(
        capacity: Int = Channel.RENDEZVOUS,
        scope: CoroutineScope
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnCheckedChangeListener(null) }
}





/**
 * Create a flow of the checked view ID changes in [RadioGroup].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun RadioGroup.checkedChanges(): Flow<Int> = channelFlow {
    offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(this, ::offer))
    awaitClose { setOnCheckedChangeListener(null) }
}





@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = object : RadioGroup.OnCheckedChangeListener {

    private var lastChecked = -1
    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (scope.isActive && checkedId != lastChecked) {
            lastChecked = checkedId
            emitter(checkedId)
        }
    }

}
