package tk.zwander.overlaymanager

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import kotlinx.android.synthetic.main.activity_main.*
import tk.zwander.overlaymanager.proxy.OverlayInfo
import tk.zwander.overlaymanager.ui.TargetAdapter
import tk.zwander.overlaymanager.util.app

class MainActivity : AppCompatActivity() {
    private val targetAdapter by lazy { TargetAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        target_list.adapter = targetAdapter

        app.receiver.postAction {
            targetAdapter.setItems(packageManager, it.allOverlays as MutableMap<String, List<OverlayInfo>>)
        }

        content.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        match_overlays.setOnCheckedChangeListener { _, isChecked ->
            targetAdapter.matchOverlays = isChecked
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

        return true
    }
}
