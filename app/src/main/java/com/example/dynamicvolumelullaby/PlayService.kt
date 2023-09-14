package com.example.dynamicvolumelullaby

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.zlw.main.recorderlib.recorder.RecordService
import java.io.File

val ACTION_NAME = "action_type"

val PARAM_PATH = "path"

val ACTION_START = 1
val ACTION_STOP = 2

class PlayService:Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null){
            return super.onStartCommand(null, flags, startId)
        }
        var bundle:Bundle? = intent.extras
        // TODO
        return super.onStartCommand(intent, flags, startId)
    }
}