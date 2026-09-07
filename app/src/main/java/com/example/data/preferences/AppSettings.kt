package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("triage_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "custom_gemini_api_key"
        private const val KEY_WEBHOOK_URL = "google_chat_webhook_url"
        private const val KEY_SUPPORT_EMAIL = "support_email"
        private const val KEY_VAULT_PATH = "obsidian_vault_path"
        private const val KEY_AUTO_SEND_WEBHOOK = "auto_send_webhook"
        private const val KEY_AUTO_EXPORT_MD = "auto_export_markdown"
    }

    var customGeminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var googleChatWebhookUrl: String
        get() = prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEBHOOK_URL, value.trim()).apply()

    var supportEmail: String
        get() = prefs.getString(KEY_SUPPORT_EMAIL, "suporte@empresa.com") ?: "suporte@empresa.com"
        set(value) = prefs.edit().putString(KEY_SUPPORT_EMAIL, value.trim()).apply()

    var customVaultPath: String
        get() = prefs.getString(KEY_VAULT_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VAULT_PATH, value.trim()).apply()

    var autoSendWebhook: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SEND_WEBHOOK, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SEND_WEBHOOK, value).apply()

    var autoExportMarkdown: Boolean
        get() = prefs.getBoolean(KEY_AUTO_EXPORT_MD, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_EXPORT_MD, value).apply()
}
