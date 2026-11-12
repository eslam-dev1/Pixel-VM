package com.example.domain.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import org.xmlpull.v1.XmlPullParser

/**
 * Metadata representing an installed icon pack theme.
 */
data class IconPackInfo(
    val packageName: String,
    val label: String
)

/**
 * Resolver that discovers installed icon packs using standard launcher theme intent queries
 * ("org.adw.launcher.THEMES" and "com.gau.go.launcherex.theme"), parses their appfilter.xml mappings,
 * and caches resolved Drawables using an LruCache for high-performance UI rendering (120Hz smooth).
 */
class IconPackResolver(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    // Cache resolved Drawables in memory (LruCache) to prevent repeated parsing during drawer/home scrolling
    private val iconCache = LruCache<String, Drawable>(150)

    // Component key -> Drawable resource name in the active icon pack
    private val iconMap = mutableMapOf<String, String>()
    private var currentLoadedPack: String? = null

    /**
     * Queries the system for all installed icon packs supporting ADW or GO launcher theme formats.
     */
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val iconPacks = mutableListOf<IconPackInfo>()

        val themeIntents = arrayOf(
            Intent("org.adw.launcher.THEMES"),
            Intent("com.gau.go.launcherex.theme")
        )

        for (intent in themeIntents) {
            val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                val appLabel = info.loadLabel(packageManager).toString()
                iconPacks.add(IconPackInfo(packageName = pkgName, label = appLabel))
            }
        }

        return iconPacks.distinctBy { it.packageName }.sortedBy { it.label }
    }

    /**
     * Loads and parses the appfilter.xml file from the target icon pack package.
     */
    private fun loadIconPack(packageName: String) {
        if (currentLoadedPack == packageName && iconMap.isNotEmpty()) return

        iconMap.clear()
        iconCache.evictAll()

        try {
            val iconPackRes = packageManager.getResourcesForApplication(packageName)
            val appFilterId = iconPackRes.getIdentifier("appfilter", "xml", packageName)

            if (appFilterId != 0) {
                val parser = iconPackRes.getXml(appFilterId)
                var eventType = parser.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawableName = parser.getAttributeValue(null, "drawable")
                        if (!component.isNullOrBlank() && !drawableName.isNullOrBlank()) {
                            iconMap[component] = drawableName
                        }
                    }
                    eventType = parser.next()
                }
                currentLoadedPack = packageName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load icon pack: $packageName", e)
        }
    }

    /**
     * Resolves an icon for a given ComponentName from the chosen icon pack.
     * Falls back to the default system application/activity icon if not found in the pack.
     */
    fun getIcon(componentName: ComponentName, packPackageName: String?): Drawable {
        if (!packPackageName.isNullOrBlank()) {
            val resolvedPackIcon = getIconFromPack(
                packageName = componentName.packageName,
                className = componentName.className,
                packPackageName = packPackageName
            )
            if (resolvedPackIcon != null) {
                return resolvedPackIcon
            }
        }

        // Fallback to original app icon
        return getFallbackIcon(componentName)
    }

    /**
     * Resolves an icon by packageName and className with fallback support.
     */
    fun getIcon(packageName: String, className: String, packPackageName: String?): Drawable? {
        if (packPackageName.isNullOrBlank()) return null
        return getIconFromPack(packageName, className, packPackageName)
    }

    private fun getIconFromPack(packageName: String, className: String, packPackageName: String): Drawable? {
        val componentKey = "ComponentInfo{$packageName/$className}"
        val cacheKey = "${packPackageName}_$componentKey"

        // Check LruCache first
        iconCache.get(cacheKey)?.let { return it }

        if (currentLoadedPack != packPackageName) {
            loadIconPack(packPackageName)
        }

        val drawableName = iconMap[componentKey] ?: return null

        return try {
            val iconPackRes = packageManager.getResourcesForApplication(packPackageName)
            val resId = iconPackRes.getIdentifier(drawableName, "drawable", packPackageName)
            if (resId != 0) {
                val drawable = iconPackRes.getDrawable(resId, null)
                iconCache.put(cacheKey, drawable)
                drawable
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving drawable $drawableName from $packPackageName", e)
            null
        }
    }

    /**
     * Resolves default system icon for a component.
     */
    private fun getFallbackIcon(componentName: ComponentName): Drawable {
        val cacheKey = "fallback_${componentName.flattenToShortString()}"
        iconCache.get(cacheKey)?.let { return it }

        val fallback = try {
            packageManager.getActivityIcon(componentName)
        } catch (e: Exception) {
            try {
                packageManager.getApplicationIcon(componentName.packageName)
            } catch (e2: Exception) {
                packageManager.defaultActivityIcon
            }
        }

        iconCache.put(cacheKey, fallback)
        return fallback
    }

    /**
     * Clears all in-memory caches.
     */
    fun clearCache() {
        iconCache.evictAll()
        iconMap.clear()
        currentLoadedPack = null
    }

    companion object {
        private const val TAG = "IconPackResolver"

        @Volatile
        private var INSTANCE: IconPackResolver? = null

        /**
         * Returns the application-wide singleton instance of [IconPackResolver]
         * to ensure consistent LruCache usage across UI components.
         */
        fun getInstance(context: Context): IconPackResolver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IconPackResolver(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
