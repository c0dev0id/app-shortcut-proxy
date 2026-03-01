package de.codevoid.appshortcutproxy

import android.content.Context
import android.content.pm.PackageManager

class ShortcutRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = prefs.contains(KEY_PACKAGE)

    fun saveShortcut(packageName: String, label: String, intentUri: String) {
        prefs.edit()
            .putString(KEY_PACKAGE, packageName)
            .putString(KEY_LABEL, label)
            .putString(KEY_INTENT_URI, intentUri)
            .apply()
    }

    fun getPackageName(): String? = prefs.getString(KEY_PACKAGE, null)
    fun getLabel(): String? = prefs.getString(KEY_LABEL, null)
    fun getIntentUri(): String? = prefs.getString(KEY_INTENT_URI, null)

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
        private const val KEY_LABEL = "label"
        private const val KEY_INTENT_URI = "intent_uri"
    }
}
