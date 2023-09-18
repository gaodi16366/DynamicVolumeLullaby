package com.example.dynamicvolumelullaby

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.dynamicvolumelullaby.ui.theme.DynamicVolumeLullabyTheme
import com.example.dynamicvolumelullaby.ui.theme.Orange
import com.example.dynamicvolumelullaby.utils.isVivo
import java.lang.Float.max
import kotlin.math.min


const val configPath: String = "config.properties"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this.application
        applyPermission()
    }

    fun afterPermission() {
        loadSoundAndConfig(applicationContext)

        setContent {
            DynamicVolumeLullabyTheme {
                ButtonAndImage(modifier = Modifier.fillMaxSize(), isPreview = false)
            }
        }
    }

    private fun applyPermission(){
        val requestFileAccess = !Environment.isExternalStorageManager()
        val requestMic = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        if (requestFileAccess || requestMic){
            setContent{
                DynamicVolumeLullabyTheme {
                    RenderRequestPage(activity = this, requestFileAccess, requestMic)
                }
            }
        }else{
            afterPermission()
        }
    }

}

@Composable
fun ButtonAndImage(modifier: Modifier = Modifier, isPreview:Boolean = false){
    val basicVolume by basicVolumeLive.observeAsState()
    val minVolume by minVolumeLive.observeAsState()
    val maxVolume by maxVolumeLive.observeAsState()
    val currentVolume by currentVolumeLive.observeAsState()
    val currentVivoVolume by currentVolumeVivoLive.observeAsState()
    val baseFft:Array<Pair<Int,Double>>? by baseFftLive.observeAsState()
    val soundEnhance by soundEnhanceLive.observeAsState()
    val sysMaxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?:15f
    var volumeIndex = basicVolume!!

    if (!isPreview){
        saveConfig()
        volumeIndex =if (isVivo){
            currentVivoVolume!!
        }else{
            currentVolume!!.toFloat()/sysMaxVolume.toFloat()
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "Baby sound", color = Orange)
        Row(){
            Button(onClick = { recordSound(RecordType.BABY) }) {
                Image(painter = painterResource(R.drawable.baseline_fiber_manual_record_24), contentDescription = "Record")
            }
            Button(onClick = {
                stopRecord(RecordType.BABY)
            })
            {
                Image(painter = painterResource(R.drawable.baseline_stop_24), contentDescription = "Play")
            }
            Button(onClick = {
                startPlaying(typeSoundPaths[RecordType.BABY])
            }) {
                Image(painter = painterResource(R.drawable.baseline_play_arrow_24), contentDescription = "Play")
            }
            Button(onClick = {
                stopPlaying()
            })
            {
                Image(painter = painterResource(R.drawable.baseline_pause_24), contentDescription = "Play")
            }
        }
//        Text(text = baseFft!!.joinToString("\n"), color = Orange)

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Volume", color = Orange)

        Row(){
            Text(text = "basic volume ", color = Orange ,modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(100.dp)
                .padding(start = 10.dp))
            Slider(value = basicVolume!!,
                onValueChange ={
                    var nextBasicVolume =  if(it<minVolume!!){
                        minVolume
                    }else if(it>maxVolume!!){
                        maxVolume
                    }else{
                        it
                    }
                    basicVolumeLive.postValue(nextBasicVolume) },
                modifier = Modifier.padding(end= 10.dp)
            )
        }
        Row(){
            Text(text = "min volume", color = Orange ,modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(100.dp)
                .padding(start = 10.dp))
            Slider(value = minVolume!!,
                onValueChange ={ minVolumeLive.postValue( min(it, basicVolume!!)) },
                modifier = Modifier.padding(end= 10.dp))
        }
        Row(){
            Text(text = "max volume", color = Orange ,modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(100.dp)
                .padding(start = 10.dp))
            Slider(value = maxVolume!!,
                onValueChange ={ maxVolumeLive.postValue( max(it, basicVolume!!)) },
                modifier = Modifier.padding(end= 10.dp))
        }
        if (isPreview || isVivo){
            Text(text = "VIVO: media player's volume, not system volume", color = Color.Red)
        }
        Row(){
            Text(text = "current volume",
                color = Orange ,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(100.dp)
                    .padding(start = 10.dp))
            Slider(value = volumeIndex,
                onValueChange ={ /* nothing */},
                modifier = Modifier
                    .padding(end= 10.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
//
//        Text(text = "Sound Enhancement", color = Orange)
//        Row(){
//            Button(onClick = {
//                decreaseAmplifier()
//            }) {
//                Image(painter = painterResource(R.drawable.minus), contentDescription = "Record")
//            }
//
//            Text(text = "%.1f dB".format(soundEnhance),
//            color = Orange,
//            modifier= Modifier
//                .align(Alignment.CenterVertically)
//                .padding(10.dp)
//            )
//
//            Button(onClick = {
//                increaseAmplifier()
//            }) {
//                Image(painter = painterResource(R.drawable.baseline_add_24), contentDescription = "Play")
//            }
//        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Your sound", color = Orange)
        Row(){
            Button(onClick = { recordSound(RecordType.PARENT) }) {
                Image(painter = painterResource(R.drawable.baseline_fiber_manual_record_24), contentDescription = "Record")
            }
            Button(onClick = { stopRecord(RecordType.PARENT) }) {
                Image(painter = painterResource(R.drawable.baseline_stop_24), contentDescription = "Play")
            }
            Button(onClick = {
                startPlaying(typeSoundPaths[RecordType.PARENT])
            }) {
                Image(painter = painterResource(R.drawable.baseline_play_arrow_24), contentDescription = "Play")
            }
            Button(onClick = {
                stopPlaying()
            })
            {
                Image(painter = painterResource(R.drawable.baseline_pause_24), contentDescription = "Play")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Monitor baby sound", color = Orange)
        if (isPreview || isVivo){
            Text(text = "VIVO: will change media player's volume, not system volume", color = Color.Red)
        }
        Row(){
            Button(onClick = {
                recordSound(RecordType.MONITOR)
            }) {
                Image(painter = painterResource(R.drawable.baseline_play_arrow_24), contentDescription = "Play")
            }
            Button(onClick = {
                stopRecord(RecordType.MONITOR)
            })
            {
                Image(painter = painterResource(R.drawable.baseline_stop_24), contentDescription = "Play")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "White noise", color = Orange)
        Row(){
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.pure)
            }){
                Text(text = "Pure")
            }
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.blender)
            }){
                Text(text = "Blender")
            }
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.fan)
            }){
                Text(text = "Fan")
            }
        }
        Row(){
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.ocean)
            }){
                Text(text = "Ocean")
            }
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.rain)
            }){
                Text(text = "Rain")
            }
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.waterfall)
            }){
                Text(text = "Waterfall")
            }
        }
        Row(){
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.underwater)
            }){
                Text(text = "Underwater")
            }
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.train)
            }){
                Text(text = "Train")
            }
            Button(onClick = {
                stopPlaying()
                startPlaying(R.raw.waves)
            }){
                Text(text = "Waves")
            }
            Button(onClick = {
                stopPlaying()
            }){
                Image(painter = painterResource(R.drawable.baseline_stop_24), contentDescription = "Play")
            }
        }
    }

}

@Preview
@Composable
fun RenderApp(){
    ButtonAndImage(modifier = Modifier.fillMaxSize(), isPreview = true)
}

@Composable
fun RenderRequestPage(activity:MainActivity, requestFileAccess:Boolean, requestMic:Boolean){
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Need Access all files and mic permissions.", modifier = Modifier.align(
            Alignment.CenterHorizontally
        ))
        Button(onClick = {
            if (requestFileAccess){
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts(
                    "package",
                    activity.packageName, null
                )
                intent.data = uri
                activity.startActivity(intent)
            }
            if (requestMic){
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf<String>(
                        Manifest.permission.RECORD_AUDIO
                    ),
                    1
                )
            }
            activity.afterPermission()
        },
            modifier = Modifier.align(Alignment.CenterHorizontally)){
            Text(text = "Continue")
        }
    }
}

