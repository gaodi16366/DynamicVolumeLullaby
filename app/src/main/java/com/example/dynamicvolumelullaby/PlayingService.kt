package com.example.dynamicvolumelullaby

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import java.io.File
import java.util.Date
import java.util.LinkedList

val ACTION_NAME = "action_type"

val PARAM_PATH = "path"

val ACTION_START = 1
val ACTION_STOP = 2

// monitoring data and config
const val intervalSeconds = 5
var monitorFftDataSum: DoubleArray = DoubleArray(sampleNumber){0.0}
var previousMonitorDateTime: Date? = null
var monitorCount:Int =0

class PlayingService:Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null){
            return super.onStartCommand(null, flags, startId)
        }
        var bundle:Bundle? = intent.extras
        // TODO
        return super.onStartCommand(intent, flags, startId)
    }
}

fun setNextVolume(data:ByteArray){
    val currentDate:Date = Date()
    if (previousMonitorDateTime == null){
        previousMonitorDateTime = currentDate
    }
    var baseFftData = baseFftLive.value ?: return
    var amplifierList = LinkedList<Double>()

    if ((currentDate.time - previousMonitorDateTime!!.time)/1000 >= intervalSeconds && monitorCount > 0){
        // need volume adjustment
        for (i in 0 until sampleNumber){
            amplifierList.add(monitorFftDataSum!![i]/baseFftData[i].second/ monitorCount)
        }
        val amplifier=amplifierList.average().toFloat()

        val basicVolume = basicVolumeLive.value
        val maxVolume = maxVolumeLive.value
        val minVolume = minVolumeLive.value

        val audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val tempVolume = basicVolume!! * amplifier
        val nextVolume =  ((if(tempVolume > maxVolume!!){
            maxVolume
        }else if (tempVolume < minVolume!!){
            minVolume
        }else{
            tempVolume
        })*audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()).toInt()

        if(nextVolume > currentVolume){
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            currentVolumeLive.postValue(nextVolume)
        } else if (nextVolume < currentVolume){
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
//            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            currentVolumeLive.postValue(nextVolume)
        }

        // reset monitoring fft data
        monitorFftDataSum = DoubleArray(sampleNumber){0.0}
        monitorCount = 0
        previousMonitorDateTime = currentDate
    }

    // add data to monitorFftDataSum
    for (i in 0 until sampleNumber){
        val index=baseFftData[i].first
        monitorFftDataSum[i] += data[index].toDouble()
    }
    monitorCount++
}

fun startMonitoring(file: File?) {
    val intent = Intent(context, PlayingService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_START)
    intent.putExtra(PARAM_PATH, file)
    context?.startService(intent)
}

fun stopMonitoring() {
    val intent = Intent(context, PlayingService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_STOP)
    context?.startService(intent)
}