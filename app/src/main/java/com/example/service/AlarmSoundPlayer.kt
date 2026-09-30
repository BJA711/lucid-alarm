package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AlarmSoundPlayer(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private var focusRequest: AudioFocusRequest? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val alarmAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun startPlaying(ringtoneUriString: String) {
        stopPlaying()
        requestAlarmAudioFocus()

        when (ringtoneUriString) {
            "builtin_gentle" -> playSynthesizedTone(SynthType.GENTLE_CHIME)
            "builtin_digital" -> playSynthesizedTone(SynthType.DIGITAL_PULSE)
            "builtin_bell" -> playSynthesizedTone(SynthType.CLASSIC_BELL)
            else -> {
                // Try system ringtone URI or default alarm
                val uri = if (ringtoneUriString.isNotBlank()) {
                    try {
                        Uri.parse(ringtoneUriString)
                    } catch (e: Exception) {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    }
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }

                tryPlayMediaPlayer(uri)
            }
        }
    }

    private fun requestAlarmAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(alarmAudioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* maintain alarm playback */ }
                    .build()
                focusRequest = req
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
        } catch (e: Exception) {
            Log.w("AlarmSoundPlayer", "Failed to request audio focus", e)
        }
    }

    private fun abandonAlarmAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w("AlarmSoundPlayer", "Failed to abandon audio focus", e)
        }
    }

    private fun tryPlayMediaPlayer(uri: Uri?) {
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(alarmAudioAttributes)
                setDataSource(context, uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                isLooping = true
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.w("AlarmSoundPlayer", "Could not play ringtone via MediaPlayer, falling back to gentle chime synth", e)
            playSynthesizedTone(SynthType.GENTLE_CHIME)
        }
    }

    enum class SynthType {
        GENTLE_CHIME,
        DIGITAL_PULSE,
        CLASSIC_BELL
    }

    private fun playSynthesizedTone(type: SynthType) {
        val sampleRate = 44100
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(alarmAudioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        synthJob = scope.launch {
            val buffer = ShortArray(1024)
            var sampleIndex = 0L

            while (isActive) {
                for (i in buffer.indices) {
                    val timeSec = sampleIndex.toDouble() / sampleRate
                    val sampleValue: Double = when (type) {
                        SynthType.GENTLE_CHIME -> {
                            val cycleTime = timeSec % 2.0
                            val env = Math.exp(-3.0 * cycleTime)
                            val note1 = sin(2.0 * Math.PI * 659.25 * timeSec)
                            val note2 = sin(2.0 * Math.PI * 830.61 * timeSec)
                            val note3 = sin(2.0 * Math.PI * 987.77 * timeSec)
                            val note4 = sin(2.0 * Math.PI * 1318.51 * timeSec)
                            (note1 * 0.35 + note2 * 0.3 + note3 * 0.25 + note4 * 0.2) * env
                        }
                        SynthType.DIGITAL_PULSE -> {
                            val pulseTime = timeSec % 0.6
                            if (pulseTime < 0.35) {
                                val freq = if (pulseTime < 0.17) 880.0 else 1174.66
                                sin(2.0 * Math.PI * freq * timeSec) * 0.6
                            } else {
                                0.0
                            }
                        }
                        SynthType.CLASSIC_BELL -> {
                            val cycleTime = timeSec % 1.5
                            val env = Math.exp(-2.5 * cycleTime)
                            val f1 = sin(2.0 * Math.PI * 523.25 * timeSec) * 0.5
                            val f2 = sin(2.0 * Math.PI * 1046.50 * timeSec) * 0.3
                            val f3 = sin(2.0 * Math.PI * 1567.98 * timeSec) * 0.2
                            (f1 + f2 + f3) * env
                        }
                    }

                    buffer[i] = (sampleValue * Short.MAX_VALUE * 0.85).toInt().coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    ).toShort()

                    sampleIndex++
                }

                track.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stopPlaying() {
        synthJob?.cancel()
        synthJob = null

        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundPlayer", "Error stopping AudioTrack", e)
        }
        audioTrack = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundPlayer", "Error stopping MediaPlayer", e)
        }
        mediaPlayer = null

        abandonAlarmAudioFocus()
    }
}
