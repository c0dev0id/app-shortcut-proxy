package de.codevoid.appshortcutproxy

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class ConfigActivity : Activity() {

    private enum class Step { APP_LIST, ACTIVITY_LIST }

    private lateinit var listView: ListView
    private lateinit var textTitle: TextView
    private lateinit var repo: ShortcutRepository

    private val appItems = mutableListOf<AppItem>()
    private val activityItems = mutableListOf<ActivityItem>()
    private var currentStep = Step.APP_LIST

    data class AppItem(val packageName: String, val label: String)
    data class ActivityItem(val packageName: String, val activityName: String, val label: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        repo = ShortcutRepository(this)
        textTitle = findViewById(R.id.textTitle)
        listView = findViewById(R.id.listView)
        loadApps()
    }

    private fun loadApps() {
        currentStep = Step.APP_LIST
        textTitle.setText(R.string.select_app_title)

        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        appItems.clear()
        resolveInfos.forEach { ri ->
            appItems.add(AppItem(
                packageName = ri.activityInfo.packageName,
                label = ri.loadLabel(pm).toString()
            ))
        }

        if (appItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_apps_found), Toast.LENGTH_LONG).show()
        }

        val adapter = object : ArrayAdapter<AppItem>(this, R.layout.item_shortcut, appItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_shortcut, parent, false)
                val item = getItem(position)!!
                view.findViewById<TextView>(R.id.textAppName).text = item.label
                view.findViewById<TextView>(R.id.textShortcutName).text = item.packageName
                return view
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            loadActivities(appItems[position])
        }
    }

    private fun loadActivities(app: AppItem) {
        val pm = packageManager
        val activities = try {
            pm.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES)
                .activities?.filter { it.exported } ?: emptyList()
        } catch (e: PackageManager.NameNotFoundException) {
            emptyList()
        }

        if (activities.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_activities_found), Toast.LENGTH_SHORT).show()
            return
        }

        if (activities.size == 1) {
            val ai = activities[0]
            saveActivity(app.packageName, ai.name, activityLabel(pm, ai))
            return
        }

        currentStep = Step.ACTIVITY_LIST
        textTitle.text = getString(R.string.select_activity_title, app.label)

        activityItems.clear()
        activities.forEach { ai ->
            activityItems.add(ActivityItem(
                packageName = app.packageName,
                activityName = ai.name,
                label = activityLabel(pm, ai)
            ))
        }

        val adapter = object : ArrayAdapter<ActivityItem>(this, R.layout.item_shortcut, activityItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_shortcut, parent, false)
                val item = getItem(position)!!
                view.findViewById<TextView>(R.id.textAppName).text = item.label
                view.findViewById<TextView>(R.id.textShortcutName).text = item.activityName
                return view
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = activityItems[position]
            saveActivity(item.packageName, item.activityName, item.label)
        }
    }

    private fun activityLabel(pm: PackageManager, ai: android.content.pm.ActivityInfo): String =
        ai.loadLabel(pm).toString().ifEmpty { ai.name }

    private fun saveActivity(packageName: String, activityName: String, label: String) {
        val intent = Intent().setComponent(ComponentName(packageName, activityName))
        val intentUri = intent.toUri(Intent.URI_INTENT_SCHEME)
        repo.saveShortcut(packageName, label, intentUri)
        Toast.makeText(this, getString(R.string.shortcut_saved, label), Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LaunchActivity::class.java))
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentStep == Step.ACTIVITY_LIST) {
            loadApps()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
