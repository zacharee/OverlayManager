package tk.zwander.overlaymanager

import android.app.Application
import android.content.Context
import eu.chainfire.librootjava.RootIPCReceiver
import eu.chainfire.librootjava.RootJava
import eu.chainfire.libsuperuser.Shell
import tk.zwander.overlaymanager.root.RootBridge
import android.content.ComponentName

import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import android.os.IBinder

import android.content.ServiceConnection
import android.util.Log
import kotlinx.coroutines.*
import tk.zwander.overlaymanager.root.IRootBridgeImpl


class App : Application() {
    val receiver by lazy { Receiver(this) }

    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch {
            if (Shell.SU.available()) {
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
        }

        receiver.setContext(this)
    }

    class Receiver(context: Context) : RootIPCReceiver<IRootBridge>(context, 0, IRootBridge::class.java), CoroutineScope by MainScope() {
        private val userServiceStandaloneProcessArgs = UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                IRootBridgeImpl::class.java.name
            )
        )
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

        private val userServiceConnection: ServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
                if (binder != null && binder.pingBinder()) {
                    val service: IRootBridge = IRootBridge.Stub.asInterface(binder)
                    onConnect(service)
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                onDisconnect(null)
            }
        }

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

        fun tryBindShizuku() {
            try {
                if (Shizuku.getVersion() >= 10) {
                    Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
                }
            } catch (tr: Throwable) {
                tr.printStackTrace()
            }
        }

        suspend fun awaitBridge(): IRootBridge {
            return withContext(Dispatchers.IO) {
                while (ipc == null) {}
                ipc!!
            }
        }
    }
}