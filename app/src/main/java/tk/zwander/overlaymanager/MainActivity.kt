package tk.zwander.overlaymanager

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.hmomeni.progresscircula.ProgressCircula
import kotlinx.android.synthetic.main.activity_main.*
import tk.zwander.overlaymanager.proxy.OverlayInfo
import tk.zwander.overlaymanager.ui.TargetAdapter
import tk.zwander.overlaymanager.util.DividerItemDecoration
import tk.zwander.overlaymanager.util.app
import tk.zwander.overlaymanager.util.layoutTransition

class MainActivity : AppCompatActivity() {
    private val targetAdapter by lazy { TargetAdapter() }

    private var progress: ProgressCircula? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        target_list.adapter = targetAdapter
        target_list.itemAnimator = object : DefaultItemAnimator() {
            override fun onAddFinished(item: RecyclerView.ViewHolder) {
                super.onAddFinished(item)

                if (targetAdapter.itemCount == targetAdapter.targetSize) {
                    progress?.visibility = View.GONE
                }
            }
        }
        target_list.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))

        content.layoutTransition = layoutTransition

        match_overlays.setOnCheckedChangeListener { _, isChecked ->
            targetAdapter.matchOverlays = isChecked
        }

        app.receiver.postAction {
            targetAdapter.setItems(
                packageManager,
                it.allOverlays as MutableMap<String, List<OverlayInfo>>
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.
            search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView?

        searchView?.setOnQueryTextListener(targetAdapter)

        searchView?.setOnSearchClickListener { match_overlays.visibility = View.VISIBLE }
        searchView?.setOnCloseListener {
            match_overlays.visibility = View.GONE
            false
        }

        progress = menu.findItem(R.id.progress).actionView as ProgressCircula?

        return true
    }
}
