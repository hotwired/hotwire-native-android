package dev.hotwire.navigation.util

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.NavBackStackEntry
import dev.hotwire.navigation.R

fun Toolbar.displayBackButton() {
    navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_back)
}

fun Toolbar.displayBackButtonAsCloseIcon() {
    navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_close)
}

internal val NavBackStackEntry?.location: String?
    get() = this?.arguments?.getString("location")

internal fun Context.colorFromThemeAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    val attr = obtainStyledAttributes(typedValue.data, intArrayOf(attrColor))
    val attrValue = attr.getColor(0, -1)
    attr.recycle()

    return attrValue
}

internal fun Int.animateColorTo(toColor: Int, duration: Long = 150, onUpdate: (Int) -> Unit) {
    ValueAnimator.ofObject(ArgbEvaluator(), this, toColor).apply {
        this.duration = duration
        this.addUpdateListener {
            val color = it.animatedValue as Int?
            color?.let { onUpdate(color) }
        }
    }.start()
}
