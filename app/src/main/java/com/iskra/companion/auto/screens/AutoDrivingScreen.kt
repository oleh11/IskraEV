package com.iskra.companion.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.iskra.companion.data.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * In-Car Android Auto Driving Dashboard Screen
 * Modeled after gburlingame/ioniq-app (EV Dashboard) in-car layout & dynamic charging flow.
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
                // Dynamic Charging Page: Auto-transition when car begins charging
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

        // If not connected to OBD-II dongle and demo mode is off, show connection status & quick demo start
        if (!t.isConnected && !t.isDemoMode) {
            return MessageTemplate.Builder("OBD-II Adapter Disconnected\n\nPlease connect your Bluetooth adapter in the Iskra phone app, or start Demo mode to view real-time driving telemetry.")
                .setTitle("${t.vehicleModel.displayName} • Disconnected")
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

        val isRegen = t.battery.powerKw > 0.5f
        val powerLabel = if (isRegen) "REGEN +${String.format("%.1f", abs(t.battery.powerKw))} kW" else "DRAW -${String.format("%.1f", abs(t.battery.powerKw))} kW"

        val paneBuilder = Pane.Builder()

        // 1. Live Power, Speed & Gear
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("⚡ $powerLabel  •  ${t.drive.speedMph.toInt()} mph (${t.drive.speedKmh.toInt()} km/h)")
                .addText("Gear: [${t.drive.gear.label}]  •  Motor: ${t.drive.frontMotorRpm} RPM  •  Regen: Level ${t.drive.regenPaddleLevel}")
                .build()
        )

        // 2. Range & 15-Min Rolling Efficiency
        val trendSymbol = if (t.efficiency.isBetterThanAverage) "▲" else "▼"
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("🔋 Range: ${t.battery.estRangeMiles.toInt()} mi (${t.battery.estRangeKm.toInt()} km)  •  ${t.battery.socDisplay.toInt()}% SOC")
                .addText("Efficiency: ${String.format("%.1f", t.efficiency.currentMiPerKwh)} mi/kWh ($trendSymbol 15-min avg ${String.format("%.1f", t.efficiency.rollingAverageMiPerKwh)})  •  ${String.format("%.1f", t.battery.remainingEnergyKwh)} kWh left")
                .build()
        )

        // 3. Preconditioning / Fast-Charge Readiness
        val preheatText = if (t.preconditioning.timeToTargetMin > 0) {
            "⏳ Preconditioning: ~${t.preconditioning.timeToTargetMin} min to 21°C (70°F) for peak 800V DC"
        } else if (t.battery.minTempC >= 21.0f) {
            "⚡ Pack Ready: Battery ${t.battery.avgTempC.toInt()}°C (Optimal for 235+ kW charge curve)"
        } else {
            "🌡️ Pack Temp: ${String.format("%.1f", t.battery.avgTempC)}°C (Min ${t.battery.minTempC.toInt()}° / Max ${t.battery.maxTempC.toInt()}°)"
        }
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(preheatText)
                .addText("192 Cells Balanced: ΔV ${t.cells.deltaVoltageMv.toInt()} mV  •  SOH: ${t.battery.soh.toInt()}%  •  Pack: ${t.battery.voltage.toInt()} V")
                .build()
        )

        // 4. 12V Aux Battery & 4-Corner TPMS (Maximum 4 rows in Pane)
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("🚗 Tires: FL ${String.format("%.1f", t.tpms.frontLeftPsi)} • FR ${String.format("%.1f", t.tpms.frontRightPsi)} • RL ${String.format("%.1f", t.tpms.rearLeftPsi)} • RR ${String.format("%.1f", t.tpms.rearRightPsi)} PSI")
                .addText("12V Aux: ${String.format("%.2f", t.auxBattery.voltage)}V (${t.auxBattery.soc.toInt()}%)  •  LDC ${if (t.auxBattery.ldcActive) "Active" else "Standby"}  •  Cabin: ${String.format("%.1f", t.climate.cabinTempC)}°C")
                .build()
        )

        // Primary Actions (Maximum 2 actions in Pane)
        paneBuilder.addAction(
            Action.Builder()
                .setTitle("Charging")
                .setOnClickListener {
                    screenManager.push(AutoChargingScreen(carContext))
                }
                .build()
        )

        paneBuilder.addAction(
            Action.Builder()
                .setTitle("Cell Diagnostics")
                .setOnClickListener {
                    screenManager.push(AutoCellDiagnosticsScreen(carContext))
                }
                .build()
        )

        val titleSuffix = if (t.isDemoMode) " (Demo)" else " • Live"
        val templateBuilder = PaneTemplate.Builder(paneBuilder.build())
            .setTitle("${t.vehicleModel.displayName}$titleSuffix")

        // ActionStrip for Demo Mode control
        if (t.isDemoMode) {
            val actionStrip = ActionStrip.Builder()
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
            templateBuilder.setActionStrip(actionStrip)
        }

        return templateBuilder.build()
    }
}
