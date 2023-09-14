package com.example.dynamicvolumelullaby

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import com.example.dynamicvolumelullaby.ui.theme.DynamicVolumeLullabyTheme
import java.io.File
import java.lang.Float.max
import kotlin.math.min

const val configPath: String = "config.properties"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPermission()
        context = this.application
        setContent {
            DynamicVolumeLullabyTheme {
                RenderApp()
            }
        }
    }

    private fun applyPermission(){
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts(
                    "package",
                    packageName, null
                )
                intent.data = uri
                startActivity(intent)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1
                )
            }
        }
    }

}

@Composable
fun ButtonAndImage(modifier: Modifier = Modifier, amplifierLive:LiveData<Float>, baseFftLive: LiveData<Array<Pair<Int, Double>>>){
    val amplifier by amplifierLive.observeAsState()
    val baseFft by baseFftLive.observeAsState()
    var basicVolume by remember { mutableFloatStateOf(0.5f) }
    var minVolume by remember {
        mutableFloatStateOf(0f)
    }
    var maxVolume by remember { mutableFloatStateOf(1f) }
    var tempVolume = basicVolume * amplifier!!
    
    val nextVolume =  if(tempVolume >maxVolume ){
        maxVolume
    }else if (tempVolume < minVolume){
        minVolume
    }else{
        tempVolume
    }

    saveConfig()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "Baby sound", color = Color.Yellow)
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
            Button(onClick = { /*TODO*/ }) {
                Image(painter = painterResource(R.drawable.baseline_play_arrow_24), contentDescription = "Play")
            }
        }
        Text(text = baseFft?.joinToString("\n") ?: "No baby sound recorded yet")

        Spacer(modifier = Modifier.height(40.dp))

        Text(text = "Volume", color = Color.Yellow)

        Row(){
            Text(text = "basic volume ", color = Color.Blue ,modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(100.dp))
            Slider(value = basicVolume, onValueChange ={
                basicVolume = if(it<minVolume){
                    minVolume
                }else if(it>maxVolume){
                    maxVolume
                }else{
                    it
                }
                                                       },
                modifier = Modifier.padding(end= 10.dp) )
        }
        Row(){
            Text(text = "min volume", color = Color.Blue ,modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(100.dp))
            Slider(value = minVolume, onValueChange ={ minVolume = min(it, basicVolume) }, modifier = Modifier.padding(end= 10.dp))
        }
        Row(){
            Text(text = "max volume", color = Color.Blue ,modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(100.dp))
            Slider(value = maxVolume, onValueChange ={ maxVolume = max(it, basicVolume)}, modifier = Modifier.padding(end= 10.dp))
        }
        Row(){
            Text(text = "current volume",
                color = Color.Blue ,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(100.dp))
            Slider(value = nextVolume,
                onValueChange ={ /* nothing */},
                modifier = Modifier
                    .padding(end= 10.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(text = "Your sound", color = Color.Yellow)
        Row(){
            Button(onClick = { recordSound(RecordType.PARENT) }) {
                Image(painter = painterResource(R.drawable.baseline_fiber_manual_record_24), contentDescription = "Record")
            }
            Button(onClick = { stopRecord(RecordType.PARENT) }) {
                Image(painter = painterResource(R.drawable.baseline_stop_24), contentDescription = "Play")
            }
            Button(onClick = {
                recordSound(RecordType.MONITOR)
                startPlaying(typeSoundPaths[RecordType.PARENT])
            }) {
                Image(painter = painterResource(R.drawable.baseline_play_arrow_24), contentDescription = "Play")
            }
            Button(onClick = {
                stopRecord(RecordType.MONITOR)
                stopPlaying()
            })
            {
                Image(painter = painterResource(R.drawable.baseline_pause_24), contentDescription = "Play")
            }
        }
    }

}

fun saveConfig() {
    val file: File = File(context!!.filesDir, configPath)
    /*
    * do nothing
    * */
}


@Preview
@Composable
fun RenderApp(){
    LoadSoundAndConfig()
    ButtonAndImage(modifier = Modifier.fillMaxSize(), amplifierLive = amplifierLive, baseFftLive = baseFftLive)
}

fun LoadSoundAndConfig() {
    /* do nothing*/
}

