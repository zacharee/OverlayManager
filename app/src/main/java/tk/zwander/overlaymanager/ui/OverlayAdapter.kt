package tk.zwander.overlaymanager.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import tk.zwander.overlaymanager.IRootBridge
import tk.zwander.overlaymanager.R
import tk.zwander.overlaymanager.data.BatchedUpdate
import tk.zwander.overlaymanager.databinding.OverlayItemBinding
import tk.zwander.overlaymanager.proxy.OverlayInfo
import tk.zwander.overlaymanager.util.app
import tk.zwander.overlaymanager.util.createEnabledUpdate
import tk.zwander.overlaymanager.util.createPriorityUpdate

class OverlayAdapter(private val batchedUpdates: MutableMap<String, BatchedUpdate>) : RecyclerView.Adapter<OverlayAdapter.OverlayHolder>() {
    private val items = SortedList(OverlayInfo::class.java, object : SortedList.Callback<OverlayInfo>() {
        override fun areItemsTheSame(item1: OverlayInfo?, item2: OverlayInfo?) =
            item1 == item2

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int) {
            notifyItemRangeChanged(position, count)
        }

        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(position, count)
        }

        override fun compare(o1: OverlayInfo, o2: OverlayInfo) =
            if (o1.showEnabled && !o2.showEnabled) -1 else if (!o1.showEnabled && o2.showEnabled) 1 else o1.packageName.compareTo(o2.packageName)

        override fun areContentsTheSame(oldItem: OverlayInfo, newItem: OverlayInfo) =
            oldItem.packageName == newItem.packageName

    })

    override fun getItemCount() = items.size()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverlayHolder {
        return OverlayHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.overlay_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: OverlayHolder, position: Int) {
        holder.bindInfo(items[position], itemCount)
    }

    fun setItems(list: List<OverlayInfo>) {
        items.clear()
        items.addAll(list)
    }

    inner class OverlayHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = OverlayItemBinding.bind(itemView)

        fun bindInfo(info: OverlayInfo, size: Int) {
            itemView.apply {
                binding.overlayPackage.text = info.packageName

                binding.enabled.setOnCheckedChangeListener(null)

                if (info.isStatic) {
                    binding.enabled.isChecked = true
                    binding.enabled.isEnabled = false
                } else {
                    binding.enabled.isChecked = info.showEnabled

                    binding.enabled.setOnCheckedChangeListener { _, isChecked ->
                        val item = items.get(adapterPosition)
                        val update = item.createEnabledUpdate(isChecked)

                        batchedUpdates[update.first] = update.second
                        item.showEnabled = isChecked
                        items.recalculatePositionOfItemAt(adapterPosition)
                    }
                }

                binding.priority.text = itemView.context.resources.getString(R.string.priority, info.priority)

                binding.setHighestPriority.isEnabled = size > 1
                binding.setLowestPriority.isEnabled = size > 1

                if (size > 1) {
                    binding.setHighestPriority.setOnClickListener {
                        val item = items[adapterPosition]
                        val update = item.createPriorityUpdate(true) {
                            notifyItemChanged(adapterPosition)
                        }

                        batchedUpdates[update.first] = update.second
                    }

                    binding.setLowestPriority.setOnClickListener {
                        val item = items[adapterPosition]
                        val update = item.createPriorityUpdate(false) {
                            notifyItemChanged(adapterPosition)
                        }

                        batchedUpdates[update.first] = update.second
                    }
                }
            }
        }
    }
}