package com.example.shortcutproxy

import android.content.Context
import android.content.pm.PackageManager

class ShortcutRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = prefs.contains(KEY_PACKAGE)

    fun saveShortcut(packageName: String, shortcutId: String, label: String) {
        prefs.edit()
            .putString(KEY_PACKAGE, packageName)
            .putString(KEY_SHORTCUT_ID, shortcutId)
            .putString(KEY_LABEL, label)
            .apply()
    }

    fun getPackageName(): String? = prefs.getString(KEY_PACKAGE, null)
    fun getShortcutId(): String? = prefs.getString(KEY_SHORTCUT_ID, null)
    fun getLabel(): String? = prefs.getString(KEY_LABEL, null)

    fun isTargetAppInstalled(): Boolean {
        val pkg = getPackageName() ?: return false
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "shortcut_prefs"
        private const val KEY_PACKAGE = "package_name"
        private const val KEY_SHORTCUT_ID = "shortcut_id"
        private const val KEY_LABEL = "label"
    }
}
