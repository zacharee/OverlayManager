package tk.zwander.overlaymanager.root

import tk.zwander.overlaymanager.IRootBridge
import tk.zwander.overlaymanager.proxy.IOverlayManager
import tk.zwander.overlaymanager.proxy.OverlayInfo

class IRootBridgeImpl : IRootBridge.Stub() {
    private val om = IOverlayManager()

    override fun getAllOverlays(): MutableMap<String, List<OverlayInfo>> =
        om.getAllOverlays()

    override fun getOverlayInfosForTarget(packageName: String): List<OverlayInfo> =
        om.getOverlayInfosForTarget(packageName)

    override fun getOverlayInfo(packageName: String) =
        om.getOverlayInfo(packageName)

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