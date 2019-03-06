package tk.zwander.overlaymanager.root

import eu.chainfire.librootjava.RootIPC
import eu.chainfire.librootjava.RootJava
import tk.zwander.overlaymanager.BuildConfig
import tk.zwander.overlaymanager.IRootBridge
import tk.zwander.overlaymanager.proxy.IOverlayManager
import tk.zwander.overlaymanager.proxy.OverlayInfo

object RootBridge {
    @JvmStatic
    fun main(args: Array<String>) {
        RootJava.restoreOriginalLdLibraryPath()

        try {
            RootIPC(BuildConfig.APPLICATION_ID, IRootBridgeImpl(), 0, 30 * 1000, true)
        } catch (e: RootIPC.TimeoutException) {}
    }

    class IRootBridgeImpl : IRootBridge.Stub() {
        private val om = IOverlayManager()

        override fun getAllOverlays(): MutableMap<String, List<OverlayInfo>> =
            om.getAllOverlays()

        override fun setOverlayEnabled(packageName: String, enabled: Boolean) =
            om.setEnabled(packageName, enabled)

        override fun setOverlayEnabledExclusive(packageName: String, enabled: Boolean) =
            om.setEnabledExclusive(packageName, enabled)

        override fun setOverlayEnabledExclusiveInCategory(packageName: String) =
            om.setEnabledExclusiveInCategory(packageName)

        override fun setOverlayPriority(packageName: String, packageToOutrank: String) =
            om.setPriority(packageName, packageToOutrank)

        override fun setOverlayHighestPriority(packageName: String) =
            om.setHighestPriority(packageName)

        override fun setOverlayLowestPriority(packageName: String) =
            om.setLowestPriority(packageName)

    }
}