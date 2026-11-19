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

/**
 * In-car battery health screen: cell balance, pack health, 12V and lifetime energy.
 */
class AutoCellDiagnosticsScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

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
            return MessageTemplate.Builder("No OBD-II adapter connected.\nPair your adapter in the Iskra phone app, or start Demo mode to preview battery health.")
                .setTitle("Battery Health  •  Disconnected")
                .setHeaderAction(Action.BACK)
                .setIcon(AutoUi.icon(carContext, R.drawable.ic_auto_chart, AutoUi.SPARK))
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

        val deltaColor = AutoUi.deltaColor(t.cells.deltaVoltageMv)
        val deltaWord = when (deltaColor) {
            AutoUi.OK -> "Balanced"
            AutoUi.WARN -> "Slight imbalance"
            else -> "Imbalanced"
        }
        val sohColor = if (t.battery.soh >= 90f) AutoUi.OK else if (t.battery.soh >= 80f) AutoUi.WARN else AutoUi.BAD
        val auxColor = AutoUi.socColor(t.auxBattery.soc)

        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_chart, deltaColor,
                "Cell Δ ${t.cells.deltaVoltageMv.toInt()} mV",
                deltaWord, deltaColor,
                "Max ${String.format("%.3f", t.cells.maxCellVoltage)} V (#${t.cells.maxCellIndex + 1})  ·  Min ${String.format("%.3f", t.cells.minCellVoltage)} V (#${t.cells.minCellIndex + 1})"
            )
        )

        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_battery, sohColor,
                "Pack health ${String.format("%.1f", t.battery.soh)}%",
                if (sohColor == AutoUi.OK) "Healthy" else if (sohColor == AutoUi.WARN) "Ageing" else "Check battery", sohColor,
                "Isolation resistance ${t.battery.isolationResistanceKohm} kΩ"
            )
        )

        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_bolt, auxColor,
                "12V battery ${String.format("%.2f", t.auxBattery.voltage)} V",
                "${t.auxBattery.soc.toInt()}% charged", auxColor,
                "Converter ${String.format("%.1f", t.auxBattery.current)} A  ·  ${t.auxBattery.temperatureC.toInt()}°C"
            )
        )

        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_station, AutoUi.SPARK,
                "Lifetime energy",
                "${String.format("%.0f", t.battery.cumulativeEnergyChargedKwh)} kWh charged", AutoUi.SPARK,
                "${String.format("%.0f", t.battery.cumulativeEnergyDischargedKwh)} kWh discharged in total"
            )
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Iskra  •  Battery Health")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
