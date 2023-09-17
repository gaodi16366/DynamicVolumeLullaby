package com.example.dynamicvolumelullaby

import android.media.audiofx.LoudnessEnhancer
import androidx.lifecycle.MutableLiveData

val soundEnhanceLive  = MutableLiveData<Float>(0.0f)
var loudnessEnhancer:LoudnessEnhancer? = null

fun decreaseAmplifier(){
    soundEnhanceLive.postValue(soundEnhanceLive.value?.minus(0.1f) ?: 0f)
    initAndSetEnhancer()
}

fun initAndSetEnhancer() {
    if (audioSessionId >= 0 && loudnessEnhancer == null) {
        loudnessEnhancer = LoudnessEnhancer(audioSessionId)
    } else if (audioSessionId < 0) {
        loudnessEnhancer = null
    }
    loudnessEnhancer?.setTargetGain((soundEnhanceLive.value?.times(1000)?:0).toInt())
}

fun increaseAmplifier(){
    soundEnhanceLive.postValue(soundEnhanceLive.value?.plus(0.1f) ?: 0.1f)
    initAndSetEnhancer()
}
