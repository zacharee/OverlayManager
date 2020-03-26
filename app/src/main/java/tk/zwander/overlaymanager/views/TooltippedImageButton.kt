package tk.zwander.overlaymanager.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import tk.zwander.overlaymanager.util.addTooltip

class TooltippedImageButton(context: Context, attrs: AttributeSet) : AppCompatImageButton(context, attrs) {
    init {
        addTooltip()
    }
}