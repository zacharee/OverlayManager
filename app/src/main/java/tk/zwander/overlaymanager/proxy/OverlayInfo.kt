package tk.zwander.overlaymanager.proxy

import android.annotation.SuppressLint
import android.os.Build
import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.lang.reflect.Method

@Suppress("UNCHECKED_CAST")
@SuppressLint("PrivateApi")
@Parcelize
class OverlayInfo(private var instance: Parcelable) : Parcelable {
    companion object {
        private val infoClass = Class.forName("android.content.om.OverlayInfo")

        const val STATE_UNKNOWN = -1
        const val STATE_MISSING_TARGET = 0
        const val STATE_NO_IDMAP = 1
        const val STATE_DISABLED = 2
        const val STATE_ENABLED = 3
        const val STATE_TARGET_UPDGRADING = 4
        const val STATE_OVERLAY_UPGRADING = 5
        const val STATE_ENABLED_STATIC = 6

        const val CATEGORY_THEME = "android.theme"

        fun stateToString(state: Int): String {
            return infoClass.getMethod("stateToString", Int::class.java)
                .invoke(null, state) as String
        }
    }

    init {
        if (instance::class.java.canonicalName != infoClass.canonicalName) {
            throw IllegalStateException("${instance::class.java.canonicalName} is not an OverlayInfo instance")
        }
    }

    val packageName: String
        get() = getField("packageName")

    val targetPackageName: String
        get() = getField("targetPackageName")

    val category: String
        get() = getField("category")

    val baseCodePath: String
        get() = getField("baseCodePath")

    val state: Int
        get() = getField("state")

    val userId: Int
        get() = getField("userId")

    val priority: Int
        get() = getField("priority")

    val isStatic: Boolean
        get() = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            !getField<Boolean>("isMutable")
        } else getField("isStatic")

    val isEnabled: Boolean
        get() = invokeMethod(getMethod("isEnabled"))

    @IgnoredOnParcel
    var showEnabled = isEnabled

    fun updateInstance(instance: Parcelable) {
        this.instance = instance
        showEnabled = isEnabled
    }

    private fun <T> getField(fieldName: String) =
            infoClass.getField(fieldName).get(instance) as T

    private fun getMethod(methodName: String, vararg args: Class<*>) =
            infoClass.getMethod(methodName, *args)

    private fun <T> invokeMethod(method: Method, vararg args: Any) =
            method.invoke(instance, *args) as T
}