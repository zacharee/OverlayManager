package tk.zwander.overlaymanager

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hmomeni.progresscircula.ProgressCircula
import kotlinx.android.synthetic.main.activity_main.*
import tk.zwander.overlaymanager.data.ObservableHashMap
import tk.zwander.overlaymanager.proxy.OverlayInfo
import tk.zwander.overlaymanager.ui.TargetAdapter
import tk.zwander.overlaymanager.util.DividerItemDecoration
import tk.zwander.overlaymanager.util.app
import tk.zwander.overlaymanager.util.layoutTransition
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private val batchedUpdates = ObservableHashMap<OverlayInfo, Boolean>()
    private val targetAdapter by lazy { TargetAdapter(this, batchedUpdates) }
    private val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    private var progressItem: MenuItem? = null
    private var unappliedAlert: MenuItem? = null
    private var doneLoading = false
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
                doneLoading = true
                progressItem?.isVisible = false
                change_all_wrapper.isVisible = true

                apply.isVisible = true
                apply.setOnClickListener {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.apply_changes)
                        .setMessage(R.string.apply_overlays_desc)
                        .setPositiveButton(android.R.string.yes) {_, _ ->
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
                        .setNegativeButton(android.R.string.no, null)
                        .show()
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
            match_overlays.isVisible = true
        }

        searchView?.setOnCloseListener {
            match_overlays.isVisible = false
            false
        }

        searchView?.setOnQueryTextListener(targetAdapter)

        progressItem = menu.findItem(R.id.progress)
        if (doneLoading) {
            progressItem?.isVisible = false
        }

        unappliedAlert = menu.findItem(R.id.unappliedChanges)
        unappliedAlert?.isVisible = batchedUpdates.isNotEmpty()
        unappliedAlert?.setOnMenuItemClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.unapplied_changes)
                .setMessage(R.string.unapplied_changes_desc)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            true
        }

        batchedUpdates.addObserver { _, _ ->
            unappliedAlert?.isVisible = batchedUpdates.isNotEmpty()
        }

        return true
    }

    private fun onKeyboardOpen() {
        title_text.isVisible = false
        title_border.isVisible = false
    }

    private fun onKeyboardClose() {
        title_text.isVisible = true
        title_border.isVisible = true
    }
}
