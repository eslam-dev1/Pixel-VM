package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.PixelIconShape
import com.example.model.PixelThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixelsim_settings")

data class SystemSettings(
    val darkMode: String = "dark", // "system", "light", "dark"
    val wallpaperId: String = "wp_obsidian_eclipse",
    val hashedPin: String? = null,
    val showBezel: Boolean = false, // Bezel OFF by default on real phone
    val fontScale: Float = 1.0f,
    // Quick Settings States
    val qsWifi: Boolean = true,
    val qsBluetooth: Boolean = false,
    val qsDnd: Boolean = false,
    val qsAutoRotate: Boolean = true,
    val qsBatterySaver: Boolean = false,
    val qsBrightness: Float = 0.8f,
    // Notification Preferences
    val notifClockEnabled: Boolean = true,
    val notifNotesEnabled: Boolean = true,
    val notifMessagesEnabled: Boolean = true,
    val notifLockScreenVisible: Boolean = true,
    // Device customization
    val bodyColor: String = "obsidian",
    val smoothScrolling120Hz: Boolean = true,
    val deviceProfileStyle: String = "honor_x9d",
    val disableEdgeBack: Boolean = false,
    val selectedIconPack: String? = null,
    val searchEngine: String = "google", // "google", "duckduckgo", "bing"
    // Pixel Material You & Icon Shape Customization
    val pixelIconShape: String = PixelIconShape.CIRCLE.name,
    val useMaterialYouMonochrome: Boolean = false,
    val tintPackIconsWithMaterialYou: Boolean = false,
    // Android 15 Private Space
    val privateSpaceAppsCsv: String = "notes,gallery",
    val privateSpacePinHash: String? = null
) {
    fun toPixelThemeSettings(): PixelThemeSettings {
        val parsedShape = try {
            PixelIconShape.valueOf(pixelIconShape)
        } catch (_: Exception) {
            PixelIconShape.CIRCLE
        }
        return PixelThemeSettings(
            useMaterialYouMonochrome = useMaterialYouMonochrome,
            tintPackIconsWithMaterialYou = tintPackIconsWithMaterialYou,
            iconShape = parsedShape
        )
    }

    fun privateSpaceAppIds(): Set<String> {
        return privateSpaceAppsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val WALLPAPER_ID = stringPreferencesKey("wallpaper_id")
        val HASHED_PIN = stringPreferencesKey("hashed_pin")
        val SHOW_BEZEL = booleanPreferencesKey("show_bezel")
        val FONT_SCALE = floatPreferencesKey("font_scale")

        val QS_WIFI = booleanPreferencesKey("qs_wifi")
        val QS_BLUETOOTH = booleanPreferencesKey("qs_bluetooth")
        val QS_DND = booleanPreferencesKey("qs_dnd")
        val QS_AUTO_ROTATE = booleanPreferencesKey("qs_auto_rotate")
        val QS_BATTERY_SAVER = booleanPreferencesKey("qs_battery_saver")
        val QS_BRIGHTNESS = floatPreferencesKey("qs_brightness")

        val NOTIF_CLOCK = booleanPreferencesKey("notif_clock")
        val NOTIF_NOTES = booleanPreferencesKey("notif_notes")
        val NOTIF_MESSAGES = booleanPreferencesKey("notif_messages")
        val NOTIF_LOCK_SCREEN = booleanPreferencesKey("notif_lock_screen")

        val BODY_COLOR = stringPreferencesKey("body_color")
        val SMOOTH_120HZ = booleanPreferencesKey("smooth_120hz")
        val DEVICE_PROFILE_STYLE = stringPreferencesKey("device_profile_style")
        val DISABLE_EDGE_BACK = booleanPreferencesKey("disable_edge_back")
        val ICON_PACK = stringPreferencesKey("selected_icon_pack")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")

        val PIXEL_ICON_SHAPE = stringPreferencesKey("pixel_icon_shape")
        val MATERIAL_YOU_MONOCHROME = booleanPreferencesKey("material_you_monochrome")
        val TINT_PACK_ICONS_MONOCHROME = booleanPreferencesKey("tint_pack_icons_monochrome")

        val PRIVATE_SPACE_APPS_CSV = stringPreferencesKey("private_space_apps_csv")
        val PRIVATE_SPACE_PIN_HASH = stringPreferencesKey("private_space_pin_hash")
    }

    val settingsFlow: Flow<SystemSettings> = context.dataStore.data.map { prefs ->
        SystemSettings(
            darkMode = prefs[Keys.DARK_MODE] ?: "dark",
            wallpaperId = prefs[Keys.WALLPAPER_ID] ?: "wp_obsidian_eclipse",
            hashedPin = prefs[Keys.HASHED_PIN],
            showBezel = prefs[Keys.SHOW_BEZEL] ?: false,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            qsWifi = prefs[Keys.QS_WIFI] ?: true,
            qsBluetooth = prefs[Keys.QS_BLUETOOTH] ?: false,
            qsDnd = prefs[Keys.QS_DND] ?: false,
            qsAutoRotate = prefs[Keys.QS_AUTO_ROTATE] ?: true,
            qsBatterySaver = prefs[Keys.QS_BATTERY_SAVER] ?: false,
            qsBrightness = prefs[Keys.QS_BRIGHTNESS] ?: 0.8f,
            notifClockEnabled = prefs[Keys.NOTIF_CLOCK] ?: true,
            notifNotesEnabled = prefs[Keys.NOTIF_NOTES] ?: true,
            notifMessagesEnabled = prefs[Keys.NOTIF_MESSAGES] ?: true,
            notifLockScreenVisible = prefs[Keys.NOTIF_LOCK_SCREEN] ?: true,
            bodyColor = prefs[Keys.BODY_COLOR] ?: "obsidian",
            smoothScrolling120Hz = prefs[Keys.SMOOTH_120HZ] ?: true,
            deviceProfileStyle = prefs[Keys.DEVICE_PROFILE_STYLE] ?: "honor_x9d",
            disableEdgeBack = prefs[Keys.DISABLE_EDGE_BACK] ?: false,
            selectedIconPack = prefs[Keys.ICON_PACK],
            searchEngine = prefs[Keys.SEARCH_ENGINE] ?: "google",
            pixelIconShape = prefs[Keys.PIXEL_ICON_SHAPE] ?: PixelIconShape.CIRCLE.name,
            useMaterialYouMonochrome = prefs[Keys.MATERIAL_YOU_MONOCHROME] ?: false,
            tintPackIconsWithMaterialYou = prefs[Keys.TINT_PACK_ICONS_MONOCHROME] ?: false,
            privateSpaceAppsCsv = prefs[Keys.PRIVATE_SPACE_APPS_CSV] ?: "notes,gallery",
            privateSpacePinHash = prefs[Keys.PRIVATE_SPACE_PIN_HASH]
        )
    }

    suspend fun setBodyColor(colorId: String) {
        context.dataStore.edit { it[Keys.BODY_COLOR] = colorId }
    }

    suspend fun setSmoothScrolling120Hz(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMOOTH_120HZ] = enabled }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }

    suspend fun setWallpaperId(id: String) {
        context.dataStore.edit { it[Keys.WALLPAPER_ID] = id }
    }

    suspend fun setPin(pin: String?) {
        context.dataStore.edit { prefs ->
            if (pin.isNullOrBlank()) {
                prefs.remove(Keys.HASHED_PIN)
            } else {
                prefs[Keys.HASHED_PIN] = hashPin(pin)
            }
        }
    }

    suspend fun setShowBezel(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_BEZEL] = show }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale }
    }

    suspend fun setQsWifi(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QS_WIFI] = enabled }
    }

    suspend fun setQsBluetooth(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QS_BLUETOOTH] = enabled }
    }

    suspend fun setQsDnd(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QS_DND] = enabled }
    }

    suspend fun setQsAutoRotate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QS_AUTO_ROTATE] = enabled }
    }

    suspend fun setQsBatterySaver(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QS_BATTERY_SAVER] = enabled }
    }

    suspend fun setQsBrightness(brightness: Float) {
        context.dataStore.edit { it[Keys.QS_BRIGHTNESS] = brightness }
    }

    suspend fun setNotifClockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_CLOCK] = enabled }
    }

    suspend fun setNotifNotesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_NOTES] = enabled }
    }

    suspend fun setNotifMessagesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_MESSAGES] = enabled }
    }

    suspend fun setNotifLockScreenVisible(visible: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_LOCK_SCREEN] = visible }
    }

    suspend fun setDeviceProfileStyle(style: String) {
        context.dataStore.edit { it[Keys.DEVICE_PROFILE_STYLE] = style }
    }

    suspend fun setDisableEdgeBack(disabled: Boolean) {
        context.dataStore.edit { it[Keys.DISABLE_EDGE_BACK] = disabled }
    }

    suspend fun setIconPack(packageName: String?) {
        context.dataStore.edit { prefs ->
            if (packageName == null) prefs.remove(Keys.ICON_PACK)
            else prefs[Keys.ICON_PACK] = packageName
        }
    }

    suspend fun setSearchEngine(engine: String) {
        context.dataStore.edit { it[Keys.SEARCH_ENGINE] = engine }
    }

    suspend fun setPixelIconShape(shape: PixelIconShape) {
        context.dataStore.edit { it[Keys.PIXEL_ICON_SHAPE] = shape.name }
    }

    suspend fun setUseMaterialYouMonochrome(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MATERIAL_YOU_MONOCHROME] = enabled }
    }

    suspend fun setTintPackIconsWithMaterialYou(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TINT_PACK_ICONS_MONOCHROME] = enabled }
    }

    suspend fun setPrivateSpaceApps(appIds: Set<String>) {
        val csv = appIds.map { it.trim() }.filter { it.isNotEmpty() }.sorted().joinToString(",")
        context.dataStore.edit { it[Keys.PRIVATE_SPACE_APPS_CSV] = csv }
    }

    suspend fun setPrivateSpacePin(pin: String?) {
        context.dataStore.edit { prefs ->
            if (pin.isNullOrBlank()) {
                prefs.remove(Keys.PRIVATE_SPACE_PIN_HASH)
            } else {
                prefs[Keys.PRIVATE_SPACE_PIN_HASH] = hashPin(pin)
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        fun hashPin(pin: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun verifyPin(enteredPin: String, storedHash: String?): Boolean {
            if (storedHash.isNullOrBlank()) return true
            return hashPin(enteredPin) == storedHash
        }

        fun getSearchUrl(engine: String, query: String): String {
            val encoded = try {
                java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20")
            } catch (_: Exception) {
                query
            }
            return when (engine.lowercase()) {
                "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
                "bing" -> "https://www.bing.com/search?q=$encoded"
                else -> "https://www.google.com/search?q=$encoded"
            }
        }
    }
}
