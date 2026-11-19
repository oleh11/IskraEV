package com.iskra.companion.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
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
 * In-car driving dashboard: four icon rows, each with a colour-coded headline
 * (power, charge/range, pack readiness, tyres) and a short detail line.
 */
class AutoDrivingScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val repository = TelemetryRepository.getInstance(carContext)
    private var telemetryJob: Job? = null
    private var hasAutoTransitionedToCharging = false

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        telemetryJob = CoroutineScope(Dispatchers.Main).launch {
            repository.telemetry.collect { t ->
                // Auto-transition when the car begins charging
                if (t.charging.isCharging && !hasAutoTransitionedToCharging) {
                    hasAutoTransitionedToCharging = true
                    screenManager.push(AutoChargingScreen(carContext))
                    return@collect
                } else if (!t.charging.isCharging) {
                    hasAutoTransitionedToCharging = false
                }
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
            return MessageTemplate.Builder("No OBD-II adapter connected.\nPair your Bluetooth adapter in the Iskra phone app, or start Demo mode to preview live telemetry.")
                .setTitle("${t.vehicleModel.displayName}  •  Disconnected")
                .setIcon(AutoUi.icon(carContext, R.drawable.ic_auto_bolt, AutoUi.SPARK))
                .addAction(
                    Action.Builder()
                        .setTitle("Start Demo")
                        .setOnClickListener {
                            repository.setDemoMode(true)
                            invalidate()
                        }
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setTitle("Refresh")
                        .setOnClickListener { invalidate() }
                        .build()
                )
                .build()
        }

        val kw = t.battery.powerKw
        val isRegen = kw > 0.5f
        val powerColor = when {
            isRegen -> AutoUi.OK
            abs(kw) > 150f -> AutoUi.BAD
            abs(kw) > 80f -> AutoUi.WARN
            else -> AutoUi.SPARK
        }
        val powerWord = if (isRegen) "REGEN" else "DRAW"

        val paneBuilder = Pane.Builder()

        // 1. Power, speed, gear
        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_bolt, powerColor,
                "${String.format("%.1f", abs(kw))} kW  ·  ${t.drive.speedKmh.toInt()} km/h",
                powerWord, powerColor,
                "${t.drive.speedMph.toInt()} mph  ·  Gear ${t.drive.gear.label}  ·  Regen level ${t.drive.regenPaddleLevel}"
            )
        )

        // 2. Charge level, range, efficiency
        val socColor = AutoUi.socColor(t.battery.socDisplay)
        val trend = if (t.efficiency.isBetterThanAverage) "▲" else "▼"
        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_battery, socColor,
                "${t.battery.socDisplay.toInt()}%  ·  ${t.battery.estRangeMiles.toInt()} mi",
                "${t.battery.estRangeKm.toInt()} km range", socColor,
                "${String.format("%.1f", t.efficiency.currentMiPerKwh)} mi/kWh $trend  ·  15-min avg ${String.format("%.1f", t.efficiency.rollingAverageMiPerKwh)}  ·  ${String.format("%.1f", t.battery.remainingEnergyKwh)} kWh left"
            )
        )

        // 3. Pack readiness for fast charging
        val packTitle: String
        val packStatus: String
        val packColor: androidx.car.app.model.CarColor
        when {
            t.preconditioning.timeToTargetMin > 0 -> {
                packTitle = "Preconditioning"
                packStatus = "~${t.preconditioning.timeToTargetMin} min to 21°C"
                packColor = AutoUi.WARN
            }
            t.battery.minTempC >= 21.0f -> {
                packTitle = "Fast-charge ready"
                packStatus = "Pack ${t.battery.avgTempC.toInt()}°C is optimal"
                packColor = AutoUi.OK
            }
            else -> {
                packTitle = "Pack ${String.format("%.1f", t.battery.avgTempC)}°C"
                packStatus = "min ${t.battery.minTempC.toInt()}° / max ${t.battery.maxTempC.toInt()}°"
                packColor = AutoUi.SPARK
            }
        }
        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_thermo, packColor,
                packTitle, packStatus, packColor,
                "Cell Δ ${t.cells.deltaVoltageMv.toInt()} mV  ·  Health ${t.battery.soh.toInt()}%  ·  ${t.battery.voltage.toInt()} V"
            )
        )

        // 4. Tyres + 12V
        val tyreColor = AutoUi.worstTyre(t.tpms.frontLeftPsi, t.tpms.frontRightPsi, t.tpms.rearLeftPsi, t.tpms.rearRightPsi)
        val tyreWord = if (tyreColor == AutoUi.OK) "Tyres OK" else "Check tyres"
        paneBuilder.addRow(
            AutoUi.row(
                carContext, R.drawable.ic_auto_car, tyreColor,
                tyreWord,
                "${String.format("%.0f", t.tpms.frontLeftPsi)} · ${String.format("%.0f", t.tpms.frontRightPsi)} · ${String.format("%.0f", t.tpms.rearLeftPsi)} · ${String.format("%.0f", t.tpms.rearRightPsi)} PSI", tyreColor,
                "12V ${String.format("%.1f", t.auxBattery.voltage)} V (${t.auxBattery.soc.toInt()}%)  ·  Cabin ${String.format("%.1f", t.climate.cabinTempC)}°C"
            )
        )

        paneBuilder.addAction(
            Action.Builder()
                .setTitle("Charging")
                .setOnClickListener { screenManager.push(AutoChargingScreen(carContext)) }
                .build()
        )
        paneBuilder.addAction(
            Action.Builder()
                .setTitle("Cells")
                .setOnClickListener { screenManager.push(AutoCellDiagnosticsScreen(carContext)) }
                .build()
        )

        val titleSuffix = if (t.isDemoMode) "Demo" else "Live"
        val templateBuilder = PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Iskra  •  ${t.vehicleModel.displayName}  •  $titleSuffix")

        if (t.isDemoMode) {
            templateBuilder.setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Stop Demo")
                            .setOnClickListener {
                                repository.setDemoMode(false)
                                invalidate()
                            }
                            .build()
                    )
                    .build()
            )
        }

        return templateBuilder.build()
    }
}
