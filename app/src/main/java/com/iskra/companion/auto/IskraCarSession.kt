package com.iskra.companion.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.iskra.companion.auto.screens.AutoDrivingScreen
import com.iskra.companion.util.AppLogger

class IskraCarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        AppLogger.i("ANDROID_AUTO", "onCreateScreen: Rendering in-car AutoDrivingScreen")
        return AutoDrivingScreen(carContext)
    }
}
