package com.example.os

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator

class SoundEffectsManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_SYSTEM, 75)
    } catch (_: Exception) {
        null
    }

    fun playTapSound() {
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.5f)
                ?: toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
        } catch (_: Exception) {}
    }

    fun playLockSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 60)
        } catch (_: Exception) {
            playTapSound()
        }
    }

    fun playUnlockSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 70)
        } catch (_: Exception) {
            playTapSound()
        }
    }

    fun playNotificationSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                ringtone.play()
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            }
        } catch (_: Exception) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
