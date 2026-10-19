package com.rex7.calculator

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.view.SoundEffectConstants
import android.view.View
import java.util.concurrent.Executors

object SoundManager {
    private val executor = Executors.newFixedThreadPool(2)
    private var soundPool: SoundPool? = null
    private val memeMap = HashMap<Int, Int>()
    private val loadedSounds = HashSet<Int>()
    private var isPoolLoaded = false
    private var currentStreamId: Int = 0

    val memeKeys = listOf(
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
        "sound_bruh",
        "sound_gareeb",
        "sound_khatam",
        "sound_jalwa",
        "sound_arpit",
        "sound_tunak",
        "sound_akshay_laugh",
        "sound_baburao",
        "sound_r2h_abesaale",
        "sound_vlog_funny",
        "sound_main_hoon"
    )

    private val desiIndices = listOf(1, 4, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

    fun initPool(context: Context) {
        if (isPoolLoaded) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder()
            .setMaxStreams(6)
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

    private fun playSingleSound(sp: SoundPool, soundId: Int) {
        if (currentStreamId != 0) {
            sp.stop(currentStreamId)
        }
        currentStreamId = sp.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun play(context: Context, view: View, soundIndex: Int, isSoundEnabled: Boolean, isEquals: Boolean = false) {
        if (!isSoundEnabled) return
        initPool(context)
        val sp = soundPool ?: return

        when (soundIndex) {
            0 -> {
                if (currentStreamId != 0) sp.stop(currentStreamId)
                try { view.playSoundEffect(SoundEffectConstants.CLICK) } catch (_: Exception) {}
            }
            1 -> playTone(1200, 25, 0.6)
            2 -> playTone(950, 40, 0.45)
            3 -> playTone(450, 30, 0.8)
            4 -> playTone(280, 20, 0.7)
            5 -> playTone(1800, 35, 0.4)
            6 -> playTone(160, 25, 0.9)

            // Mix Mode 1: Random Mix (All)
            7 -> {
                val available = memeMap.values.filter { loadedSounds.contains(it) }
                if (available.isNotEmpty()) {
                    playSingleSound(sp, available.random())
                }
            }

            // Mix Mode 2: Desi Viral Mix
            8 -> {
                val available = desiIndices.mapNotNull { memeMap[it] }.filter { loadedSounds.contains(it) }
                if (available.isNotEmpty()) {
                    playSingleSound(sp, available.random())
                }
            }

            // Individual Memes (1-to-1 exact)
            in 9 until 30 -> {
                val mappedIdx = soundIndex - 9
                val sId = memeMap[mappedIdx]
                if (sId != null && loadedSounds.contains(sId)) {
                    playSingleSound(sp, sId)
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
