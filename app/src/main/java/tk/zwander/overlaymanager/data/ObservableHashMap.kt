package tk.zwander.overlaymanager.data

import java.util.*
import kotlin.collections.HashMap

class ObservableHashMap<Key, Value> : Observable(), MutableMap<Key, Value> {
    private val wrapped = HashMap<Key, Value>()

    override val size: Int
        get() = wrapped.size
    override val entries: MutableSet<MutableMap.MutableEntry<Key, Value>>
        get() = wrapped.entries
    override val keys: MutableSet<Key>
        get() = wrapped.keys
    override val values: MutableCollection<Value>
        get() = wrapped.values

    override fun containsKey(key: Key): Boolean {
        return wrapped.containsKey(key)
    }

    override fun containsValue(value: Value): Boolean {
        return wrapped.containsValue(value)
    }

    override fun get(key: Key): Value? {
        return wrapped[key]
    }

    override fun isEmpty(): Boolean {
        return wrapped.isEmpty()
    }

    override fun clear() {
        wrapped.clear()
        setChangedAndNotifyObservers()
    }

    override fun put(key: Key, value: Value): Value? {
        return wrapped.put(key, value).also {
            setChangedAndNotifyObservers()
        }
    }

    override fun putAll(from: Map<out Key, Value>) {
        wrapped.putAll(from)
        setChangedAndNotifyObservers()
    }

    override fun remove(key: Key): Value? {
        return wrapped.remove(key).also {
            setChangedAndNotifyObservers()
        }
    }

    private fun setChangedAndNotifyObservers() {
        setChanged()
        notifyObservers()
    }
}