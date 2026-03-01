package de.codevoid.appshortcutproxy

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = ShortcutRepository(this)
        if (repo.isConfigured() && repo.isTargetAppInstalled()) {
            startActivity(Intent(this, LaunchActivity::class.java))
        } else {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
        finish()
    }
}
