package tk.zwander.overlaymanager.util

import android.animation.LayoutTransition
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import tk.zwander.overlaymanager.App
import kotlin.coroutines.coroutineContext

val Context.app: App
    get() = applicationContext as App

val mainScope = CoroutineScope(Dispatchers.Main)
val logicScope = CoroutineScope(Dispatchers.IO)

val layoutTransition = LayoutTransition().apply {
    enableTransitionType(LayoutTransition.CHANGING)
}