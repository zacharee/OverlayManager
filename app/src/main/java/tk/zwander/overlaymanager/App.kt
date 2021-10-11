package tk.zwander.overlaymanager

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent

import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import android.os.IBinder

import android.content.ServiceConnection
import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.*
import org.lsposed.hiddenapibypass.HiddenApiBypass
import tk.zwander.overlaymanager.root.IRootBridgeImpl
import tk.zwander.overlaymanager.root.RootBridgeService
import tk.zwander.overlaymanager.util.shizukuAvailable


class App : Application(), CoroutineScope by MainScope() {
    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
            )
        }
    }

    val receiver by lazy { Receiver() }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("L")
        }

        async(Dispatchers.IO) {
            if (!shizukuAvailable && Shell.rootAccess()) {
                receiver.bindRoot(this@App)
            }
        }
    }

    class Receiver : CoroutineScope by MainScope() {
        private val userServiceStandaloneProcessArgs = UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                IRootBridgeImpl::class.java.name
            )
        )
            .daemon(false)
            .tag("OverlayManager")
            .processNameSuffix("oms")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

        private val userServiceConnection: ServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
                if (binder != null && binder.pingBinder()) {
                    val service: IRootBridge = IRootBridge.Stub.asInterface(binder)
                    ipc = service
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                ipc = null
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

        fun postAction(action: (IRootBridge) -> Unit) {
            if (ipc == null) {
                queuedActions.add(action)
            } else {
                action.invoke(ipc!!)
            }
        }

        fun tryBindShizuku() {
            try {
                Log.e("OverlayManager", "starting Shizuku")
                if (Shizuku.getVersion() >= 10) {
                    Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
                }
            } catch (tr: Throwable) {
                Log.e("OverlayManager", "error starting Shizuku")
                tr.printStackTrace()
            }
        }

        fun bindRoot(context: Context) {
            RootService.bind(
                Intent(context, RootBridgeService::class.java),
                userServiceConnection
            )
        }

        suspend fun awaitBridge(): IRootBridge {
            return withContext(Dispatchers.IO) {
                while (ipc == null) {}
                ipc!!
            }
        }
    }
}