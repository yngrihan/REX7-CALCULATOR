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
    private val loadedSounds = HashSet<Int>()
    private var isPoolLoaded = false

    val memeKeys = listOf(
        "sound_fahhh",               // 0 (Dialog: 8)
        "sound_baap_re",              // 1 (Dialog: 9)
        "sound_omg",                  // 2 (Dialog: 10)
        "sound_vine_boom",            // 3 (Dialog: 11)
        "sound_aayein",               // 4 (Dialog: 12)
        "sound_emotional_damage",     // 5 (Dialog: 13)
        "sound_suiii",                // 6 (Dialog: 14)
        "sound_omg_laugh",            // 7 (Dialog: 15)
        "sound_tudutanei",            // 8 (Dialog: 16)
        "sound_moye_moye",            // 9 (Dialog: 17)
        "sound_bruh",                 // 10 (Dialog: 18)
        "sound_gareeb",               // 11 (Dialog: 19)
        "sound_khatam",               // 12 (Dialog: 20)
        "sound_jalwa",                // 13 (Dialog: 21)
        "sound_arpit",                // 14 (Dialog: 22)
        "sound_tunak",                // 15 (Dialog: 23)
        "sound_akshay_laugh",         // 16 (Dialog: 24)
        "sound_baburao",              // 17 (Dialog: 25)
        "sound_r2h_abesaale",         // 18 (Dialog: 26)
        "sound_r2h_scream",           // 19 (Dialog: 27)
        "sound_r2h_bhaago"            // 20 (Dialog: 28)
    )

    fun initPool(context: Context) {
        if (isPoolLoaded) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        sp.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
            }
        }

        memeKeys.forEachIndexed { idx, name ->
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) {
                val sId = sp.load(context, resId, 1)
                memeMap[idx] = sId
            }
        }
        soundPool = sp
        isPoolLoaded = true
    }

    fun play(context: Context, view: View, soundIndex: Int, isSoundEnabled: Boolean, isEquals: Boolean = false) {
        if (!isSoundEnabled) return
        initPool(context)
        val sp = soundPool ?: return

        // = button: Khatam Bye Bye ya Jalwa Hai Hamara ka jabardast finish
        if (isEquals && soundIndex >= 7) {
            val khatamId = memeMap[12]
            val jalwaId = memeMap[13]
            val targetId = if (Random.nextBoolean() && jalwaId != null && loadedSounds.contains(jalwaId)) jalwaId else khatamId
            if (targetId != null && loadedSounds.contains(targetId)) {
                sp.play(targetId, 1f, 1f, 1, 0, 1f)
                return
            }
        }

        when (soundIndex) {
            // --- 0..6: Purane 7 Classic Sounds ---
            0 -> {
                try { view.playSoundEffect(SoundEffectConstants.CLICK) } catch (_: Exception) {}
            }
            1 -> playTone(1200, 25, 0.6) // Mechanical Click
            2 -> playTone(950, 40, 0.45) // Retro Nokia Beep
            3 -> playTone(450, 30, 0.8)  // Soft Bubble Pop
            4 -> playTone(280, 20, 0.7)  // Typewriter Clack
            5 -> playTone(1800, 35, 0.4) // Digital Bip
            6 -> playTone(160, 25, 0.9)  // Subtle Wood Tap

            // --- 7: Random Mix (Saare Memes) ---
            7 -> {
                val available = memeMap.values.filter { loadedSounds.contains(it) }
                if (available.isNotEmpty()) {
                    val sId = available.random()
                    sp.play(sId, 1f, 1f, 1, 0, 1f)
                }
            }

            // --- 8..28: Har Ek Individual Viral Sound ---
            in 8..28 -> {
                val mappedIdx = soundIndex - 8
                val sId = memeMap[mappedIdx]
                if (sId != null && loadedSounds.contains(sId)) {
                    sp.play(sId, 1f, 1f, 1, 0, 1f)
                }
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
