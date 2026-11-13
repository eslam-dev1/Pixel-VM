package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsDataStore
import com.example.model.PixelIconShape
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PixelFeaturesDataStoreTest {

    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        settingsDataStore = SettingsDataStore(context)
    }

    @Test
    fun testPixelCustomizationPreferencesPersistence() = runTest {
        settingsDataStore.clearAll()

        settingsDataStore.setPixelIconShape(PixelIconShape.SQUIRCLE)
        settingsDataStore.setUseMaterialYouMonochrome(true)
        settingsDataStore.setTintPackIconsWithMaterialYou(true)

        val updated = settingsDataStore.settingsFlow.first()
        assertEquals(PixelIconShape.SQUIRCLE, updated.toPixelThemeSettings().iconShape)
        assertTrue(updated.useMaterialYouMonochrome)
        assertTrue(updated.tintPackIconsWithMaterialYou)
    }

    @Test
    fun testPrivateSpaceAppsAndPinPersistence() = runTest {
        settingsDataStore.clearAll()

        settingsDataStore.setPrivateSpaceApps(setOf("messages", "notes"))
        settingsDataStore.setPrivateSpacePin("2468")

        val updated = settingsDataStore.settingsFlow.first()
        assertEquals(setOf("messages", "notes"), updated.privateSpaceAppIds())
        assertTrue(SettingsDataStore.verifyPin("2468", updated.privateSpacePinHash))
        assertFalse(SettingsDataStore.verifyPin("0000", updated.privateSpacePinHash))

        settingsDataStore.setPrivateSpacePin(null)
        assertNull(settingsDataStore.settingsFlow.first().privateSpacePinHash)

        settingsDataStore.clearAll()
        val reset = settingsDataStore.settingsFlow.first()
        assertEquals(PixelIconShape.CIRCLE, reset.toPixelThemeSettings().iconShape)
        assertFalse(reset.useMaterialYouMonochrome)
        assertEquals(setOf("notes", "gallery"), reset.privateSpaceAppIds())
    }

    @Test
    fun testIconPackResolverBitmapAndMonochrome() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val resolver = com.example.domain.repository.IconPackResolver.getInstance(context)
        resolver.clearCache()

        val component = android.content.ComponentName(context.packageName, "com.example.MainActivity")
        val fallbackBmp = resolver.getIconBitmap(
            componentName = component,
            packPackageName = null,
            usePixelMonochrome = false,
            forceMonochromeAll = false
        )
        assertEquals(com.example.domain.repository.IconPackResolver.MAX_ICON_PX, fallbackBmp.width)
        assertEquals(com.example.domain.repository.IconPackResolver.MAX_ICON_PX, fallbackBmp.height)

        val monoBmp = resolver.applyPixelForcedMonochrome(fallbackBmp)
        assertEquals(fallbackBmp.width, monoBmp.width)
        assertEquals(fallbackBmp.height, monoBmp.height)

        val packMiss = resolver.getPackIconBitmapOrNull(
            packageName = "com.nonexistent.pkg",
            className = "MainActivity",
            packPackageName = null,
            forceMonochrome = true
        )
        assertNull(packMiss)
        resolver.clearCache()
    }

    @Test
    fun testIconPackResolverConcurrentCacheAndNoRecycledBitmap() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val resolver = com.example.domain.repository.IconPackResolver.getInstance(context)
        val component = android.content.ComponentName(context.packageName, "com.example.MainActivity")

        val threads = (0 until 4).map { idx ->
            Thread {
                repeat(15) { iter ->
                    val bmp = resolver.getIconBitmap(
                        componentName = component,
                        packPackageName = null,
                        usePixelMonochrome = iter % 2 == 0,
                        forceMonochromeAll = false
                    )
                    assertFalse(bmp.isRecycled)
                    if (idx == 0 && iter % 5 == 0) {
                        resolver.clearCache()
                    }
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        resolver.clearCache()
    }
}
