package tk.zwander.overlaymanager.root

import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import tk.zwander.overlaymanager.IRootBridge
import tk.zwander.overlaymanager.proxy.IOverlayManager
import tk.zwander.overlaymanager.proxy.OverlayInfo
import kotlin.system.exitProcess

class IRootBridgeImpl : IRootBridge.Stub() {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("L")
        }
    }

    private val om = IOverlayManager()

    override fun getAllOverlays(): MutableMap<String, List<OverlayInfo>> =
        om.getAllOverlays()

    override fun getOverlayInfosForTarget(packageName: String): List<OverlayInfo> =
        om.getOverlayInfosForTarget(packageName)

    override fun getOverlayInfo(packageName: String) =
        om.getOverlayInfo(packageName)

    override fun getOverlayInfoByIdentifier(identifier: String) =
        om.getOverlayInfoByIdentifier(IOverlayManager.getIdentifier(identifier))

    override fun setOverlayEnabled(packageName: String, enabled: Boolean) =
        om.setEnabled(packageName, enabled)

    override fun setOverlayEnabledByIdentifier(identifier: String, enabled: Boolean) =
        om.setEnabled(IOverlayManager.getIdentifier(identifier), enabled)

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

    override fun clearCache(packageName: String) =
        om.clearCache(packageName)

    override fun destroy() {
        exitProcess(0)
    }
}