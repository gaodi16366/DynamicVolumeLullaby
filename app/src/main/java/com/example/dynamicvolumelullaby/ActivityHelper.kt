package com.example.dynamicvolumelullaby

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioFormat.CHANNEL_IN_STEREO
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.zlw.main.recorderlib.RecordManager
import com.zlw.main.recorderlib.recorder.RecordConfig
import com.zlw.main.recorderlib.recorder.RecordHelper
import java.io.File
import java.lang.Integer.min
import java.util.Date
import java.util.LinkedList

enum class RecordType{
    BABY,
    PARENT,
    MONITOR,
    NONE
}

class ActivityHelper {
}

val recordLock: Any = Any()
var context: Application? = null
var fftDataSum: DoubleArray? = null
var count: Int =0
const val sampleNumber:Int = 10

val baseFftLive : MutableLiveData<Array<Pair<Int,Double>>> = MutableLiveData(Array(sampleNumber){Pair(0,0.0)})
val amplifierLive = MutableLiveData<Float>(1.0f)

var typeSoundPaths:MutableMap<RecordType,File> = HashMap()

var monitorFftDataSum: DoubleArray = DoubleArray(sampleNumber){0.0}
var previousDateTime: Date? = null


fun recordSound(type: RecordType){
    val appDirectory: File = context!!.filesDir
    val directory: File = File(appDirectory, type.toString())
    val rm: RecordManager = RecordManager.getInstance()
    synchronized(recordLock){
        if (rm.state == RecordHelper.RecordState.RECORDING){
            Toast.makeText(context,"other sound type still recording", Toast.LENGTH_SHORT).show()
            return
        }

        cleanRmListener(rm)
        rm.init(context,false)
        rm.changeRecordDir(directory.path+"/")
        rm.recordConfig.sampleRate = 44100
        rm.recordConfig.channelConfig= CHANNEL_IN_STEREO
        rm.changeFormat(RecordConfig.RecordFormat.MP3)
        if(type == RecordType.BABY){
            cleanFftData()
            rm.setRecordFftDataListener { recordFftData(it) }
        }
        if (type == RecordType.MONITOR){
            rm.setRecordFftDataListener{
                calculateAmplifier(it)
            }
        }
        rm.start();
    }
}

fun cleanRmListener(rm: RecordManager) {
    rm.setRecordResultListener(null)
    rm.setRecordFftDataListener(null)
}


fun stopRecord(type: RecordType){
    val rm: RecordManager = RecordManager.getInstance()
    synchronized(recordLock){
        if (!rm.recordConfig.recordDir.contains(type.toString()) || rm.state != RecordHelper.RecordState.RECORDING){
            Toast.makeText(context,"$type sound not recording", Toast.LENGTH_SHORT).show()
            return
        }
        rm.setRecordResultListener{
            resultListener(it,type)
        }
        rm.stop()
    }
}

fun recordFftData(data:ByteArray){
    val length:Int = min(fftDataSum?.size ?: data.size, data.size)

    // init fft data
    if (fftDataSum == null) {
        initFftData(length)
    }
    count++
    for (i in 0 until length){
        fftDataSum!![i] += data[i].toDouble()
    }
}

fun calculateSampleFftData(){
    val fftDataAverage: ArrayList<Pair<Int,Double>> = ArrayList()
    val length:Int = fftDataSum?.size?:0
    for (i in 0 until length){
        fftDataAverage.add(Pair(i,fftDataSum!![i] / count.toDouble()))
    }
    fftDataAverage.sortByDescending { it.second }
    var sampleFftData = Array(sampleNumber){ fftDataAverage[it] }
    baseFftLive.postValue(sampleFftData)
    Log.i(ActivityHelper::class.java.simpleName, sampleFftData.joinToString("\n"))
}

fun cleanFftData(){
    fftDataSum=null
    count=0
}

fun initFftData(length:Int){
    fftDataSum=DoubleArray(length){0.0}
    count=0
}

fun calculateAmplifier(data:ByteArray){
    var baseFftData = baseFftLive.value ?: return
    var amplifierList = LinkedList<Double>()
    baseFftData.forEach {
        amplifierList.add( data[it.first].toDouble()/it.second)
    }
    amplifierLive.postValue(amplifierList.average().toFloat())
}

fun resultListener(file:File, type: RecordType){
    when(type){
        RecordType.BABY -> {
            Toast.makeText(context,"$type sound recording completed", Toast.LENGTH_SHORT).show()
            calculateSampleFftData()
        }
        RecordType.PARENT -> Toast.makeText(context,"$type sound recording completed", Toast.LENGTH_SHORT).show()
        else -> {/*do nothing*/}
    }
    typeSoundPaths[type] = file

    // clean directory every time so that only last file left
    val directory = file.parentFile.listFiles()
                                        .filter { !it.equals(file) }
                                        .forEach{it.delete()}
}

fun startPlaying( file:File?) {
    val intent = Intent(context, PlayService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_START)
    intent.putExtra(PARAM_PATH, file)
    context?.startService(intent)
}

fun stopPlaying() {
    val intent = Intent(context, PlayService::class.java)
    intent.putExtra(ACTION_NAME, ACTION_STOP)
    context?.startService(intent)
}