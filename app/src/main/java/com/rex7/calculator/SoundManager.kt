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
    private var isPoolLoaded = false
    private var currentStreamId: Int = 0

    val memeKeys = listOf(
        "sound_fahhh",
        "sound_omg",
        "sound_vine_boom",
        "sound_khatam",
        "sound_suiii",
        "sound_omg_laugh",
        "sound_cat_laugh",
        "sound_bruh",
        "sound_moye_moye",
        "sound_tunak",
        "sound_le_beta",
        "sound_tu_kaun",
        "sound_vlog_beat",
        "sound_main_hoon",
        "sound_auugh",
        "sound_aayein",
        "sound_yeh_kya_hai",
        "sound_kya_haal_hai",
        "sound_chalo",
        "sound_baigan",
        "sound_baap_re",
        "sound_ammi_bachale",
        "sound_lekin_ek_second",
        "sound_kar_kya_raha_hai",
        "sound_ye_le"
    )

    private val desiIndices = listOf(9, 10, 11, 12, 13, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)

    fun initPool(context: Context) {
        if (isPoolLoaded) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .build()
        val sp = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()

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

    private fun playInstantSound(sp: SoundPool, soundId: Int) {
        val oldStream = currentStreamId
        currentStreamId = sp.play(soundId, 1f, 1f, 1, 0, 1f)
        if (oldStream != 0) {
            sp.stop(oldStream)
        }
    }

    fun play(context: Context, view: View, soundIndex: Int, isSoundEnabled: Boolean, isEquals: Boolean = false) {
        if (!isSoundEnabled) return
        initPool(context)
        val sp = soundPool ?: return

        when (soundIndex) {
            0 -> {
                if (currentStreamId != 0) {
                    sp.stop(currentStreamId)
                    currentStreamId = 0
                }
                try { view.playSoundEffect(SoundEffectConstants.CLICK) } catch (_: Exception) {}
            }
            1 -> playTone(1200, 25, 0.6)
            2 -> playTone(950, 40, 0.45)
            3 -> playTone(450, 30, 0.8)
            4 -> playTone(280, 20, 0.7)
            5 -> playTone(1800, 35, 0.4)
            6 -> playTone(160, 25, 0.9)

            7 -> {
                val available = memeMap.values.filter { it > 0 }
                if (available.isNotEmpty()) {
                    playInstantSound(sp, available.random())
                }
            }

            8 -> {
                val available = desiIndices.mapNotNull { memeMap[it] }.filter { it > 0 }
                if (available.isNotEmpty()) {
                    playInstantSound(sp, available.random())
                }
            }

            in 9 until 34 -> {
                val mappedIdx = soundIndex - 9
                val sId = memeMap[mappedIdx]
                if (sId != null && sId > 0) {
                    playInstantSound(sp, sId)
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
