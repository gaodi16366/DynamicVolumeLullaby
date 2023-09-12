package com.example.dynamicvolumelullaby

import android.content.Context
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.example.dynamicvolumelullaby.ui.theme.DynamicVolumeLullabyTheme
import com.zlw.main.recorderlib.RecordManager
import com.zlw.main.recorderlib.recorder.RecordHelper
import java.io.File
import java.lang.Float.max
import kotlin.math.min



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        setContent {
            DynamicVolumeLullabyTheme {
                RenderApp()
            }
        }
    }

}

@Composable
fun ButtonAndImage(modifier: Modifier = Modifier, live:LiveData<Int>){
    var result by remember { mutableStateOf(1)}
    val stat:Int? by live.observeAsState()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "Baby sound", color = Color.Yellow)
        Row(){
            Button(onClick = { recordSound(RecordType.BABY) }) {
                Image(painter = painterResource(R.drawable.baseline_fiber_manual_record_24), contentDescription = "Record")
            }
            Button(onClick = { stopRecord(RecordType.BABY) }) {
                Image(painter = painterResource(R.drawable.baseline_stop_24), contentDescription = "Play")
            }
            Button(onClick = { /*TODO*/ }) {
                Image(painter = painterResource(R.drawable.baseline_play_arrow_24), contentDescription = "Play")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(text = "Volume", color = Color.Yellow)
        var basicVolume by remember { mutableFloatStateOf(0.5f) }
        var minVolume by remember {
            mutableFloatStateOf(0f)
        }
        var amplifier by remember {
            mutableFloatStateOf(0f)
        }
        var maxVolume by remember { mutableFloatStateOf(1f) }
        val nextVolume = min(basicVolume * (1.0f + amplifier), maxVolume)
        Row(){
            Text(text = "basic volume ", color = Color.Blue ,modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(100.dp))
            Slider(value = basicVolume, onValueChange ={ basicVolume = it}, modifier = Modifier.padding(end= 10.dp) )
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
            Button(onClick = { /*TODO*/ }) {
                Image(painter = painterResource(R.drawable.baseline_play_arrow_24), contentDescription = "Play")
            }
            Button(onClick = { /*TODO*/ }) {
                Image(painter = painterResource(R.drawable.baseline_pause_24), contentDescription = "Play")
            }
        }
    }

}



@Preview
@Composable
fun RenderApp(){
    LoadSoundAndConfig()
    ButtonAndImage(modifier = Modifier.fillMaxSize(), live = myLive)
}

fun LoadSoundAndConfig() {
    TODO("Not yet implemented")
}
