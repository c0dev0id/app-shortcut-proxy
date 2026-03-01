package de.codevoid.appshortcutproxy

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_HOME_ROLE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissionsAndProceed()
    }

    private fun checkPermissionsAndProceed() {
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if (!launcherApps.hasShortcutHostPermission()) {
            requestShortcutHostPermission()
            return
        }
        proceed()
    }

    private fun requestShortcutHostPermission() {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            startActivityForResult(intent, REQUEST_HOME_ROLE)
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_HOME_ROLE) {
            val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            if (launcherApps.hasShortcutHostPermission()) {
                proceed()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage(getString(R.string.permission_required_message))
            .setPositiveButton(getString(R.string.retry)) { _, _ ->
                checkPermissionsAndProceed()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun proceed() {
        val repo = ShortcutRepository(this)
        if (repo.isConfigured() && repo.isTargetAppInstalled()) {
            startActivity(Intent(this, LaunchActivity::class.java))
        } else {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
        finish()
    }
}
