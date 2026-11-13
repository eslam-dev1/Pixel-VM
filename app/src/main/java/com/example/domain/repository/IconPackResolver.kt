package com.example.domain.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

data class IconPackInfo(
    val packageName: String,
    val label: String
)

class IconPackResolver(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    // Memory-aware KB cache (max 12.5% of app heap)
    private val maxCacheSizeKb = (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceAtLeast(1024)
    private val bitmapCache = object : LruCache<String, Bitmap>(maxCacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    private val iconMap = ConcurrentHashMap<String, String>()
    private val calendarPrefixes = ConcurrentHashMap<String, String>()

    // Downsampled mask layers to reduce memory pressure
    private val iconBacks = mutableListOf<Bitmap>()
    private var iconMask: Bitmap? = null
    private var iconUpon: Bitmap? = null
    private var iconScale: Float = 1.0f

    @Volatile
    private var currentLoadedPack: String? = null
    private val loadLock = Any()

    companion object {
        private const val TAG = "IconPackResolver"
        const val MAX_ICON_PX = 192

        @Volatile
        private var INSTANCE: IconPackResolver? = null

        fun getInstance(context: Context): IconPackResolver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IconPackResolver(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val iconPacks = mutableListOf<IconPackInfo>()
        val themeIntents = arrayOf(
            Intent("org.adw.launcher.THEMES"),
            Intent("com.gau.go.launcherex.theme")
        )

        for (intent in themeIntents) {
            val resolveInfos = packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                val appLabel = info.loadLabel(packageManager).toString()
                iconPacks.add(IconPackInfo(packageName = pkgName, label = appLabel))
            }
        }
        return iconPacks.distinctBy { it.packageName }.sortedBy { it.label }
    }

    private fun loadIconPack(packageName: String) {
        if (currentLoadedPack == packageName && iconMap.isNotEmpty()) return

        synchronized(loadLock) {
            if (currentLoadedPack == packageName && iconMap.isNotEmpty()) return

            clearInternalState()

            try {
                val iconPackRes = packageManager.getResourcesForApplication(packageName)
                val appFilterId = iconPackRes.getIdentifier("appfilter", "xml", packageName)
                if (appFilterId == 0) return

                val parser = iconPackRes.getXml(appFilterId)
                try {
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            when (parser.name) {
                                "item" -> {
                                    val component = parser.getAttributeValue(null, "component")
                                    val drawableName = parser.getAttributeValue(null, "drawable")
                                    if (!component.isNullOrBlank() && !drawableName.isNullOrBlank()) {
                                        iconMap[component] = drawableName
                                    }
                                }
                                "calendar" -> {
                                    val component = parser.getAttributeValue(null, "component")
                                    val prefix = parser.getAttributeValue(null, "prefix")
                                    if (!component.isNullOrBlank() && !prefix.isNullOrBlank()) {
                                        calendarPrefixes[component] = prefix
                                    }
                                }
                                "iconback" -> {
                                    for (i in 0 until parser.attributeCount) {
                                        loadDownsampledBitmap(
                                            iconPackRes, packageName, parser.getAttributeValue(i)
                                        )?.let { iconBacks.add(it) }
                                    }
                                }
                                "iconmask" -> {
                                    val resName = parser.getAttributeValue(null, "img1")
                                        ?: parser.getAttributeValue(0)
                                    iconMask = loadDownsampledBitmap(iconPackRes, packageName, resName)
                                }
                                "iconupon" -> {
                                    val resName = parser.getAttributeValue(null, "img1")
                                        ?: parser.getAttributeValue(0)
                                    iconUpon = loadDownsampledBitmap(iconPackRes, packageName, resName)
                                }
                                "scale" -> {
                                    parser.getAttributeValue(null, "factor")?.toFloatOrNull()?.let {
                                        iconScale = it
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    currentLoadedPack = packageName
                } finally {
                    parser.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load icon pack: $packageName", e)
            }
        }
    }

    /**
     * Returns a ready-to-render compressed Bitmap directly for Compose without extra conversions.
     */
    fun getIconBitmap(
        componentName: ComponentName,
        packPackageName: String?,
        usePixelMonochrome: Boolean = false,
        forceMonochromeAll: Boolean = false
    ): Bitmap {
        val baseBitmap = if (!packPackageName.isNullOrBlank()) {
            getBitmapFromPack(componentName.packageName, componentName.className, packPackageName)
                ?: getMaskedOrFallbackBitmap(componentName, packPackageName)
        } else if (usePixelMonochrome && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPixelMonochromeBitmap(componentName) ?: getFallbackBitmap(componentName)
        } else {
            getFallbackBitmap(componentName)
        }

        return if (forceMonochromeAll) {
            applyPixelForcedMonochrome(baseBitmap)
        } else {
            baseBitmap
        }
    }

    /**
     * Resolves a Bitmap strictly from an active icon pack (without system fallback),
     * optionally tinted with Material You monochrome colors.
     */
    fun getPackIconBitmapOrNull(
        packageName: String,
        className: String,
        packPackageName: String?,
        forceMonochrome: Boolean = false
    ): Bitmap? {
        if (packPackageName.isNullOrBlank()) return null
        val base = getBitmapFromPack(packageName, className, packPackageName) ?: return null
        return if (forceMonochrome) applyPixelForcedMonochrome(base) else base
    }

    fun getDrawableByNameBitmap(packPackageName: String, drawableName: String): Bitmap? {
        val cacheKey = "direct_${packPackageName}_$drawableName"
        bitmapCache.get(cacheKey)?.takeUnless { it.isRecycled }?.let { return it }

        return try {
            val res = packageManager.getResourcesForApplication(packPackageName)
            loadDownsampledBitmap(res, packPackageName, drawableName)?.also {
                bitmapCache.put(cacheKey, it)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getBitmapFromPack(
        packageName: String,
        className: String,
        packPackageName: String
    ): Bitmap? {
        val componentKey = "ComponentInfo{$packageName/$className}"
        val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val cacheKey = "${packPackageName}_${componentKey}_$day"

        bitmapCache.get(cacheKey)?.takeUnless { it.isRecycled }?.let { return it }

        if (currentLoadedPack != packPackageName) {
            loadIconPack(packPackageName)
        }

        val calPrefix = calendarPrefixes[componentKey]
        val drawableName = if (calPrefix != null) "$calPrefix$day" else iconMap[componentKey]
        if (drawableName == null) return null

        return try {
            val res = packageManager.getResourcesForApplication(packPackageName)
            loadDownsampledBitmap(res, packPackageName, drawableName)?.also {
                bitmapCache.put(cacheKey, it)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadDownsampledBitmap(
        res: Resources,
        packageName: String,
        drawableName: String?
    ): Bitmap? {
        if (drawableName.isNullOrBlank()) return null
        val resId = res.getIdentifier(drawableName, "drawable", packageName)
        if (resId == 0) return null

        val drawable = ResourcesCompat.getDrawable(res, resId, null) ?: return null
        return drawable.toBitmap(MAX_ICON_PX, MAX_ICON_PX, Bitmap.Config.ARGB_8888)
    }

    private fun getFallbackBitmap(componentName: ComponentName): Bitmap {
        val cacheKey = "fallback_${componentName.flattenToShortString()}"
        bitmapCache.get(cacheKey)?.takeUnless { it.isRecycled }?.let { return it }

        val drawable = try {
            packageManager.getActivityIcon(componentName)
        } catch (e: Exception) {
            try {
                packageManager.getApplicationIcon(componentName.packageName)
            } catch (e2: Exception) {
                packageManager.defaultActivityIcon
            }
        }

        return drawable.toBitmap(MAX_ICON_PX, MAX_ICON_PX, Bitmap.Config.ARGB_8888).also {
            bitmapCache.put(cacheKey, it)
        }
    }

    private fun getMaskedOrFallbackBitmap(
        componentName: ComponentName,
        packPackageName: String
    ): Bitmap {
        val fallback = getFallbackBitmap(componentName)
        val cacheKey = "masked_${packPackageName}_${componentName.flattenToShortString()}"
        bitmapCache.get(cacheKey)?.takeUnless { it.isRecycled }?.let { return it }

        synchronized(loadLock) {
            bitmapCache.get(cacheKey)?.takeUnless { it.isRecycled }?.let { return it }
            if (iconBacks.isEmpty() && iconMask == null && iconUpon == null) return fallback
            if (fallback.isRecycled) return fallback

            val result = Bitmap.createBitmap(MAX_ICON_PX, MAX_ICON_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            val validBacks = iconBacks.filter { !it.isRecycled }
            if (validBacks.isNotEmpty()) {
                val index = (componentName.hashCode() and Int.MAX_VALUE) % validBacks.size
                canvas.drawBitmap(validBacks[index], 0f, 0f, null)
            }

            val scaledSize = (MAX_ICON_PX * iconScale).toInt().coerceAtLeast(1)
            val offset = (MAX_ICON_PX - scaledSize) / 2f
            val scaledApp = Bitmap.createScaledBitmap(fallback, scaledSize, scaledSize, true)
            canvas.drawBitmap(scaledApp, offset, offset, null)
            if (scaledApp != fallback) scaledApp.recycle()

            iconMask?.takeUnless { it.isRecycled }?.let { mask ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                }
                canvas.drawBitmap(mask, 0f, 0f, paint)
            }

            iconUpon?.takeUnless { it.isRecycled }?.let { upon ->
                canvas.drawBitmap(upon, 0f, 0f, null)
            }

            bitmapCache.put(cacheKey, result)
            return result
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getPixelMonochromeBitmap(componentName: ComponentName): Bitmap? {
        val cacheKey = "pixel_mono_${componentName.flattenToShortString()}"
        bitmapCache.get(cacheKey)?.let { return it }

        val baseIcon = try {
            packageManager.getActivityIcon(componentName)
        } catch (e: Exception) {
            return null
        }

        if (baseIcon !is AdaptiveIconDrawable) return null
        val mono = baseIcon.monochrome?.mutate() ?: return null

        val isNight = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val bgTint = ContextCompat.getColor(
            context,
            if (isNight) android.R.color.system_neutral1_800 else android.R.color.system_accent1_100
        )
        val fgTint = ContextCompat.getColor(
            context,
            if (isNight) android.R.color.system_accent1_200 else android.R.color.system_accent1_800
        )

        mono.setTint(fgTint)
        val adaptive = AdaptiveIconDrawable(ColorDrawable(bgTint), mono)
        return adaptive.toBitmap(MAX_ICON_PX, MAX_ICON_PX, Bitmap.Config.ARGB_8888).also {
            bitmapCache.put(cacheKey, it)
        }
    }

    fun applyPixelForcedMonochrome(source: Bitmap): Bitmap {
        if (source.isRecycled) return source
        val isNight = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val tintColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getColor(
                context,
                if (isNight) android.R.color.system_accent1_200 else android.R.color.system_accent1_700
            )
        } else {
            if (isNight) 0xFFD0BCFF.toInt() else 0xFF6750A4.toInt()
        }

        val cacheKey = "mono_${source.generationId}_${isNight}_$tintColor"
        bitmapCache.get(cacheKey)?.takeUnless { it.isRecycled }?.let { return it }

        val grayBitmap = Bitmap.createBitmap(source.width.coerceAtLeast(1), source.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val grayCanvas = Canvas(grayBitmap)
        val grayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        grayCanvas.drawBitmap(source, 0f, 0f, grayPaint)

        val output = Bitmap.createBitmap(grayBitmap.width, grayBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawBitmap(grayBitmap, 0f, 0f, paint)
        grayBitmap.recycle()

        bitmapCache.put(cacheKey, output)
        return output
    }

    private fun clearInternalState() {
        bitmapCache.evictAll()
        iconMap.clear()
        calendarPrefixes.clear()
        iconBacks.forEach { if (!it.isRecycled) it.recycle() }
        iconBacks.clear()
        iconMask?.let { if (!it.isRecycled) it.recycle() }
        iconMask = null
        iconUpon?.let { if (!it.isRecycled) it.recycle() }
        iconUpon = null
        iconScale = 1.0f
        currentLoadedPack = null
    }

    fun clearCache() {
        synchronized(loadLock) {
            clearInternalState()
        }
    }
}
