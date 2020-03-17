package tk.zwander.overlaymanager.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton

class TextButton : AppCompatButton {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    init {
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true

        transformationMethod = null

        val value = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
//        setBackgroundResource(value.resourceId)
    }
}