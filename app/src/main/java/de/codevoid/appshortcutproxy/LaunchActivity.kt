package de.codevoid.appshortcutproxy

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
                launchActivity()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun launchActivity() {
        val uri = repo.getIntentUri()
        val packageName = repo.getPackageName()

        if (uri == null || packageName == null) {
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

        try {
            val intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
            val component = intent.component
            if (component == null || component.packageName != packageName) {
                Toast.makeText(
                    this,
                    getString(R.string.error_shortcut_invalid),
                    Toast.LENGTH_LONG
                ).show()
                redirectToConfig()
                return
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
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
