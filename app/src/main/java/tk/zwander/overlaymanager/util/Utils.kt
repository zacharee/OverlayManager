package tk.zwander.overlaymanager.util

import android.animation.LayoutTransition
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import it.sephiroth.android.library.xtooltip.Tooltip
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import tk.zwander.overlaymanager.App
import tk.zwander.overlaymanager.IRootBridge
import tk.zwander.overlaymanager.data.BatchedUpdate
import tk.zwander.overlaymanager.proxy.OverlayInfo
import kotlin.coroutines.coroutineContext
import kotlin.jvm.functions.FunctionN

val Context.app: App
    get() = applicationContext as App

val layoutTransition = LayoutTransition().apply {
    enableTransitionType(LayoutTransition.CHANGING)
}

fun View.addTooltip() {
    val tooltip = Tooltip.Builder(context)
        .anchor(this)
        .text(tooltipText)
        .arrow(false)
        .showDuration(4000L)
        .animationStyle(0)
        .floatingAnimation(null)

    setOnLongClickListener {
        tooltip.create().show(rootView, Tooltip.Gravity.TOP)
        true
    }
}

fun OverlayInfo.createEnabledUpdate(toEnabled: Boolean, extraAction: (() -> Unit)? = null): Pair<String, BatchedUpdate> {
    val key = "${packageName}_enabled"
    return key to BatchedUpdate(key) {
        if (isEnabled != toEnabled) {
            it.setOverlayEnabled(packageName, toEnabled)
            updateInstance(it.getOverlayInfo(packageName))

            extraAction?.invoke()
        }
    }
}

fun OverlayInfo.createPriorityUpdate(high: Boolean, extraAction: (() -> Unit)? = null): Pair<String, BatchedUpdate> {
    val key = "${packageName}_priority"
    return key to BatchedUpdate(key) {
        if (high) {
            it.setOverlayHighestPriority(packageName)
        } else {
            it.setOverlayLowestPriority(packageName)
        }

        updateInstance(it.getOverlayInfo(packageName))

        extraAction?.invoke()
    }
}