package tk.zwander.overlaymanager

import android.content.Context
import android.content.pm.PackageManager
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
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import tk.zwander.overlaymanager.data.BatchedUpdate
import tk.zwander.overlaymanager.data.ObservableHashMap
import tk.zwander.overlaymanager.proxy.IOverlayManager
import tk.zwander.overlaymanager.proxy.OverlayInfo
import tk.zwander.overlaymanager.ui.TargetAdapter
import tk.zwander.overlaymanager.util.*
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val batchedUpdates = ObservableHashMap<String, BatchedUpdate>()
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

        launch(Dispatchers.IO) {
            if (Shell.SU.available() || shizukuGranted) {
                doLoad()
            } else if (shizukuAvailable && !shizukuGranted) {
                requestShizukuPermission(100) { code, result ->
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        doLoad()
                    } else {
                        finish()
                    }
                }
            } else {
                launch(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(R.string.shizuku_root_required_title)
                        .setMessage(R.string.shizuku_root_required_msg)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            finish()
                        }
                        .setNegativeButton(R.string.shizuku_root_get_shizuku) { _, _ ->
                            launchUrl("https://github.com/RikkaApps/Shizuku/releases")
                            finish()
                        }
                        .show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doLoad()
            } else {
                finish()
            }
        }
    }

    private fun doLoad() = launch(Dispatchers.Main) {
        if (shizukuAvailable) {
            app.receiver.tryBindShizuku()
        }

        target_list.adapter = targetAdapter

        target_list.layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
        target_list.addItemDecoration(DividerItemDecoration(this@MainActivity, RecyclerView.VERTICAL))

        content.layoutTransition = layoutTransition

        match_overlays.setOnCheckedChangeListener { _, isChecked ->
            targetAdapter.matchOverlays = isChecked
        }

        val rootBridge = app.receiver.awaitBridge()

        withContext(Dispatchers.IO) {
            targetAdapter.setItems(
                packageManager,
                rootBridge.allOverlays as MutableMap<String, List<OverlayInfo>>
            )
        }

        doneLoading = true
        progressItem?.isVisible = false
        change_all_wrapper.isVisible = true
        apply.isVisible = true

        apply.setOnClickListener {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.apply_changes)
                .setMessage(R.string.apply_overlays_desc)
                .setPositiveButton(android.R.string.yes) {_, _ ->
                    val copy = HashMap(batchedUpdates)
                    batchedUpdates.clear()

                    copy.forEach { (_, u) ->
                        u(rootBridge)
                    }

                    targetAdapter.notifyChanged()
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
        }

        enable_all.setOnClickListener {
            targetAdapter.orig.forEach { td ->
                td.info.forEach { info ->
                    val i = info.createEnabledUpdate(true)
                    batchedUpdates[i.first] = i.second

                    info.showEnabled = true
                }
            }
            targetAdapter.notifyChanged()
        }

        disable_all.setOnClickListener {
            targetAdapter.orig.forEach { td ->
                td.info.forEach { info ->
                    val i = info.createEnabledUpdate(false)
                    batchedUpdates[i.first] = i.second

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

        content.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
        content.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
    }

    override fun setTitle(titleId: Int) {
        title = getText(titleId)
    }

    override fun setTitle(title: CharSequence?) {
        title_text.text = title
        super.setTitle(null)
    }

    override fun onCreateOptionsMenu(m: Menu): Boolean {
        val menu = action_menu.menu
        menuInflater.inflate(R.menu.search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as SearchView?

        searchView?.setOnSearchClickListener {
            match_overlays.isVisible = true
            change_all_wrapper.isVisible = false
        }

        searchView?.setOnCloseListener {
            match_overlays.isVisible = false
            change_all_wrapper.isVisible = true
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
            val notEmpty = batchedUpdates.isNotEmpty()
            unappliedAlert?.isVisible = notEmpty
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
