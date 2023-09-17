package com.example.dynamicvolumelullaby

import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_IN_STEREO
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.zlw.main.recorderlib.RecordManager
import com.zlw.main.recorderlib.recorder.RecordConfig
import com.zlw.main.recorderlib.recorder.RecordHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Integer.min
import java.util.Properties

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
val basicVolumeLive  = MutableLiveData<Float>(0.5f)
val minVolumeLive  = MutableLiveData<Float>(0.0f)
val maxVolumeLive  = MutableLiveData<Float>(1.0f)
val currentVolumeLive = MutableLiveData<Int>(7)
val basicVolumeKey = "basicVolume"
val minVolumeKey = "minVolume"
val maxVolumeKey = "maxVolume"
val baseFftKey = "baseFft"
val enhanceKey = "enhance"

var typeSoundPaths:MutableMap<RecordType,File> = HashMap()

var audioManager:AudioManager? = null

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
        rm.recordConfig.channelConfig= AudioFormat.CHANNEL_IN_MONO
        rm.changeFormat(RecordConfig.RecordFormat.MP3)
        if(type == RecordType.BABY){
            cleanFftData()
            rm.setRecordFftDataListener { recordFftData(it) }
        }
        if (type == RecordType.MONITOR){
            if (baseFftLive.value!!.isEmpty() ||baseFftLive.value!!.any { it.second< 0.001 }){
                Toast.makeText(context,"Baby sound not recorded or too low to record correctly", Toast.LENGTH_SHORT).show()
                return
            }
            rm.setRecordFftDataListener{
                setNextVolume(it)
            }
        }
        rm.start()
        if (type == RecordType.MONITOR){
            Toast.makeText(context,"Start monitoring baby sound by mic", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(context,"$type sound start recording", Toast.LENGTH_SHORT).show()
        }

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
            if (type == RecordType.MONITOR){
                Toast.makeText(context,"Baby sound is not monitored", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(context,"$type sound not recording", Toast.LENGTH_SHORT).show()
            }
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
    saveConfig()
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

fun resultListener(file:File, type: RecordType){
    when(type){
        RecordType.BABY -> {
            Toast.makeText(context,"$type sound recording completed", Toast.LENGTH_SHORT).show()
            calculateSampleFftData()
        }
        RecordType.PARENT -> Toast.makeText(context,"$type sound recording completed", Toast.LENGTH_SHORT).show()
        RecordType.MONITOR -> Toast.makeText(context,"Monitoring stopped", Toast.LENGTH_SHORT).show()
        else -> {/*do nothing*/}
    }
    typeSoundPaths[type] = file

    // clean directory every time so that only last file left
    file.parentFile.listFiles()
                                        .filter { !it.equals(file) }
                                        .forEach{it.delete()}
}


fun saveConfig() {
    val file: File = File(context?.filesDir, configPath)
    val outputStream: OutputStream = FileOutputStream(file,false)
    val props: Properties = Properties()
    props.setProperty(minVolumeKey, minVolumeLive.value.toString())
    props.setProperty(maxVolumeKey, maxVolumeLive.value.toString())
    props.setProperty(basicVolumeKey, basicVolumeLive.value.toString())
    props.setProperty(baseFftKey, baseFftLive.value?.joinToString(";"))
    props.setProperty(enhanceKey, soundEnhanceLive.value.toString())
    props.store(outputStream,"this is config for dynamic volume lullaby")
}


fun loadSoundAndConfig(applicationContext: Context) {
    /* do nothing*/
    audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    applicationContext.contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, VolumeContentObserver(
        Handler(Looper.getMainLooper())
    ))

    // current volume event observer
    currentVolumeLive.postValue(audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC))

    // directory clean up
    var appDirectory:File = applicationContext!!.filesDir
    RecordType.values().forEach {
        val directory: File = File(appDirectory, it.toString())
        val fileList = directory?.listFiles()
        if (fileList !=null){
            fileList.sortByDescending { it -> it.lastModified() }
            typeSoundPaths[it] = fileList[0]
        }
    }

    var configFile = File(appDirectory, configPath)
    if (configFile.exists() && configFile.isFile){
        val props:Properties = Properties()
        props.load(FileInputStream(configFile))

        val minVolume= props.getProperty(minVolumeKey)?.toFloat()?:0f
        val maxVolume = props.getProperty(maxVolumeKey)?.toFloat()?:1f
        val basicVolume = props.getProperty(basicVolumeKey)?.toFloat()?:0.5f
        val baseFftString = props.getProperty(baseFftKey)
        val tempList = baseFftString.replace(Regex("[\\(\\)]"),"").split(";")
        val baseFftValue = Array<Pair<Int,Double>>(sampleNumber){Pair(0,0.0)}
        tempList.forEachIndexed { index,it ->
            val stringList = it.split(",")
            val pair:Pair<Int,Double> = Pair(stringList[0].toInt(),stringList[1].toDouble())
            baseFftValue[index]=pair
        }
        val soundEnhanceValue = props.getProperty(enhanceKey)?.toFloat()?:0f

        minVolumeLive.postValue(minVolume)
        maxVolumeLive.postValue(maxVolume)
        basicVolumeLive.postValue(basicVolume)
        baseFftLive.postValue(baseFftValue)
        soundEnhanceLive.postValue(soundEnhanceValue)
    }
}