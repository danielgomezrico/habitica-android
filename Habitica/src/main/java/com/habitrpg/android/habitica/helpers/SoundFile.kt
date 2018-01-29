package com.habitrpg.android.habitica.helpers

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import java.io.File


class SoundFile(val theme: String, private val fileName: String) : MediaPlayer.OnCompletionListener {
    var file: File? = null
    private var playerPrepared: Boolean = false
    private var isPlaying: Boolean = false

    val webUrl: String
        get() = "https://habitica.com/assets/audio/$theme/$fileName.mp3"

    val filePath: String
        get() = theme + "_" + fileName + ".mp3"

    init {
    }

    fun play() {
        if (isPlaying) {
            return
        }

        val m = MediaPlayer()

        m.setOnCompletionListener { mp ->
            isPlaying = false
            mp.release()
        }

        try {
            m.setDataSource(file?.path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val attributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .build()
                m.setAudioAttributes(attributes)
            } else {
                m.setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
            }
            m.prepare()

            playerPrepared = true
            m.setVolume(100f, 100f)
            m.isLooping = false
            isPlaying = true
            m.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        isPlaying = false
    }
}
