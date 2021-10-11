package tk.zwander.overlaymanager.proxy

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.Parcelable
import java.lang.reflect.Method

@Suppress("UNCHECKED_CAST")
@SuppressLint("PrivateApi")
class IOverlayManager {
    companion object {
        private val clazz = Class.forName("android.content.om.IOverlayManager")
        private val stubClass = Class.forName("android.content.om.IOverlayManager\$Stub")
        private val omtbClass =
            Class.forName("android.content.om.OverlayManagerTransaction\$Builder")
        private val omtClass = Class.forName("android.content.om.OverlayManagerTransaction")
        private val oiClass = Class.forName("android.content.om.OverlayIdentifier")

        fun getIdentifier(packageName: String, overlayName: String?): Any {
            return oiClass.getConstructor(
                String::class.java,
                String::class.java
            ).newInstance(packageName, overlayName)
        }

        fun getIdentifier(source: String): Any {
            return oiClass.getMethod(
                "fromString",
                String::class.java
            ).invoke(
                null,
                source
            )!!
        }
    }

    private val obj = run {
        val svcManClass = Class.forName("android.os.ServiceManager")
        val getService = svcManClass.getMethod("getService", String::class.java)
        val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)

        val b = getService.invoke(null, "overlay")

        asInterface.invoke(null, b)
    }

    fun getAllOverlays(): MutableMap<String, List<OverlayInfo>> {
        val r = invokeMethod<Map<*, *>>(getMethod("getAllOverlays", Int::class.java), -2)
        val ret = HashMap<String, List<OverlayInfo>>()

        r.forEach { (key, value) ->
            val l = ArrayList<OverlayInfo>()

            (value as List<*>).forEach {
                l.add(OverlayInfo(it as Parcelable))
            }

            ret[key as String] = l
        }

        return ret
    }

    fun getOverlayInfosForTarget(packageName: String) =
        invokeMethod<List<*>>(
            getMethod(
                "getOverlayInfosForTarget",
                String::class.java, Int::class.java
            ), packageName, -2
        ).map { OverlayInfo(it as Parcelable) }

    fun getOverlayInfo(packageName: String) =
        OverlayInfo(
            invokeMethod(
                getMethod(
                    "getOverlayInfo",
                    String::class.java, Int::class.java
                ), packageName, -2
            )
        )

    fun getOverlayInfoByIdentifier(identifier: Any) =
        OverlayInfo(
            invokeMethod(
                getMethod(
                    "getOverlayInfoByIdentifier",
                    oiClass,
                    Int::class.java
                ),
                identifier, -2
            )
        )

    fun setEnabled(packageName: String, enable: Boolean) =
        invokeMethod<Boolean>(
            getMethod("setEnabled", String::class.java, Boolean::class.java, Int::class.java),
            packageName, enable, -2
        )

    fun setEnabled(identifier: Any, enable: Boolean) {
        val builder = omtbClass.newInstance()

        omtbClass.getMethod(
            "setEnabled",
            oiClass,
            Boolean::class.java,
            Int::class.java
        ).invoke(
            builder,
            identifier,
            enable,
            -2
        )

        commit(
            omtbClass.getMethod("build")
                .invoke(builder)!!
        )
    }

    fun setEnabledExclusive(packageName: String, enable: Boolean) =
        invokeMethod<Boolean>(
            getMethod(
                "setEnabledExclusive",
                String::class.java,
                Boolean::class.java,
                Int::class.java
            ),
            packageName, enable, -2
        )

    fun setEnabledExclusiveInCategory(packageName: String) =
        invokeMethod<Boolean>(
            getMethod("setEnabledExclusiveInCategory", String::class.java, Int::class.java),
            packageName, -2
        )

    fun setPriority(packageName: String, packageToOutrank: String) =
        invokeMethod<Boolean>(
            getMethod("setPriority", String::class.java, String::class.java, Int::class.java),
            packageName, packageToOutrank, -2
        )

    fun setHighestPriority(packageName: String) =
        invokeMethod<Boolean>(
            getMethod("setHighestPriority", String::class.java, Int::class.java),
            packageName, -2
        )

    fun setLowestPriority(packageName: String) =
        invokeMethod<Boolean>(
            getMethod("setLowestPriority", String::class.java, Int::class.java),
            packageName, -2
        )

    fun commit(transaction: Any) =
        invokeMethod<Unit>(
            getMethod("commit", omtClass),
            transaction
        )

    private fun getMethod(methodName: String, vararg args: Class<*>) =
        clazz.getMethod(methodName, *args)

    private fun <T> invokeMethod(method: Method, vararg args: Any): T {
        return method.invoke(obj, *args) as T
    }
}