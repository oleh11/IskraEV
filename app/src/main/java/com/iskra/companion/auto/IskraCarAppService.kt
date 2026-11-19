package com.iskra.companion.auto

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.iskra.companion.util.AppLogger

/**
 * Android Auto CarAppService entry point
 */
class IskraCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) {
            // Debug builds only: lets the Desktop Head Unit and test hosts connect.
            AppLogger.i("ANDROID_AUTO", "Debug build: allowing all hosts")
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        }
        // Release builds: only trusted hosts (Google's Android Auto / Automotive hosts).
        return HostValidator.Builder(applicationContext)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
            .build()
    }

    override fun onCreateSession(): Session {
        AppLogger.i("ANDROID_AUTO", "onCreateSession: Initializing Iskra Android Auto Session...")
        return IskraCarSession()
    }
}
