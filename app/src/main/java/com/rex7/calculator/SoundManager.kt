package com.rex7.calculator

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.view.SoundEffectConstants
import android.view.View
import java.util.concurrent.Executors
import kotlin.random.Random

object SoundManager {
    private val executor = Executors.newFixedThreadPool(2)
    private var soundPool: SoundPool? = null
    private val memeMap = HashMap<Int, Int>()
    private var isPoolLoaded = false

    private val memeKeys = listOf(
        "sound_fahhh",
        "sound_baap_re",
        "sound_omg",
        "sound_vine_boom",
        "sound_aayein",
        "sound_emotional_damage",
        "sound_suiii",
        "sound_omg_laugh",
        "sound_tudutanei",
        "sound_moye_moye",
        "sound_bruh"
    )

    fun initPool(context: Context) {
        if (isPoolLoaded) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        memeKeys.forEachIndexed { idx, name ->
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) {
                val soundId = soundPool?.load(context, resId, 1) ?: 0
                if (soundId != 0) memeMap[idx] = soundId
            }
        }
        isPoolLoaded = true
    }

    fun play(context: Context, view: View, soundIndex: Int, isSoundEnabled: Boolean, isEquals: Boolean = false) {
        if (!isSoundEnabled) return
        initPool(context)

        // Agar '=' button daba ho aur koi meme mode on ho, to suiii sound play karein
        if (isEquals && soundIndex >= 7) {
            val suiiiId = memeMap[6] // index 6 is sound_suiii
            if (suiiiId != null && suiiiId != 0) {
                soundPool?.play(suiiiId, 1f, 1f, 1, 0, 1f)
                return
            }
        }

        when (soundIndex) {
            // --- Purane 7 Classic Sounds ---
            0 -> {
                try { view.playSoundEffect(SoundEffectConstants.CLICK) } catch (_: Exception) {}
            }
            1 -> playTone(1200, 25, 0.6) // Mechanical Click
            2 -> playTone(950, 40, 0.45) // Retro Nokia Beep
            3 -> playTone(450, 30, 0.8)  // Soft Bubble Pop
            4 -> playTone(280, 20, 0.7)  // Typewriter Clack
            5 -> playTone(1800, 35, 0.4) // Digital Bip
            6 -> playTone(160, 25, 0.9)  // Subtle Wood Tap

            // --- 7: Random Mix (All Memes) ---
            7 -> {
                if (memeMap.isNotEmpty()) {
                    val randomKey = memeMap.keys.random()
                    val sId = memeMap[randomKey] ?: 0
                    if (sId != 0) soundPool?.play(sId, 1f, 1f, 1, 0, 1f)
                }
            }

            // --- 8..18: Individual Specific Memes ---
            in 8..18 -> {
                val mappedIdx = soundIndex - 8
                val sId = memeMap[mappedIdx] ?: 0
                if (sId != 0) soundPool?.play(sId, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    private fun playTone(freqHz: Int, durationMs: Int, volume: Double) {
        executor.execute {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freqHz.toDouble())
                    val envelope = 1.0 - (i.toDouble() / numSamples)
                    buffer[i] = (Math.sin(angle) * Short.MAX_VALUE * volume * envelope).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 20)
                track.release()
            } catch (_: Exception) {}
        }
    }
}
