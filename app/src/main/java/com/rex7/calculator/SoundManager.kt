package com.rex7.calculator

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.SoundEffectConstants
import android.view.View
import java.util.concurrent.Executors

object SoundManager {
    private val executor = Executors.newFixedThreadPool(2)

    fun initPool(context: Context) {
        // No sound files loaded
    }

    fun play(context: Context, view: View, soundIndex: Int, isSoundEnabled: Boolean, isEquals: Boolean = false) {
        if (!isSoundEnabled) return

        when (soundIndex) {
            0 -> {
                try { view.playSoundEffect(SoundEffectConstants.CLICK) } catch (_: Exception) {}
            }
            1 -> playTone(1200, 25, 0.6)
            2 -> playTone(950, 40, 0.45)
            3 -> playTone(450, 30, 0.8)
            4 -> playTone(280, 20, 0.7)
            5 -> playTone(1800, 35, 0.4)
            6 -> playTone(160, 25, 0.9)
            else -> {
                try { view.playSoundEffect(SoundEffectConstants.CLICK) } catch (_: Exception) {}
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
