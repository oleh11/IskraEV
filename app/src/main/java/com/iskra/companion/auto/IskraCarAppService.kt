package com.iskra.companion.auto

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.iskra.companion.util.AppLogger

/**
 * Android Auto CarAppService entry point
 */
class IskraCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        AppLogger.i("ANDROID_AUTO", "Validating Car Head Unit Host: Allowing All Hosts")
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        AppLogger.i("ANDROID_AUTO", "onCreateSession: Initializing Iskra Android Auto Session...")
        return IskraCarSession()
    }
}
