package tk.zwander.overlaymanager.data

import tk.zwander.overlaymanager.IRootBridge

data class BatchedUpdate(val key: String, val action: (IRootBridge) -> Unit): (IRootBridge) -> Unit {
    override fun invoke(p1: IRootBridge) {
        action(p1)
    }
}