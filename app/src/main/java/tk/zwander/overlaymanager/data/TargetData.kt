package tk.zwander.overlaymanager.data

import android.graphics.drawable.Drawable
import tk.zwander.overlaymanager.proxy.OverlayInfo

data class TargetData(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val info: List<OverlayInfo>
)