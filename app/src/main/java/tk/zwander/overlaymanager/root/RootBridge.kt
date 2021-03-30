package tk.zwander.overlaymanager.root

import eu.chainfire.librootjava.RootIPC
import eu.chainfire.librootjava.RootJava
import tk.zwander.overlaymanager.BuildConfig

object RootBridge {
    @JvmStatic
    fun main(args: Array<String>) {
        RootJava.restoreOriginalLdLibraryPath()

        try {
            RootIPC(BuildConfig.APPLICATION_ID, IRootBridgeImpl(), 0, 30 * 1000, true)
        } catch (e: RootIPC.TimeoutException) {}
    }

}