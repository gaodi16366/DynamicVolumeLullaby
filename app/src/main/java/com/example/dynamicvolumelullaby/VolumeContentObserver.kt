package com.example.dynamicvolumelullaby

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler

class VolumeContentObserver(handler: Handler) : ContentObserver(handler) {

    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean) {
        val currentVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        currentVolumeLive.postValue(currentVolume)
    }
}