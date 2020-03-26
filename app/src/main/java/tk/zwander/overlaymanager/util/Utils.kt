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
import kotlin.coroutines.coroutineContext

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