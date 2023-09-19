package com.example.dynamicvolumelullaby

import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener
import fftlib.ByteUtils
import fftlib.FFT
import kotlin.math.abs

var visualizer: Visualizer? = null
private const val FFT_ARRAY_SIZE = 256

var audioFftSum: DoubleArray = DoubleArray(FFT_ARRAY_SIZE){0.0}
var audioFftCount: Int = 0

var fftDataLock:Any = Any()

val fftCaptureListener: OnDataCaptureListener = object:OnDataCaptureListener{
    override fun onWaveFormDataCapture(
        visualizer: Visualizer?,
        waveform: ByteArray?,
        samplingRate: Int
    ) {
        /*
        * do nothing
        * */
    }

    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
        val doubles = fft?.map { abs(it.toDouble()) }?.toDoubleArray()
        val fft = doubles.let { FFT.fft(it, 0) }
        synchronized(fftDataLock){
            fft?.forEachIndexed{
                index,it ->
                run {
                    audioFftSum[index] += it
                }
            }
            audioFftCount++
        }

    }

}

fun initVisualizer(audioSessionId:Int){
    if (visualizer !=null){
        visualizer!!.release()
    }
    visualizer=Visualizer(audioSessionId)
    visualizer?.captureSize = FFT_ARRAY_SIZE * 2 // visualizer use 8bit pcm, so it only need 2 times the fft array size
    val maxcaptureRate=Visualizer.getMaxCaptureRate()
    val status=visualizer?.setDataCaptureListener(fftCaptureListener, maxcaptureRate,true,true)
    visualizer?.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
    visualizer?.enabled = true
}

// should get lock before run this method
fun cleanAudioFft(){
    synchronized(fftDataLock) {
        audioFftCount = 0
        audioFftSum = DoubleArray(FFT_ARRAY_SIZE) { 0.0 }
    }
}

