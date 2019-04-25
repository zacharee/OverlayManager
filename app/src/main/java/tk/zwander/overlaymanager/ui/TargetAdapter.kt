package tk.zwander.overlaymanager.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import kotlinx.android.synthetic.main.target_item.view.*
import tk.zwander.overlaymanager.R
import tk.zwander.overlaymanager.data.TargetData
import tk.zwander.overlaymanager.proxy.OverlayInfo
import tk.zwander.overlaymanager.util.DividerItemDecoration
import tk.zwander.overlaymanager.util.mainHandler
import java.lang.Exception

class TargetAdapter : RecyclerView.Adapter<TargetAdapter.TargetHolder>(), SearchView.OnQueryTextListener {
    val items = SortedList<TargetData>(TargetData::class.java, object : SortedList.Callback<TargetData>() {
        override fun areItemsTheSame(item1: TargetData?, item2: TargetData?) =
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

        override fun compare(o1: TargetData, o2: TargetData) =
            o1.label.compareTo(o2.label)

        override fun areContentsTheSame(oldItem: TargetData, newItem: TargetData) =
            oldItem.packageName == newItem.packageName

    })
    val orig = object : ArrayList<TargetData>() {
        override fun add(element: TargetData): Boolean {
            if (matches(currentQuery, element)) {
                items.add(element)
            }
            return super.add(element)
        }

        override fun remove(element: TargetData): Boolean {
            items.remove(element)
            return super.remove(element)
        }
    }

    private var currentQuery = ""
    private var recyclerView: RecyclerView? = null

    var matchOverlays = false
        set(value) {
            field = value

            onQueryTextChange(currentQuery)
        }

    var targetSize = 0

    override fun onQueryTextChange(newText: String?): Boolean {
        currentQuery = newText ?: ""

        items.replaceAll(filter(currentQuery))
        recyclerView?.scrollToPosition(0)

        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemCount() = items.size()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TargetHolder {
        return TargetHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.target_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TargetHolder, position: Int) {
        holder.bindInfo(items[position])
    }

    fun setItems(packageManager: PackageManager, items: MutableMap<String, List<OverlayInfo>>) {
        mainHandler.post {
            targetSize = items.size
            orig.clear()

            items.forEach { (key, value) ->
                try {
                    val appInfo = packageManager.getApplicationInfo(key, 0)
                    val data = TargetData(
                        key,
                        appInfo.loadLabel(packageManager).toString(),
                        appInfo.loadIcon(packageManager),
                        value
                    )

                    orig.add(data)
                } catch (e: Exception) {
                    targetSize--
                }
            }
        }
    }

    private fun matches(query: String, data: TargetData): Boolean {
        if (data.label.toLowerCase().contains(query.toLowerCase())
            || data.packageName.toLowerCase().contains(query.toLowerCase()))
            return true

        if (matchOverlays) {
            data.info.forEach {
                if (it.packageName.contains(query)) return true
            }
        }

        return false
    }

    private fun filter(query: String): List<TargetData> {
        val lowerCaseQuery = query.toLowerCase()

        val filteredModelList = ArrayList<TargetData>()

        for (i in 0 until orig.size) {
            val item = orig[i]

            if (matches(lowerCaseQuery, item)) filteredModelList.add(item)
        }

        return filteredModelList
    }

    inner class TargetHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindInfo(info: TargetData) {
            val adapter = OverlayAdapter()

            itemView.target_icon.setImageDrawable(info.icon)
            itemView.target_label.text = info.label
            itemView.target_pkg.text = info.packageName
            itemView.count.text = itemView.resources.getString(R.string.overlay_count, info.info.size)
            itemView.overlay_list.adapter = adapter
            itemView.overlay_list.addItemDecoration(DividerItemDecoration(itemView.context, LinearLayoutManager.HORIZONTAL))

            itemView.overlay_list.visibility = if (info.expanded) View.VISIBLE else View.GONE

            adapter.setItems(info.info)

            itemView.setOnClickListener {
                info.expanded = !info.expanded

                notifyItemChanged(adapterPosition)
            }
        }
    }
}