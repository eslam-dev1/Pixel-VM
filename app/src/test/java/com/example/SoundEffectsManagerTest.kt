package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.os.SoundEffectsManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SoundEffectsManagerTest {

    private lateinit var soundEffectsManager: SoundEffectsManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        soundEffectsManager = SoundEffectsManager(context)
    }

    @Test
    fun testSoundEffectMethodsExecuteSafely() {
        // Execute sound methods to verify resilience and zero exceptions
        soundEffectsManager.playTapSound()
        soundEffectsManager.playLockSound()
        soundEffectsManager.playUnlockSound()
        soundEffectsManager.playNotificationSound()
        soundEffectsManager.release()
    }
}
