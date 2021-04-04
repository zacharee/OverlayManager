package tk.zwander.overlaymanager.ui

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import kotlinx.coroutines.*
import tk.zwander.overlaymanager.R
import tk.zwander.overlaymanager.data.BatchedUpdate
import tk.zwander.overlaymanager.data.TargetData
import tk.zwander.overlaymanager.databinding.TargetItemBinding
import tk.zwander.overlaymanager.proxy.OverlayInfo
import tk.zwander.overlaymanager.util.DividerItemDecoration
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class TargetAdapter(
    private val context: Context,
    private val batchedUpdates: MutableMap<String, BatchedUpdate>
) : RecyclerView.Adapter<TargetAdapter.TargetHolder>(), SearchView.OnQueryTextListener,
    CoroutineScope by MainScope() {
    val items = SortedList(TargetData::class.java, object : SortedList.Callback<TargetData>() {
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
            o1.getLabel(context).toString().compareTo(o2.getLabel(context).toString())

        override fun areContentsTheSame(oldItem: TargetData, newItem: TargetData) =
            oldItem.appInfo.packageName == newItem.appInfo.packageName

    })
    val orig = object : ArrayList<TargetData>() {
        override fun add(element: TargetData): Boolean {
            if (matches(currentQuery, element)) {
                items.add(element)
            }
            return super.add(element)
        }

        override fun addAll(elements: Collection<TargetData>): Boolean {
            items.addAll(elements.filter { matches(currentQuery, it) })
            return super.addAll(elements)
        }

        override fun remove(element: TargetData): Boolean {
            items.remove(element)
            return super.remove(element)
        }
    }

    private var currentQuery: String = ""
    private var recyclerView: RecyclerView? = null

    var matchOverlays = false
        set(value) {
            field = value

            onQueryTextChange(currentQuery)
        }

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
        cancel()
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

    fun setAllExpanded(expanded: Boolean) {
        orig.forEach {
            it.expanded = expanded
        }

        notifyDataSetChanged()
    }

    suspend fun setItems(
        packageManager: PackageManager,
        items: MutableMap<String, List<OverlayInfo>>
    ) {
        orig.clear()
        orig.addAll(
            withContext(Dispatchers.IO) {
                items.map {
                    try {
                        val appInfo = packageManager.getApplicationInfo(it.key, 0)
                        TargetData(
                            appInfo,
                            it.value
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filterNotNull()
            }
        )
    }

    fun notifyChanged() {
        notifyDataSetChanged()
    }

    private fun matches(query: String, data: TargetData): Boolean {
        if (query.isBlank()) return true

        if (data.getLabel(context).toString().toLowerCase(Locale.getDefault())
                .contains(query.toLowerCase(Locale.getDefault()))
            || data.appInfo.packageName.toLowerCase(Locale.getDefault())
                .contains(query.toLowerCase(Locale.getDefault()))
        )
            return true

        if (matchOverlays) {
            data.info.forEach {
                if (it.packageName.contains(query)) return true
            }
        }

        return false
    }

    private fun filter(query: String): List<TargetData> {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())

        val filteredModelList = ArrayList<TargetData>()

        for (i in 0 until orig.size) {
            val item = orig[i]

            if (matches(lowerCaseQuery, item)) filteredModelList.add(item)
        }

        return filteredModelList
    }

    private val picasso = Picasso.Builder(context)
        .addRequestHandler(AppIconHandler(context))
        .build()

    inner class TargetHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = TargetItemBinding.bind(itemView)

        fun bindInfo(info: TargetData) {
            val adapter = OverlayAdapter(batchedUpdates)
            val imgView = binding.targetIcon

            picasso
                .load(Uri.parse("${AppIconHandler.SCHEME}:${info.appInfo.packageName}"))
                .resize(imgView.maxWidth, imgView.maxHeight)
                .onlyScaleDown()
                .centerInside()
                .into(imgView)

            binding.targetLabel.text = info.getLabel(itemView.context)
            binding.targetPkg.text = info.appInfo.packageName
            binding.count.text =
                itemView.resources.getString(R.string.overlay_count, info.info.size)
            binding.overlayList.adapter = adapter
            binding.overlayList.addItemDecoration(
                DividerItemDecoration(
                    itemView.context,
                    LinearLayoutManager.VERTICAL
                )
            )

            binding.overlayList.isVisible = info.expanded

            adapter.setItems(info.info)

            itemView.setOnClickListener {
                val info = items[adapterPosition]
                info.expanded = !info.expanded

                notifyItemChanged(adapterPosition)
            }
        }
    }

    class AppIconHandler(private val context: Context) : RequestHandler() {
        companion object {
            const val SCHEME = "package"
        }

        override fun canHandleRequest(data: Request): Boolean {
            return data.uri != null && data.uri.scheme == SCHEME
        }

        override fun load(request: Request, networkPolicy: Int): Result? {
            return Result(
                context.packageManager.getApplicationIcon(request.uri.schemeSpecificPart)
                    .toBitmap(),
                Picasso.LoadedFrom.DISK
            )
        }
    }
}