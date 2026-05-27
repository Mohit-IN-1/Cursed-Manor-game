package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object AudioEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isMuted: Boolean = false

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun getMuted() = isMuted

    fun playTone(
        frequencyStart: Float,
        frequencyEnd: Float = frequencyStart,
        durationMs: Int,
        type: String = "sine",
        volume: Float = 0.5f
    ) {
        if (isMuted) return
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                if (numSamples <= 0) return@launch
                val buffer = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val progress = i.toFloat() / numSamples
                    val freq = frequencyStart + (frequencyEnd - frequencyStart) * progress
                    
                    val angle = 2.0 * Math.PI * freq * t
                    val value = when (type) {
                        "sawtooth" -> {
                            val cycle = freq * t
                            2.0f * (cycle - Math.floor(cycle.toDouble() + 0.5)).toFloat()
                        }
                        "triangle" -> {
                            val cycle = freq * t
                            val x = 2.0 * (cycle - Math.floor(cycle + 0.5))
                            (2.0 * Math.abs(x) - 1.0).toFloat()
                        }
                        "square" -> {
                            if (sin(angle) >= 0) 1.0f else -1.0f
                        }
                        else -> { // "sine"
                            sin(angle).toFloat()
                        }
                    }
                    
                    val fadeOutRange = Math.max(1, numSamples / 10)
                    val envelope = if (i > numSamples - fadeOutRange) {
                        (numSamples - i).toFloat() / fadeOutRange
                    } else if (i < fadeOutRange) {
                        i.toFloat() / fadeOutRange
                    } else {
                        1.0f
                    }
                    
                    val sample = (value * Short.MAX_VALUE * volume * envelope).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    buffer[i] = sample
                }
                
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = Math.max(minBufferSize, numSamples * 2)
                
                val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
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
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STATIC
                    )
                }
                
                audioTrack.write(buffer, 0, numSamples)
                audioTrack.play()
                
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                try {
                    audioTrack.stop()
                } catch (e: Exception) {
                    // Ignore track state errors
                }
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("AudioEngine", "Synthesizer error", e)
            }
        }
    }

    fun playFootstep() {
        playTone(75f, 30f, 80, "sine", 0.08f)
    }

    fun playArtifactSound() {
        playTone(330f, 990f, 300, "triangle", 0.12f)
    }

    fun playPowerUpSound() {
        playTone(110f, 880f, 400, "sawtooth", 0.08f)
    }

    fun playTeleportSound() {
        playTone(880f, 220f, 350, "sine", 0.10f)
    }

    fun playHurtSound() {
        playTone(100f, 40f, 300, "sawtooth", 0.25f)
    }

    fun playHeartbeat(distance: Float) {
        val volume = (0.5f - (distance * 0.04f)).coerceIn(0.05f, 0.40f)
        scope.launch {
            if (isMuted) return@launch
            playTone(50f, 20f, 130, "sine", volume)
            kotlinx.coroutines.delay(220)
            if (isMuted) return@launch
            playTone(50f, 20f, 130, "sine", volume)
        }
    }

    fun playMonsterSound(char: String) {
        when(char) {
            "👹" -> playTone(140f, 70f, 500, "sawtooth", 0.15f) // Evil Nun
            "👻" -> playTone(280f, 440f, 500, "sine", 0.08f) // Phantom Wraith
            "🤡" -> playTone(580f, 450f, 250, "triangle", 0.12f) // Jester Ghoul
            "🧟" -> playTone(55f, 40f, 500, "sawtooth", 0.20f) // Zombie Minion
            "🦍" -> playTone(45f, 30f, 600, "sine", 0.30f) // Brute
            "🌪️" -> playTone(100f, 140f, 400, "square", 0.08f) // Vortex Demon
        }
    }
}
