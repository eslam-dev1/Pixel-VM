package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsDataStore
import com.example.os.SystemStateHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class QuickSettingsDataStoreTest {

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var systemStateHolder: SystemStateHolder
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        settingsDataStore = SettingsDataStore(context)
        systemStateHolder = SystemStateHolder(context, testScope, settingsDataStore)
    }

    @Test
    fun testQuickSettingsDataStorePersistence() = runTest(testDispatcher) {
        // Direct DataStore setters
        settingsDataStore.setQsWifi(false)
        settingsDataStore.setQsBluetooth(true)
        settingsDataStore.setQsDnd(true)
        settingsDataStore.setQsAutoRotate(false)
        settingsDataStore.setQsBatterySaver(true)
        settingsDataStore.setQsBrightness(0.45f)

        val settings = settingsDataStore.settingsFlow.first()
        assertFalse(settings.qsWifi)
        assertTrue(settings.qsBluetooth)
        assertTrue(settings.qsDnd)
        assertFalse(settings.qsAutoRotate)
        assertTrue(settings.qsBatterySaver)
        assertEquals(0.45f, settings.qsBrightness, 0.01f)
    }
}
