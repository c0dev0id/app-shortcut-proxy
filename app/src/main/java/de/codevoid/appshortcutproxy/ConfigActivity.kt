package de.codevoid.appshortcutproxy

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class ConfigActivity : Activity() {

    private lateinit var listView: ListView
    private lateinit var repo: ShortcutRepository
    private val allItems = mutableListOf<ShortcutItem>()

    data class ShortcutItem(
        val packageName: String,
        val shortcutId: String,
        val appLabel: String,
        val shortcutLabel: String,
        val intentUri: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        repo = ShortcutRepository(this)
        listView = findViewById(R.id.listView)
        loadShortcuts()
    }

    private fun loadShortcuts() {
        allItems.clear()
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val pm = packageManager

        val query = LauncherApps.ShortcutQuery().apply {
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED_BY_ANY_LAUNCHER
            )
        }

        try {
            val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle())
            shortcuts?.forEach { info ->
                val appLabel = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(info.`package`, 0)
                    ).toString()
                } catch (e: Exception) {
                    info.`package`
                }
                val shortcutLabel = info.shortLabel?.toString()
                    ?: info.longLabel?.toString()
                    ?: info.id
                val intentUri = try {
                    info.intents?.lastOrNull()?.toUri(Intent.URI_INTENT_SCHEME)
                } catch (e: Exception) {
                    null
                }
                allItems.add(
                    ShortcutItem(
                        packageName = info.`package`,
                        shortcutId = info.id,
                        appLabel = appLabel,
                        shortcutLabel = shortcutLabel,
                        intentUri = intentUri
                    )
                )
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_LONG).show()
        }

        if (allItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_shortcuts_found), Toast.LENGTH_LONG).show()
        }

        val adapter = object : ArrayAdapter<ShortcutItem>(this, R.layout.item_shortcut, allItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_shortcut, parent, false)
                val item = getItem(position)!!
                view.findViewById<TextView>(R.id.textAppName).text = item.appLabel
                view.findViewById<TextView>(R.id.textShortcutName).text = item.shortcutLabel
                return view
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = allItems[position]
            repo.saveShortcut(
                item.packageName,
                item.shortcutId,
                "${item.appLabel}: ${item.shortcutLabel}",
                item.intentUri
            )
            if (item.intentUri != null) {
                showSwitchLauncherDialog()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.shortcut_saved, item.shortcutLabel),
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this, LaunchActivity::class.java))
                finish()
            }
        }
    }

    private fun showSwitchLauncherDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.shortcut_configured_title))
            .setMessage(getString(R.string.shortcut_configured_message))
            .setPositiveButton(getString(R.string.open_home_settings)) { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                } catch (e: Exception) {
                    // ignore if settings screen unavailable
                }
                startActivity(Intent(this, LaunchActivity::class.java))
                finish()
            }
            .setNegativeButton(getString(R.string.skip)) { _, _ ->
                startActivity(Intent(this, LaunchActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
