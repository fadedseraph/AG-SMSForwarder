package com.agsmsforwarder.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ag_sms_forwarder_prefs")

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_SERVICE_ENABLED = booleanPreferencesKey("is_service_enabled")
        val ENABLED_PACKAGES = stringSetPreferencesKey("enabled_packages")
        val DESTINATION_PHONE_NUMBER = stringPreferencesKey("destination_phone_number")
        val USE_AI_FORMATTING = booleanPreferencesKey("use_ai_formatting")
        val CUSTOM_SMS_PREFIX = stringPreferencesKey("custom_sms_prefix")
        val MODEL_FILE_PATH = stringPreferencesKey("model_file_path")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        val AI_TOP_K = intPreferencesKey("ai_top_k")
        val AI_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        val CUSTOM_SYSTEM_PROMPT = stringPreferencesKey("custom_system_prompt")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppPreferences(
                isServiceEnabled = preferences[Keys.IS_SERVICE_ENABLED] ?: true,
                enabledPackages = preferences[Keys.ENABLED_PACKAGES] ?: setOf(
                    "com.chase.sig.android",
                    "com.infonow.bofa",
                    "com.wf.wellsfargomobile"
                ),
                destinationPhoneNumber = preferences[Keys.DESTINATION_PHONE_NUMBER] ?: "",
                useAiFormatting = preferences[Keys.USE_AI_FORMATTING] ?: true,
                customSmsPrefix = preferences[Keys.CUSTOM_SMS_PREFIX] ?: "[ALERT]",
                modelFilePath = preferences[Keys.MODEL_FILE_PATH] ?: "",
                aiTemperature = preferences[Keys.AI_TEMPERATURE] ?: 0.2f,
                aiTopK = preferences[Keys.AI_TOP_K] ?: 40,
                aiMaxTokens = preferences[Keys.AI_MAX_TOKENS] ?: 128,
                customSystemPrompt = preferences[Keys.CUSTOM_SYSTEM_PROMPT] ?: AppPreferences.DEFAULT_SYSTEM_PROMPT
            )
        }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setEnabledPackages(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLED_PACKAGES] = packages
        }
    }

    suspend fun togglePackage(packageName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ENABLED_PACKAGES]?.toMutableSet() ?: mutableSetOf(
                "com.chase.sig.android",
                "com.infonow.bofa",
                "com.wf.wellsfargomobile"
            )
            if (enabled) {
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            prefs[Keys.ENABLED_PACKAGES] = current
        }
    }

    suspend fun setDestinationPhoneNumber(phoneNumber: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DESTINATION_PHONE_NUMBER] = phoneNumber.trim()
        }
    }

    suspend fun setUseAiFormatting(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_AI_FORMATTING] = enabled
        }
    }

    suspend fun setCustomSmsPrefix(prefix: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_SMS_PREFIX] = prefix.trim()
        }
    }

    suspend fun setModelFilePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MODEL_FILE_PATH] = path.trim()
        }
    }

    suspend fun updateAiParameters(temperature: Float, topK: Int, maxTokens: Int, customPrompt: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_TEMPERATURE] = temperature
            prefs[Keys.AI_TOP_K] = topK
            prefs[Keys.AI_MAX_TOKENS] = maxTokens
            prefs[Keys.CUSTOM_SYSTEM_PROMPT] = customPrompt.trim()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppPreferencesRepository? = null

        fun getInstance(context: Context): AppPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferencesRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
