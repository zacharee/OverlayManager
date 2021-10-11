package tk.zwander.overlaymanager.util

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.Toast
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
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
    if (tooltipText != null) {
        val tooltip = Tooltip.Builder(context)
            .anchor(this)
            .text(tooltipText!!)
            .arrow(false)
            .showDuration(4000L)
            .closePolicy(ClosePolicy.TOUCH_ANYWHERE_NO_CONSUME)

        setOnLongClickListener {
            tooltip.create().show(rootView, Tooltip.Gravity.TOP)
            true
        }
    }
}

fun OverlayInfo.createEnabledUpdate(toEnabled: Boolean, extraAction: (() -> Unit)? = null): Pair<String, BatchedUpdate> {
    val key = "${idStringOrPackageName}_enabled"
    return key to BatchedUpdate(key) {
        if (isEnabled != toEnabled) {
            if (identifier != null) {
                it.setOverlayEnabledByIdentifier(idStringOrPackageName, toEnabled)
            } else {
                it.setOverlayEnabled(idStringOrPackageName, toEnabled)
            }

            updateInstance((if (identifier != null) it.getOverlayInfoByIdentifier(idStringOrPackageName)
                            else it.getOverlayInfo(idStringOrPackageName)))

            extraAction?.invoke()
        }
    }
}

fun OverlayInfo.createPriorityUpdate(high: Boolean, extraAction: (() -> Unit)? = null): Pair<String, BatchedUpdate> {
    val key = "${idStringOrPackageName}_priority"
    return key to BatchedUpdate(key) {
        if (high) {
            it.setOverlayHighestPriority(idStringOrPackageName)
        } else {
            it.setOverlayLowestPriority(idStringOrPackageName)
        }

        updateInstance(it.getOverlayInfo(idStringOrPackageName))

        extraAction?.invoke()
    }
}

val shizukuAvailable: Boolean
    get() = Shizuku.pingBinder()

val Context.shizukuGranted: Boolean
    get() = shizukuAvailable && (if (Shizuku.isPreV11() && Shizuku.getVersion() < 11) {
        checkCallingOrSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
    } else {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    })

fun Activity.requestShizukuPermission(code: Int, listener: Shizuku.OnRequestPermissionResultListener) {
    if (Shizuku.getVersion() >= 11 && !Shizuku.isPreV11()) {
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(code)
    } else {
        requestPermissions(arrayOf(ShizukuProvider.PERMISSION), code)
    }
}

//Safely launch a URL.
//If no matching Activity is found, silently fail.
fun Context.launchUrl(url: String) {
    try {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    } catch (e: Exception) {}
}
