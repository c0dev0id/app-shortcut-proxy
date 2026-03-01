package de.codevoid.appshortcutproxy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process
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
        val shortcutLabel: String
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
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
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
                allItems.add(
                    ShortcutItem(
                        packageName = info.`package`,
                        shortcutId = info.id,
                        appLabel = appLabel,
                        shortcutLabel = shortcutLabel
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
                "${item.appLabel}: ${item.shortcutLabel}"
            )
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
