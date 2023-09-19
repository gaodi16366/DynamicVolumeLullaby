package com.example.dynamicvolumelullaby

import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.AnyRes
import com.example.dynamicvolumelullaby.utils.isVivo
import fftlib.ByteUtils
import fftlib.FFT
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import java.io.File
import java.lang.Float.max
import java.lang.Float.min
import java.util.Date


const val PARAM_RESOURCE_ID = "resource_id"
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
                    val resourceId = bundle.getInt(PARAM_RESOURCE_ID)

                    if ((path !=null || resourceId > 0) && mediaPlayer ==null){
                        val myUri: Uri = if (path !=null){
                            Uri.fromFile(File(path)) // initialize Uri here
                        } else {
                            getResourceUri(resourceId)
                        }

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
        initVisualizer(audioSessionId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        audioSessionId=-1
    }
}

fun setNextVolume(data:ByteArray){
    val doubles: DoubleArray = ByteUtils.toHardDouble(ByteUtils.toShorts(data))
    val fft = FFT.fft(doubles, 0)
    val currentDate:Date = Date()
    if (previousMonitorDateTime == null){
        previousMonitorDateTime = currentDate
    }
    var baseFftData = baseFftLive.value ?: return

    if ((currentDate.time - previousMonitorDateTime!!.time)/1000 >= intervalSeconds && monitorCount > 0){
        synchronized(fftDataLock){
            // need volume adjustment
            val amplifier = calculateAmplifier(baseFftData)

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
                currentVolumeVivoLive.value = nextVolumeInFloat
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
            cleanAudioFft()
        }
    }

    // add data to monitorFftDataSum
    for (i in 0 until sampleNumber){
        val index=baseFftData[i].first
        monitorFftDataSum[i] += fft[index]
    }
    monitorCount++
}

private fun calculateAmplifier(
    baseFftData: Array<Pair<Int, Double>>
): Float {
    val monitoringFftDataAvg = monitorFftDataSum.map() { it -> it/ monitorCount }.toDoubleArray()
    val amplifier1 = monitoringFftDataAvg.mapIndexed(){index, it -> it/baseFftData[index].second}.average().toFloat()
    val audioFftDataAvg = DoubleArray(sampleNumber){0.0}
    baseFftData.forEachIndexed { index, pair -> audioFftDataAvg[index] = audioFftSum[pair.first] / audioFftCount }

    val regression = OLSMultipleLinearRegression()
    val x = Array(sampleNumber){
        arrayOf(baseFftData[it].second,audioFftDataAvg[it]).toDoubleArray()
    }
    regression.newSampleData(monitoringFftDataAvg,x)
    val beta = regression.estimateRegressionParameters()
    if (beta[0]>0 && beta[1] >0){
        return beta[0].toFloat()
    }
    return amplifier1
}

fun startPlaying(file: File?) {
    val intent = Intent(context, PlayingService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_START)
    intent.putExtra(PARAM_PATH, file?.absolutePath)
    context?.startService(intent)
    Log.i("playing service","start service with %s".format(file?.absolutePath))
}

fun startPlaying(resourceId: Int) {
    val intent = Intent(context, PlayingService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_START)
    intent.putExtra(PARAM_RESOURCE_ID, resourceId)
    context?.startService(intent)
    Log.i("playing service","start service with %d".format(resourceId))
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

/**
 * @param resourceId identifies an application resource
 * @return the Uri by which the application resource is accessed
 */
internal fun Context.getResourceUri(@AnyRes resourceId: Int): Uri = Uri.Builder()
    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
    .authority(packageName)
    .path(resourceId.toString())
    .build()
