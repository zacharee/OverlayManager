package tk.zwander.overlaymanager

import android.app.Application
import android.content.Context
import android.os.Build
import eu.chainfire.librootjava.RootIPCReceiver
import eu.chainfire.librootjava.RootJava
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import tk.zwander.overlaymanager.root.RootBridge
import java.lang.reflect.Method

class App : Application() {
    val receiver by lazy { Receiver(this) }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
            val getDeclaredMethod = Class::class.java.getDeclaredMethod("getDeclaredMethod", String::class.java, arrayOf<Class<*>>()::class.java)

            val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as Method
            val setHiddenApiExemptions = getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", arrayOf(arrayOf<String>()::class.java)) as Method

            val vmRuntime = getRuntime.invoke(null)

            setHiddenApiExemptions.invoke(vmRuntime, arrayOf("L"))
        }

        GlobalScope.launch {
            Shell.Pool.SU.run(
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

    class Receiver(context: Context) : RootIPCReceiver<IRootBridge>(context, 0, IRootBridge::class.java), CoroutineScope by MainScope() {
        private val queuedActions = ArrayList<(IRootBridge) -> Unit>()

        private val ipcLock = Any()

        private var ipc: IRootBridge? = null
            set(value) {
                launch {
                    synchronized(ipcLock) {
                        field = value

                        if (value != null) {
                            queuedActions.forEach { it.invoke(ipc!!) }
                            queuedActions.clear()
                        }
                    }
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