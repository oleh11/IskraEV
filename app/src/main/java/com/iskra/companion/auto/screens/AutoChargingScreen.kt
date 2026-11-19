package com.iskra.companion.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.iskra.companion.R
import com.iskra.companion.auto.AutoUi
import com.iskra.companion.data.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * In-car charging screen: speed, time to 80%, station limit and battery temperature.
 */
class AutoChargingScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val repository = TelemetryRepository.getInstance(carContext)
    private var telemetryJob: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        telemetryJob = CoroutineScope(Dispatchers.Main).launch {
            repository.telemetry.collect {
                invalidate()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        telemetryJob?.cancel()
        telemetryJob = null
    }

    override fun onGetTemplate(): Template {
        val t = repository.telemetry.value

        if (!t.isConnected && !t.isDemoMode) {
            return MessageTemplate.Builder("No OBD-II adapter connected.\nPair your adapter in the Iskra phone app, or start Demo mode to preview charging telemetry.")
                .setTitle("Charging  •  Disconnected")
                .setHeaderAction(Action.BACK)
                .setIcon(AutoUi.icon(carContext, R.drawable.ic_auto_station, AutoUi.SPARK))
                .addAction(
                    Action.Builder()
                        .setTitle("Start Demo")
                        .setOnClickListener {
                            repository.setDemoMode(true)
                            invalidate()
                        }
                        .build()
                )
                .build()
        }

        val isCharging = t.charging.isCharging || t.battery.powerKw > 0.5f
        val kw = if (t.charging.isCharging) t.charging.chargingPowerKw else abs(t.battery.powerKw)
        val speedColor = when {
            !isCharging -> AutoUi.SPARK
            kw >= 150f -> AutoUi.OK
            kw >= 50f -> AutoUi.WARN
            else -> AutoUi.SPARK
        }
        val socColor = AutoUi.socColor(t.battery.socDisplay)
        val warm = t.battery.avgTempC >= 21.0f

        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_bolt, speedColor,
                "${String.format("%.1f", kw)} kW",
                if (isCharging) "CHARGING" else "IDLE", speedColor,
                "${t.battery.voltage.toInt()} V  ·  ${String.format("%.1f", abs(t.battery.current))} A delivered"
            )
        )

        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_timer, socColor,
                if (isCharging) "${t.charging.estMinutesTo80} min to 80%" else "-- to 80%",
                "Battery ${t.battery.socDisplay.toInt()}%", socColor,
                "${String.format("%.2f", t.charging.sessionKwhDelivered)} kWh added this session"
            )
        )

        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_station, AutoUi.SPARK,
                "Station limit ${t.charging.evseMaxPowerKw.toInt()} kW",
                "${t.charging.evseMaxCurrentA.toInt()} A available", AutoUi.SPARK,
                "Car requests ${t.charging.requestedCurrentA.toInt()} A"
            )
        )

        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_thermo, if (warm) AutoUi.OK else AutoUi.WARN,
                "Battery ${String.format("%.1f", t.battery.avgTempC)}°C",
                if (warm) "Ready for peak speed" else "Warming up", if (warm) AutoUi.OK else AutoUi.WARN,
                if (warm) "Battery at the best temperature for fast charging" else "Heating towards 21°C (70°F) for the fastest charge"
            )
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Iskra  •  Charging")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
