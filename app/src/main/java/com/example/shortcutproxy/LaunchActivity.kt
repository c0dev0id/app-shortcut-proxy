package com.example.shortcutproxy

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Process
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class LaunchActivity : Activity() {

    private lateinit var repo: ShortcutRepository
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        repo = ShortcutRepository(this)

        val textCountdown = findViewById<TextView>(R.id.textCountdown)
        val textLabel = findViewById<TextView>(R.id.textLabel)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        textLabel.text = repo.getLabel() ?: getString(R.string.app_name)

        btnCancel.setOnClickListener {
            countDownTimer?.cancel()
            finish()
        }

        btnSettings.setOnClickListener {
            countDownTimer?.cancel()
            startActivity(Intent(this, ConfigActivity::class.java))
            finish()
        }

        countDownTimer = object : CountDownTimer(3000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) + 1
                textCountdown.text = seconds.toString()
            }

            override fun onFinish() {
                textCountdown.text = getString(R.string.launching)
                launchShortcut()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun launchShortcut() {
        val packageName = repo.getPackageName()
        val shortcutId = repo.getShortcutId()

        if (packageName == null || shortcutId == null) {
            redirectToConfig()
            return
        }

        if (!repo.isTargetAppInstalled()) {
            Toast.makeText(
                this,
                getString(R.string.error_app_uninstalled),
                Toast.LENGTH_LONG
            ).show()
            redirectToConfig()
            return
        }

        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            val query = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
                setPackage(packageName)
            }
            val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle())
            val target = shortcuts?.find { it.id == shortcutId }
            if (target != null) {
                launcherApps.startShortcut(target, null, null)
                finish()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.error_shortcut_invalid),
                    Toast.LENGTH_LONG
                ).show()
                redirectToConfig()
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                getString(R.string.error_shortcut_invalid),
                Toast.LENGTH_LONG
            ).show()
            redirectToConfig()
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                getString(R.string.error_permission),
                Toast.LENGTH_LONG
            ).show()
            redirectToConfig()
        }
    }

    private fun redirectToConfig() {
        repo.clear()
        startActivity(Intent(this, ConfigActivity::class.java))
        finish()
    }
}
