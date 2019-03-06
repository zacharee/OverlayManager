package tk.zwander.overlaymanager.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import tk.zwander.overlaymanager.R

/**
 * Modified version of Android's DividerItemDecoration
 * that doesn't draw the divider after the last item
 * @see androidx.recyclerview.widget.DividerItemDecoration
 */
class DividerItemDecoration(context: Context, orientation: Int) : RecyclerView.ItemDecoration() {

    var divider: Drawable? = null
        set(drawable) {
            if (drawable == null) {
                throw IllegalArgumentException("Divider cannot be nulll")
            }

            field = drawable
        }

    /**
     * Current orientation. Either [.HORIZONTAL] or [.VERTICAL].
     */
    var orientation: Int = 0
        set(orientation) {
            if (orientation != HORIZONTAL && orientation != VERTICAL) {
                throw IllegalArgumentException(
                    "Invalid orientation. It should be either HORIZONTAL or VERTICAL"
                )
            }

            field = orientation
        }

    private val bounds = Rect()

    init {
        divider = ContextCompat.getDrawable(context, R.drawable.divider)
        this.orientation = orientation
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || divider == null) {
            return
        }
        if (orientation == VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val left: Int
        val right: Int

        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(
                left, parent.paddingTop, right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }

        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, bounds)
            val bottom = bounds.bottom + Math.round(child.translationY)
            val top = bottom - divider!!.intrinsicHeight
            divider!!.setBounds(left, top, right, bottom)
            divider!!.draw(canvas)
        }
        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val top: Int
        val bottom: Int

        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(
                parent.paddingLeft, top,
                parent.width - parent.paddingRight, bottom
            )
        } else {
            top = 0
            bottom = parent.height
        }

        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.layoutManager!!.getDecoratedBoundsWithMargins(child, bounds)
            val right = bounds.right + Math.round(child.translationX)
            val left = right - divider!!.intrinsicWidth
            divider!!.setBounds(left, top, right, bottom)
            divider!!.draw(canvas)
        }
        canvas.restore()
    }

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (divider == null) {
            outRect.set(0, 0, 0, 0)
            return
        }
        if (orientation == VERTICAL) {
            outRect.set(0, 0, 0, divider!!.intrinsicHeight)
        } else {
            outRect.set(0, 0, divider!!.intrinsicWidth, 0)
        }
    }

    companion object {
        const val HORIZONTAL = LinearLayout.HORIZONTAL
        const val VERTICAL = LinearLayout.VERTICAL
    }
}