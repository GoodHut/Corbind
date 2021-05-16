/*
 * Copyright 2019 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ldralighieri.corbind.material

import android.view.MenuItem
import androidx.annotation.CheckResult
import com.google.android.material.bottomnavigation.BottomNavigationView
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

/**
 * Perform an action on the selected item in [BottomNavigationView].
 *
 * *Warning:* The created actor uses [BottomNavigationView.setOnNavigationItemSelectedListener].
 * Only one actor can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun BottomNavigationView.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main.immediate, capacity) {
        for (item in channel) action(item)
    }

    setInitialValue(this, events::trySend)
    setOnNavigationItemSelectedListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnNavigationItemSelectedListener(null) }
}

/**
 * Perform an action on the selected item in [BottomNavigationView], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [BottomNavigationView.setOnNavigationItemSelectedListener].
 * Only one actor can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun BottomNavigationView.itemSelections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit
) = coroutineScope {
    itemSelections(this, capacity, action)
}

/**
 * Create a channel which emits the selected item in [BottomNavigationView].
 *
 * *Warning:* The created channel uses [BottomNavigationView.setOnNavigationItemSelectedListener].
 * Only one channel can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      bottomNavigationView.itemSelections(scope)
 *          .consumeEach { /* handle selected item */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun BottomNavigationView.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setInitialValue(this@itemSelections, ::trySend)
    setOnNavigationItemSelectedListener(listener(scope, ::trySend))
    invokeOnClose { setOnNavigationItemSelectedListener(null) }
}

/**
 * Create a flow which emits the selected item in [BottomNavigationView].
 *
 * *Warning:* The created flow uses [BottomNavigationView.setOnNavigationItemSelectedListener]. Only
 * one flow can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * bottomNavigationView.itemSelections()
 *      .onEach { /* handle selected item */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * bottomNavigationView.itemSelections()
 *      .drop(1)
 *      .onEach { /* handle selected item */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun BottomNavigationView.itemSelections(): Flow<MenuItem> = channelFlow {
    setInitialValue(this@itemSelections, ::trySend)
    setOnNavigationItemSelectedListener(listener(this, ::trySend))
    awaitClose { setOnNavigationItemSelectedListener(null) }
}

private fun setInitialValue(
    bottomNavigationView: BottomNavigationView,
    emitter: (MenuItem) -> Unit
) {
    val menu = bottomNavigationView.menu
    for (i in 0 until menu.size()) {
        val item = menu.getItem(i)
        if (item.isChecked) {
            emitter(item)
            break
        }
    }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (MenuItem) -> Unit
) = BottomNavigationView.OnNavigationItemSelectedListener {
    if (scope.isActive) { emitter(it) }
    return@OnNavigationItemSelectedListener true
}
