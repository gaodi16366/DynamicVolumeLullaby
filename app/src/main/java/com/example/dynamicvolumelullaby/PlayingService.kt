package com.example.dynamicvolumelullaby

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import java.io.File
import java.util.Date
import java.util.LinkedList

const val ACTION_NAME = "action_type"

const val PARAM_PATH = "path"

const val ACTION_INVALID =0
const val ACTION_START = 1
const val ACTION_STOP = 2

// monitoring data and config
const val intervalSeconds = 5
var monitorFftDataSum: DoubleArray = DoubleArray(sampleNumber){0.0}
var previousMonitorDateTime: Date? = null
var monitorCount:Int =0
var playingStatus = PlayingStatus.STOP


enum class PlayingStatus{
    PLAY,
    STOP,
    PAUSE
}

class PlayingService:Service(), MediaPlayer.OnPreparedListener {

    private var mediaPlayer:MediaPlayer? =null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null){
            return super.onStartCommand(null, flags, startId)
        }
        var bundle:Bundle? = intent.extras
        if (bundle !=null){
            var action= bundle.getInt(ACTION_NAME, ACTION_INVALID)
            when(action){
                ACTION_START -> {
                    var path=bundle.getString(PARAM_PATH)
                    var file=File(path)
                    if (file.exists() && (mediaPlayer ==null || !mediaPlayer!!.isPlaying)){
                        val myUri: Uri = Uri.fromFile(file) // initialize Uri here
                        mediaPlayer = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                            setDataSource(applicationContext, myUri)
                            isLooping = true
                            prepareAsync()
                            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                        }
                        playingStatus = PlayingStatus.PLAY
                    }
                }
                ACTION_STOP -> {
                    if (mediaPlayer?.isPlaying == true && playingStatus == PlayingStatus.PLAY){
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }
                else -> {

                }
            }

        }
        // TODO
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
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

fun startPlaying(file: File?) {
    val intent = Intent(context, PlayingService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_START)
    intent.putExtra(PARAM_PATH, file?.absolutePath)
    context?.startService(intent)
}

fun stopPlaying() {
    val intent = Intent(context, PlayingService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_STOP)
    context?.startService(intent)
}