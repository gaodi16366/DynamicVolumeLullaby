package com.example.dynamicvolumelullaby

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.dynamicvolumelullaby.utils.isVivo
import java.io.File
import java.lang.Float.max
import java.lang.Float.min
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
var audioSessionId:Int =-1

var mediaPlayer:MediaPlayer? =null


class PlayingService:Service(), MediaPlayer.OnPreparedListener {

    override fun onCreate() {
        Log.i("playing service","playing service created")

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null){
            return super.onStartCommand(null, flags, startId)
        }
        val bundle:Bundle? = intent.extras
        if (bundle !=null){
            when(bundle.getInt(ACTION_NAME, ACTION_INVALID)){
                ACTION_START -> {
                    val path=bundle.getString(PARAM_PATH)
                    val file=File(path)
                    if (file.exists() && mediaPlayer ==null){
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
                            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                            setOnPreparedListener(this@PlayingService)
                            if (isVivo){
                                setVolume(currentVolumeVivoLive.value!!, currentVolumeVivoLive.value!!)
                            }
                            prepareAsync()
                        }
                    }
                }
                ACTION_STOP -> {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        audioSessionId = -1
                        initAndSetEnhancer()
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
        initAndSetEnhancer()
        audioSessionId=mp.audioSessionId
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        audioSessionId=-1
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

        val tempVolume = basicVolume!! * amplifier

        var nextVolume =  (if(tempVolume > maxVolume!!){
            maxVolume
        }else if (tempVolume < minVolume!!){
            minVolume
        }else{
            tempVolume
        })
        if (isVivo){
            val currentVivoVolume = currentVolumeVivoLive.value!!
            val nextVolumeInFloat = if (nextVolume > currentVivoVolume){
                 calculateVolume(currentVivoVolume,AudioManager.ADJUST_RAISE)
            } else if (nextVolume < currentVivoVolume) {
                calculateVolume(currentVivoVolume,AudioManager.ADJUST_LOWER)
            }else{
                currentVivoVolume
            }
            mediaPlayer?.setVolume(nextVolumeInFloat, nextVolumeInFloat)
            currentVolumeVivoLive.postValue(nextVolumeInFloat)
        }else{
            val currentVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
            nextVolume *= audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if(nextVolume > currentVolume){
                audioManager!!.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            } else if (nextVolume < currentVolume){
                audioManager!!.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)

            }
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
    Log.i("playingservice","start service with %s".format(file?.absolutePath))
}

fun calculateVolume(currentVolume:Float, direction: Int): Float{
    val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    return max(min(currentVolume + direction.toFloat()/maxVolume.toFloat(),1f),0f)
}

fun stopPlaying() {
    val intent = Intent(context, PlayingService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_STOP)
    context?.startService(intent)
}