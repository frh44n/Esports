package com.example.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

object MinesSoundPlayer {
    fun playClickSound() {
        // High frequency short crisp blip for instant tactile press feedback
        playTone(listOf(1046.50), 30) 
    }

    fun playGemSound() {
        playTone(listOf(523.25, 659.25, 783.99), 180) // C5, E5, G5 major triad quick chime
    }

    fun playExplodeSound() {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 700
                val numSamples = durationMs * sampleRate / 1000
                val generatedSnd = ByteArray(2 * numSamples)

                val random = java.util.Random()
                var idx = 0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    
                    // Pitch sweep from 150 Hz down to 30 Hz for rumble
                    val freq = 150.0 * Math.exp(-8.0 * t) + 30.0
                    val bassWave = sin(2.0 * Math.PI * freq * t)
                    
                    // White noise for initial blast sound
                    val noiseSample = random.nextDouble() * 2.0 - 1.0
                    
                    val progress = i.toDouble() / numSamples
                    
                    // Envelopes: Noise decays quickly, low rumble sustains longer
                    val noiseEnvelope = Math.max(0.0, 1.0 - progress * 4.0) // cuts out after 25% of duration
                    val bassEnvelope = Math.pow(1.0 - progress, 2.5) // smooth exponential decay
                    
                    val mixedSample = (noiseSample * 0.65 * noiseEnvelope) + (bassWave * 0.35 * bassEnvelope)
                    val valShort = (mixedSample * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    generatedSnd.size,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playCashoutSound() {
        playToneAscending(listOf(523.25, 587.33, 659.25, 698.46, 783.99, 880.00, 987.77, 1046.50), 450) // ascending major scale sparkle
    }

    private fun playTone(frequencies: List<Double>, durationMs: Int) {
        Thread {
            try {
                val sampleRate = 44100
                val numSamples = durationMs * sampleRate / 1000
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)

                for (i in 0 until numSamples) {
                    var sum = 0.0
                    for (freq in frequencies) {
                        sum += sin(2.0 * Math.PI * i / (sampleRate / freq))
                    }
                    sample[i] = sum / frequencies.size
                }

                var idx = 0
                for (dVal in sample) {
                    // scale to maximum amplitude with a gentle fade-out envelope to avoid clicks
                    val fadeOut = if (idx > numSamples * 2 * 0.8) {
                        (numSamples * 2 - idx).toDouble() / (numSamples * 2 * 0.2)
                    } else 1.0
                    val valShort = (dVal * 32767 * fadeOut).toInt().toShort()
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    generatedSnd.size,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun playToneAscending(frequencies: List<Double>, durationMs: Int) {
        Thread {
            try {
                val sampleRate = 44100
                val stepDuration = durationMs / frequencies.size
                val numSamplesStep = stepDuration * sampleRate / 1000
                val totalSamples = numSamplesStep * frequencies.size
                val generatedSnd = ByteArray(2 * totalSamples)

                var idx = 0
                for (step in frequencies.indices) {
                    val freq = frequencies[step]
                    for (i in 0 until numSamplesStep) {
                        val dVal = sin(2.0 * Math.PI * i / (sampleRate / freq))
                        val noteFade = if (i > numSamplesStep * 0.7) {
                            (numSamplesStep - i).toDouble() / (numSamplesStep * 0.3)
                        } else 1.0
                        val valShort = (dVal * 32767 * noteFade).toInt().toShort()
                        generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                        generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                    }
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    generatedSnd.size,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
