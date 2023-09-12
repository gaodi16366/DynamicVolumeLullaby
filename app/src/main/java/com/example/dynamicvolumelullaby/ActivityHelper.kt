package com.example.dynamicvolumelullaby

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.zlw.main.recorderlib.RecordManager
import com.zlw.main.recorderlib.recorder.RecordConfig
import com.zlw.main.recorderlib.recorder.RecordHelper
import com.zlw.main.recorderlib.recorder.listener.RecordStateListener
import java.io.File
import java.lang.Integer.min

enum class RecordType{
    BABY,
    PARENT,
    MONITOR,
    NONE
}

class ActivityHelper {
}

var currentType: RecordType = RecordType.NONE
val recordLock: Any = Any()
var context: Context? = null
var fftDataSum: DoubleArray? = null
var count: Int =0
var fftDataAverage: DoubleArray? = null
var sampleFftData: Map<Int, Double>? = null

val myLive = MutableLiveData<Int>(0)

fun recordSound(type: RecordType){
    val appDirectory: File = context!!.filesDir
    val directory: File = File(appDirectory, type.toString())
    val rm: RecordManager = RecordManager.getInstance()
    synchronized(recordLock){
        if (rm.state == RecordHelper.RecordState.RECORDING){
            Toast.makeText(context,"other sound type still recording", Toast.LENGTH_SHORT).show()
            return
        }
        rm.changeRecordDir(directory.path)
        if(type == RecordType.BABY){
            cleanFftData()
            rm.setRecordFftDataListener { data ->
                run {
                    recordFftData(data)
                }
            }
        }
        rm.changeFormat(RecordConfig.RecordFormat.WAV)

        rm.start();
    }
}


fun stopRecord(type: RecordType){
    val rm: RecordManager = RecordManager.getInstance()
    synchronized(recordLock){
        if (!rm.recordConfig.recordDir.contains(type.toString()) || rm.state != RecordHelper.RecordState.RECORDING){
            Toast.makeText(context,"$type sound not recording", Toast.LENGTH_SHORT).show()
            return
        }
        rm.setRecordStateListener(object: RecordStateListener {
            override fun onStateChange(state: RecordHelper.RecordState){
                if (state == RecordHelper.RecordState.FINISH){
                    Toast.makeText(context,"$type sound recording completed", Toast.LENGTH_SHORT).show()
                    rm.setRecordStateListener(null)
                }
            }

            override fun onError(error: String){
                /*
                * do nothing
                * */
            }
        })
        rm.stop()
    }
}

fun recordFftData(data:ByteArray){
    val length:Int = min(fftDataSum?.size ?: data.size, data.size)

    // init fft data
    if (fftDataSum == null){
        fftDataSum=DoubleArray(length)
        fftDataAverage=DoubleArray(length)
        count=0
        sampleFftData= HashMap<Int, Double>()
    }

    
    myLive.postValue(myLive.value?.plus(1) ?: 0)
}

fun cleanFftData(){
    fftDataSum=null
    fftDataAverage=null
    count=0
    sampleFftData= null
}