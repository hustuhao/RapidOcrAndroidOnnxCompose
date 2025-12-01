package com.benjaminwan.ocr.storage

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ocr_preferences",
        Context.MODE_PRIVATE
    )

    fun getModelVersion(): String {
        return prefs.getString(KEY_MODEL_VERSION, DEFAULT_VERSION) ?: DEFAULT_VERSION
    }

    fun setModelVersion(version: String) {
        prefs.edit().putString(KEY_MODEL_VERSION, version).apply()
    }

    companion object {
        private const val KEY_MODEL_VERSION = "model_version"
        private const val DEFAULT_VERSION = "V3"
    }
}
