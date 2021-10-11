package tk.zwander.overlaymanager.root

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

class RootBridgeService : RootService() {
    override fun onBind(intent: Intent): IBinder {
        return IRootBridgeImpl()
    }
}