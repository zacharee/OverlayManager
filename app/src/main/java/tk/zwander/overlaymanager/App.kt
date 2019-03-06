package tk.zwander.overlaymanager

import android.app.Application
import android.content.Context
import eu.chainfire.librootjava.RootIPCReceiver
import eu.chainfire.librootjava.RootJava
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tk.zwander.overlaymanager.root.RootBridge

class App : Application() {
    val receiver by lazy { Receiver(this) }

    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch {
            Shell.SU.run(
                RootJava.getLaunchScript(
                    this@App,
                    RootBridge::class.java,
                    null,
                    null,
                    null,
                    BuildConfig.APPLICATION_ID + ":root"
                )
            )
        }

        receiver.setContext(this)
    }

    class Receiver(context: Context) : RootIPCReceiver<IRootBridge>(context, 0, IRootBridge::class.java) {
        private val queuedActions = ArrayList<(IRootBridge) -> Unit>()

        private var ipc: IRootBridge? = null
            set(value) {
                field = value

                if (value != null) {
                    queuedActions.forEach { it.invoke(ipc!!) }
                    queuedActions.clear()
                }
            }

        override fun onConnect(ipc: IRootBridge?) {
            this.ipc = ipc
        }

        override fun onDisconnect(ipc: IRootBridge?) {
            this.ipc = null
        }

        fun postAction(action: (IRootBridge) -> Unit) {
            if (ipc == null) {
                queuedActions.add(action)
            } else {
                action.invoke(ipc!!)
            }
        }
    }
}