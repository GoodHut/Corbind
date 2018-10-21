package ru.ldralighieri.corbind.widget

import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------

data class AdapterViewItemLongClickEvent(
        val view: AdapterView<*>,
        val clickedView: View,
        val position: Int,
        val id: Long
)

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        scope: CoroutineScope,
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue,
        action: suspend (AdapterViewItemLongClickEvent) -> Unit
) {
    val events = scope.actor<AdapterViewItemLongClickEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onItemLongClickListener = listener(handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue,
        action: suspend (AdapterViewItemLongClickEvent) -> Unit
) = coroutineScope {
    val events = actor<AdapterViewItemLongClickEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onItemLongClickListener = listener(handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}


// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        scope: CoroutineScope,
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<AdapterViewItemLongClickEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    onItemLongClickListener = listener(handled, ::offer)
    invokeOnClose { onItemLongClickListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<AdapterViewItemLongClickEvent> = coroutineScope {

    produce<AdapterViewItemLongClickEvent>(Dispatchers.Main, Channel.CONFLATED) {
        onItemLongClickListener = listener(handled, ::offer)
        invokeOnClose { onItemLongClickListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        handled: (AdapterViewItemLongClickEvent) -> Boolean,
        emitter: (AdapterViewItemLongClickEvent) -> Boolean
) = AdapterView.OnItemLongClickListener { parent, view, position, id ->
    val event = AdapterViewItemLongClickEvent(parent, view, position, id)
    if (handled(event)) { emitter(event) } else { false }
}