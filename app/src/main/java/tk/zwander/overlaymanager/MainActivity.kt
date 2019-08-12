package tk.zwander.overlaymanager

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hmomeni.progresscircula.ProgressCircula
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tk.zwander.overlaymanager.proxy.OverlayInfo
import tk.zwander.overlaymanager.ui.TargetAdapter
import tk.zwander.overlaymanager.util.DividerItemDecoration
import tk.zwander.overlaymanager.util.app
import tk.zwander.overlaymanager.util.layoutTransition

class MainActivity : AppCompatActivity() {
    private val batchedUpdates = HashMap<OverlayInfo, Boolean>()
    private val targetAdapter by lazy { TargetAdapter(this, batchedUpdates) }
    private val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    private var progress: ProgressCircula? = null
    private var searchView: SearchView? = null

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val closed = imm.inputMethodWindowVisibleHeight <= 0

        if (closed) onKeyboardClose()
        else onKeyboardOpen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(bottom_bar)
        setTitle(R.string.app_name)

        target_list.adapter = targetAdapter

        target_list.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        target_list.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))

        content.layoutTransition = layoutTransition

        match_overlays.setOnCheckedChangeListener { _, isChecked ->
            targetAdapter.matchOverlays = isChecked
        }

        app.receiver.postAction {
            targetAdapter.setItems(
                packageManager,
                it.allOverlays as MutableMap<String, List<OverlayInfo>>
            ) {
                progress?.visibility = View.GONE
                change_all_wrapper.visibility = View.VISIBLE

                apply.visibility = View.VISIBLE
                apply.setOnClickListener {
                    app.receiver.postAction {
                        val copy = HashMap(batchedUpdates)
                        batchedUpdates.clear()

                        copy.forEach { (t, u) ->
                            if (t.isEnabled != u) {
                                it.setOverlayEnabled(t.packageName, u)

                                t.updateInstance(it.getOverlayInfo(t.packageName))
                                targetAdapter.notifyChanged()
                            }
                        }
                    }
                }

                enable_all.setOnClickListener {
                    targetAdapter.orig.forEach { td ->
                        td.info.forEach { info ->
                            batchedUpdates[info] = true
                            info.showEnabled = true
                        }
                    }
                    targetAdapter.notifyChanged()
                }

                disable_all.setOnClickListener {
                    targetAdapter.orig.forEach { td ->
                        td.info.forEach { info ->
                            batchedUpdates[info] = false
                            info.showEnabled = false
                        }
                    }
                    targetAdapter.notifyChanged()
                }

                expand_all.setOnClickListener {
                    targetAdapter.setAllExpanded(true)
                }

                collapse_all.setOnClickListener {
                    targetAdapter.setAllExpanded(false)
                }
            }
        }

        content.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        content.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
    }

    override fun setTitle(titleId: Int) {
        title = getText(titleId)
    }

    override fun setTitle(title: CharSequence?) {
        title_text.text = title
        super.setTitle(null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as SearchView?

        searchView?.setOnSearchClickListener {
            match_overlays.visibility = View.VISIBLE
        }

        searchView?.setOnCloseListener {
            match_overlays.visibility = View.GONE
            false
        }

        searchView?.setOnQueryTextListener(targetAdapter)

        progress = menu.findItem(R.id.progress).actionView as ProgressCircula?

        return true
    }

    private fun onKeyboardOpen() {
        title_text.visibility = View.GONE
        title_border.visibility = View.GONE
    }

    private fun onKeyboardClose() {
        title_text.visibility = View.VISIBLE
        title_border.visibility = View.VISIBLE
    }
}
