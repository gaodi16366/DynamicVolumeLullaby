package com.example.dynamicvolumelullaby

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler

class VolumeContentObserver : ContentObserver {
    private val audioManager:AudioManager

    constructor(context:Context,handler: Handler?) : super(handler) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean) {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        currentVolumeLive.postValue(currentVolume)
    }
}