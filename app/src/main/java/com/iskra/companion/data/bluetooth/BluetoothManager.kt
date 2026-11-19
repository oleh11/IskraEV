package com.iskra.companion.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import com.iskra.companion.data.model.ConnectionState
import com.iskra.companion.data.model.DiscoveredBluetoothDevice
import com.iskra.companion.util.AppLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Universal High-Reliability Bluetooth OBD-II Engine.
 * Supports all Bluetooth Classic (RFCOMM/SPP) and BLE 4.0/4.2/5.0/5.2 GATT Codecs
 * reverse-engineered from Car Scanner, Torque, and OBDLink.
 *
 * Supported Adapters:
 * - Veepeak (OBDCheck BLE, BLE+, VP11, VP30, Dual)
 * - Vgate (iCar Pro BLE4.0, iCar 2, iCar 3, vLinker MC, vLinker FD, vLinker FS, vLinker BM)
 * - OBDLink (MX+, CX, LX, EX, SX)
 * - Carista (Carista OBD, Carista EVO)
 * - UniCarScan (UCSI-2000, UCSI-2100)
 * - LELink (Bluetooth 4.0 / BLE)
 * - BAFX Products (Bluetooth & BLE)
 * - Konnwei (KW902, KW903 BLE/SPP)
 * - Viecar (2.0, 3.0, BLE)
 * - Tonwon (Pro BLE)
 * - Carly Universal Adapter
 * - Generic ELM327 V1.5 / V2.1 Bluetooth & BLE Clones
 */
class BluetoothManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val TAG = "BLUETOOTH"

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter
    }

    private val _incomingDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val incomingDataFlow: SharedFlow<String> = _incomingDataFlow.asSharedFlow()

    // Classic Bluetooth SPP
    private var sppSocket: BluetoothSocket? = null
    private var sppOutputStream: OutputStream? = null
    private var sppInputStream: InputStream? = null

    // BLE GATT
    private var bleGatt: BluetoothGatt? = null
    private var bleTxCharacteristic: BluetoothGattCharacteristic? = null
    private var bleRxCharacteristic: BluetoothGattCharacteristic? = null

    /**
     * Codec Definition for Bluetooth Low Energy OBD-II Adapters
     */
    data class BleCodecProfile(
        val name: String,
        val serviceUuid: UUID,
        val txCharacteristicUuid: UUID? = null,
        val rxCharacteristicUuid: UUID? = null
    )

    companion object {
        // Bluetooth Classic Standard RFCOMM Serial Port Profile
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // BLE Client Characteristic Configuration Descriptor (CCCD for Notifications/Indications)
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        /**
         * Comprehensive Matrix of all known OBD-II BLE GATT Codecs
         */
        val BLE_CODEC_PROFILES = listOf(
            // 1. TI CC254x / Vgate iCar Pro / Generic ELM327 (FFE0 Codec)
            BleCodecProfile(
                name = "Vgate / TI CC254x Generic (FFE0)",
                serviceUuid = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"),
                txCharacteristicUuid = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"),
                rxCharacteristicUuid = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
            ),
            // 2. Nordic UART Service - NUS (Veepeak BLE+, vLinker MC+/FD+, Carista EVO)
            BleCodecProfile(
                name = "Nordic UART Service NUS (6E40)",
                serviceUuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
                txCharacteristicUuid = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
                rxCharacteristicUuid = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
            ),
            // 3. Carista / UniCarScan UCSI-2000 / WGSoft (FFF0 Codec)
            BleCodecProfile(
                name = "Carista / UniCarScan (FFF0)",
                serviceUuid = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB"),
                txCharacteristicUuid = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB"),
                rxCharacteristicUuid = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB")
            ),
            // 4. vLinker Custom / Tonwon Pro BLE (E781 Codec)
            BleCodecProfile(
                name = "vLinker Custom / Tonwon (E781)",
                serviceUuid = UUID.fromString("E7810A71-73AE-499D-8C15-FAA9AEF0C3F2"),
                txCharacteristicUuid = UUID.fromString("BEF8D6C9-9C21-4C9E-B632-BD58C1009F9F"),
                rxCharacteristicUuid = UUID.fromString("BEF8D6C9-9C21-4C9E-B632-BD58C1009F9F")
            ),
            // 5. ISSC / Microchip Transparent UART (4953 Codec - LELink / Viecar)
            BleCodecProfile(
                name = "ISSC / Microchip Transparent UART (4953)",
                serviceUuid = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455"),
                txCharacteristicUuid = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3"),
                rxCharacteristicUuid = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")
            ),
            // 6. Veepeak iOS/Android Dual (18F0 Codec)
            BleCodecProfile(
                name = "Veepeak Dual Mode (18F0)",
                serviceUuid = UUID.fromString("000018F0-0000-1000-8000-00805F9B34FB"),
                txCharacteristicUuid = UUID.fromString("00002AF0-0000-1000-8000-00805F9B34FB"),
                rxCharacteristicUuid = UUID.fromString("00002AF1-0000-1000-8000-00805F9B34FB")
            ),
            // 7. OBDLink CX / BLE (BEFF Codec)
            BleCodecProfile(
                name = "OBDLink CX / BLE (BEFF)",
                serviceUuid = UUID.fromString("0000BEFF-0000-1000-8000-00805F9B34FB"),
                txCharacteristicUuid = UUID.fromString("0000BEFE-0000-1000-8000-00805F9B34FB"),
                rxCharacteristicUuid = UUID.fromString("0000BEFE-0000-1000-8000-00805F9B34FB")
            ),
            // 8. Carly Custom BLE (FFF5 Codec)
            BleCodecProfile(
                name = "Carly Adapter (FFF5)",
                serviceUuid = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB"),
                txCharacteristicUuid = UUID.fromString("0000FFF5-0000-1000-8000-00805F9B34FB"),
                rxCharacteristicUuid = UUID.fromString("0000FFF4-0000-1000-8000-00805F9B34FB")
            ),
            // 9. Lufi / J1939 OBD (FFF6 Codec)
            BleCodecProfile(
                name = "Lufi / J1939 (FFF6)",
                serviceUuid = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB"),
                txCharacteristicUuid = UUID.fromString("0000FFF6-0000-1000-8000-00805F9B34FB"),
                rxCharacteristicUuid = UUID.fromString("0000FFF6-0000-1000-8000-00805F9B34FB")
            )
        )
    }

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Bluetooth Device"
            val isObd = isUniversalObdDeviceName(name)

            val discovered = DiscoveredBluetoothDevice(
                name = name,
                address = device.address,
                isBle = true,
                isVeepeak = isObd,
                rssi = result.rssi
            )

            updateAndSortDevices(discovered)
        }

        override fun onScanFailed(errorCode: Int) {
            AppLogger.e(TAG, "BLE Scan failed with errorCode: $errorCode")
        }
    }

    @Synchronized
    private fun updateAndSortDevices(newDevice: DiscoveredBluetoothDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.address == newDevice.address }

        if (index >= 0) {
            current[index] = current[index].copy(
                rssi = newDevice.rssi,
                name = if (newDevice.name != "Unknown Bluetooth Device") newDevice.name else current[index].name,
                isVeepeak = newDevice.isVeepeak || current[index].isVeepeak
            )
        } else {
            AppLogger.d(TAG, "Discovered device: '${newDevice.name}' (${newDevice.address}) RSSI: ${newDevice.rssi} dBm (OBD: ${newDevice.isVeepeak})")
            current.add(newDevice)
        }

        // Sort: OBD-recognized adapters first, then sort by highest RSSI signal strength
        _discoveredDevices.value = current.sortedWith(
            compareByDescending<DiscoveredBluetoothDevice> { it.isVeepeak }
                .thenByDescending { it.rssi }
        )
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning
        AppLogger.i(TAG, "Starting Universal Bluetooth & BLE Scan...")

        val adapter = bluetoothAdapter
        if (adapter == null) {
            AppLogger.e(TAG, "BluetoothAdapter is NULL or disabled on this device!")
            _connectionState.value = ConnectionState.Error("Bluetooth not available")
            return
        }

        if (!adapter.isEnabled) {
            AppLogger.w(TAG, "Bluetooth is turned OFF. Please enable Bluetooth.")
            _connectionState.value = ConnectionState.Error("Bluetooth is turned off")
            return
        }

        // 1. Immediately load already paired/bonded devices (Bluetooth Classic SPP & BLE)
        try {
            val bonded = adapter.bondedDevices
            AppLogger.i(TAG, "Checking bonded/paired devices (Found: ${bonded?.size ?: 0})")
            bonded?.forEach { b ->
                val name = b.name ?: "Paired OBD Device"
                val isObd = isUniversalObdDeviceName(name)
                AppLogger.d(TAG, "Found paired device: '$name' (${b.address})")
                updateAndSortDevices(
                    DiscoveredBluetoothDevice(
                        name = name,
                        address = b.address,
                        isBle = false,
                        isVeepeak = isObd,
                        rssi = -45 // High signal priority for paired device
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying bonded devices", e)
        }

        // 2. Start BLE Scanner with low latency and aggressive window
        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            adapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
            AppLogger.d(TAG, "BLE Low Latency Scanner active")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start BLE scanner", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        AppLogger.d(TAG, "Stopping Bluetooth scan")
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping scan", e)
        }
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Universal matching across all OBD-II dongle brands and nomenclatures
     */
    private fun isUniversalObdDeviceName(name: String): Boolean {
        val upper = name.uppercase()
        return upper.contains("OBD") ||
               upper.contains("ELM") ||
               upper.contains("VEEPEAK") ||
               upper.contains("VLINKER") ||
               upper.contains("CARISTA") ||
               upper.contains("UNICARSCAN") ||
               upper.contains("VIECAR") ||
               upper.contains("VGATE") ||
               upper.contains("ICAR") ||
               upper.contains("KONNWEI") ||
               upper.contains("TONWON") ||
               upper.contains("BAFX") ||
               upper.contains("LELINK") ||
               upper.contains("OBDLINK") ||
               upper.contains("SCANTOOL") ||
               upper.contains("CARLY") ||
               upper.contains("KW90") ||
               upper.contains("VP11") ||
               upper.contains("VP30") ||
               upper.contains("IOS-VLINK") ||
               upper.contains("ANDROID-VLINK") ||
               upper.contains("KIWI")
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceInfo: DiscoveredBluetoothDevice) {
        stopScan()
        AppLogger.i(TAG, "Connecting to '${deviceInfo.name}' (${deviceInfo.address})...")
        _connectionState.value = ConnectionState.Connecting(deviceInfo.name)

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is disabled")
            return
        }

        val device = try {
            adapter.getRemoteDevice(deviceInfo.address)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Invalid Bluetooth Address: ${deviceInfo.address}", e)
            _connectionState.value = ConnectionState.Error("Invalid device address")
            return
        }

        scope.launch {
            try {
                if (deviceInfo.isBle) {
                    AppLogger.d(TAG, "Mode: BLE Connection first...")
                    val bleSuccess = connectBle(device, deviceInfo.name)
                    if (!bleSuccess) {
                        AppLogger.w(TAG, "BLE GATT connection failed. Triggering Classic SPP fallback...")
                        _connectionState.value = ConnectionState.Handshaking(deviceInfo.name, "Retrying via Bluetooth Classic SPP...")
                        val sppSuccess = connectClassicSpp(device, deviceInfo.name)
                        if (!sppSuccess) {
                            _connectionState.value = ConnectionState.Error("Connection failed via BLE & Classic SPP")
                        }
                    }
                } else {
                    AppLogger.d(TAG, "Mode: Classic SPP connection first...")
                    val sppSuccess = connectClassicSpp(device, deviceInfo.name)
                    if (!sppSuccess) {
                        AppLogger.w(TAG, "Classic SPP connection failed. Triggering BLE fallback...")
                        _connectionState.value = ConnectionState.Handshaking(deviceInfo.name, "Retrying via Bluetooth BLE...")
                        val bleSuccess = connectBle(device, deviceInfo.name)
                        if (!bleSuccess) {
                            _connectionState.value = ConnectionState.Error("Connection failed via SPP & BLE")
                        }
                    }
                }
            } catch (e: Throwable) {
                AppLogger.e(TAG, "Uncaught error during connection: ${e.message}", e)
                _connectionState.value = ConnectionState.Error("Connect error: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectClassicSpp(device: BluetoothDevice, name: String?): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        AppLogger.i(TAG, "Connecting to '${device.address}' via Classic RFCOMM SPP...")

        var socket: BluetoothSocket? = try {
            AppLogger.d(TAG, "SPP Try 1: Insecure RFCOMM socket (UUID: $SPP_UUID)")
            device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Insecure RFCOMM creation failed: ${e.message}")
            null
        }

        if (socket == null) {
            socket = try {
                AppLogger.d(TAG, "SPP Try 2: Secure RFCOMM socket")
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Secure RFCOMM creation failed: ${e.message}")
                null
            }
        }

        if (socket == null) {
            socket = try {
                AppLogger.d(TAG, "SPP Try 3: Reflection createRfcommSocket(1)")
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                method.invoke(device, 1) as? BluetoothSocket
            } catch (e: Exception) {
                AppLogger.w(TAG, "Reflection socket creation failed: ${e.message}")
                null
            }
        }

        if (socket == null) {
            AppLogger.e(TAG, "Failed to create any RFCOMM socket")
            return@withContext false
        }

        return@withContext try {
            socket.connect()
            sppSocket = socket
            sppOutputStream = socket.outputStream
            sppInputStream = socket.inputStream

            AppLogger.i(TAG, "✓ Classic SPP Socket CONNECTED successfully!")
            _connectionState.value = ConnectionState.Connected(name ?: "OBD Adapter", "Bluetooth SPP (Classic)")
            startSppReadLoop()
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Socket connect() threw exception: ${e.message}")
            try { socket.close() } catch (_: Exception) {}
            false
        }
    }

    private fun startSppReadLoop() {
        scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            val lineBuilder = StringBuilder()

            try {
                val input = sppInputStream ?: return@launch
                AppLogger.d(TAG, "SPP Stream Reader loop started")
                while (sppSocket?.isConnected == true && _connectionState.value is ConnectionState.Connected) {
                    val count = input.read(buffer)
                    if (count > 0) {
                        val chunk = String(buffer, 0, count, Charsets.US_ASCII)
                        for (c in chunk) {
                            if (c == '\r' || c == '\n' || c == '>') {
                                val line = lineBuilder.toString().trim()
                                if (line.isNotEmpty()) {
                                    AppLogger.d("SPP-RX", "< $line")
                                    _incomingDataFlow.emit(line)
                                }
                                lineBuilder.clear()
                            } else {
                                lineBuilder.append(c)
                            }
                        }
                    } else if (count == -1) {
                        AppLogger.w(TAG, "SPP End of Stream (-1)")
                        break
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "SPP Read Loop error: ${e.message}")
                if (_connectionState.value is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectBle(device: BluetoothDevice, name: String?): Boolean = withContext(Dispatchers.Main) {
        disconnect()
        AppLogger.i(TAG, "Connecting to '${device.address}' via BLE GATT...")

        val deferred = CompletableDeferred<Boolean>()

        try {
            bleGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    AppLogger.d(TAG, "BLE onConnectionStateChange: status=$status, newState=$newState")
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        AppLogger.i(TAG, "BLE GATT Connected! Discovering services...")
                        _connectionState.value = ConnectionState.Handshaking(name ?: "OBD BLE Adapter", "Negotiating High Speed Link...")
                        try {
                            gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                            gatt?.discoverServices()
                        } catch (e: Exception) {
                            AppLogger.e(TAG, "discoverServices error: ${e.message}")
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        AppLogger.w(TAG, "BLE GATT Disconnected (status: $status)")
                        deferred.complete(false)
                        _connectionState.value = ConnectionState.Disconnected
                        try { gatt?.close() } catch (_: Exception) {}
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    AppLogger.d(TAG, "BLE onServicesDiscovered: status=$status, total services=${gatt?.services?.size ?: 0}")
                    if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                        val success = configureBleGattServices(gatt, name)
                        deferred.complete(success)
                    } else {
                        AppLogger.e(TAG, "BLE Service Discovery failed with status $status")
                        deferred.complete(false)
                    }
                }

                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    @Suppress("DEPRECATION")
                    val value = characteristic.value ?: return
                    handleBleBytes(value)
                }

                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                    handleBleBytes(value)
                }
            })
        } catch (e: Exception) {
            AppLogger.e(TAG, "connectGatt error: ${e.message}", e)
            return@withContext false
        }

        val result = withTimeoutOrNull(10000) {
            deferred.await()
        } ?: false

        return@withContext result
    }

    private val bleLineBuilder = StringBuilder()

    private fun handleBleBytes(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val chunk = String(bytes, Charsets.US_ASCII)
        scope.launch {
            for (c in chunk) {
                if (c == '\r' || c == '\n' || c == '>') {
                    val line = bleLineBuilder.toString().trim()
                    if (line.isNotEmpty()) {
                        AppLogger.d("BLE-RX", "< $line")
                        _incomingDataFlow.emit(line)
                    }
                    bleLineBuilder.clear()
                } else {
                    bleLineBuilder.append(c)
                }
            }
        }
    }

    /**
     * Matches across all 10 known BLE Codecs, with automatic fallback discovery
     */
    @SuppressLint("MissingPermission")
    private fun configureBleGattServices(gatt: BluetoothGatt, name: String?): Boolean {
        // 1. Check Known Codec Profiles
        for (profile in BLE_CODEC_PROFILES) {
            val service = gatt.getService(profile.serviceUuid) ?: continue
            AppLogger.i(TAG, "Matched BLE Codec Profile: ${profile.name}")

            var txChar: BluetoothGattCharacteristic? = null
            var rxChar: BluetoothGattCharacteristic? = null

            if (profile.txCharacteristicUuid != null && profile.rxCharacteristicUuid != null) {
                txChar = service.getCharacteristic(profile.txCharacteristicUuid)
                rxChar = service.getCharacteristic(profile.rxCharacteristicUuid)
            }

            if (txChar == null || rxChar == null) {
                // Fallback inside matched service: search characteristics for write/notify
                for (char in service.characteristics) {
                    val hasNotify = (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                                    (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    val hasWrite = (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                   (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

                    if (hasWrite && txChar == null) txChar = char
                    if (hasNotify && rxChar == null) rxChar = char
                }
            }

            if (txChar != null && rxChar != null) {
                bleTxCharacteristic = txChar
                bleRxCharacteristic = rxChar
                enableBleNotification(gatt, rxChar)
                AppLogger.i(TAG, "✓ Configured Codec [${profile.name}]: TX=${txChar.uuid}, RX=${rxChar.uuid}")
                _connectionState.value = ConnectionState.Connected(name ?: "OBD BLE Adapter", "BLE (${profile.name})")
                return true
            }
        }

        // 2. Universal Auto-Discovery fallback across all GATT services
        AppLogger.d(TAG, "Scanning all ${gatt.services.size} GATT services for Universal Auto-Discovery...")
        for (service in gatt.services) {
            var foundTx: BluetoothGattCharacteristic? = null
            var foundRx: BluetoothGattCharacteristic? = null

            for (char in service.characteristics) {
                val hasNotify = (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                                (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                val hasWrite = (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                               (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

                if (hasWrite && foundTx == null) foundTx = char
                if (hasNotify && foundRx == null) foundRx = char
            }

            if (foundTx != null && foundRx != null) {
                bleTxCharacteristic = foundTx
                bleRxCharacteristic = foundRx
                enableBleNotification(gatt, foundRx)
                AppLogger.i(TAG, "✓ Auto-Detected Generic Codec: Service=${service.uuid}, TX=${foundTx.uuid}, RX=${foundRx.uuid}")
                _connectionState.value = ConnectionState.Connected(name ?: "OBD BLE Adapter", "BLE (Auto-Detected Codec)")
                return true
            }
        }

        AppLogger.e(TAG, "No compatible Notify/Write characteristic found in GATT table")
        return false
    }

    @SuppressLint("MissingPermission")
    private fun enableBleNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                val isIndicate = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                val value = if (isIndicate) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                AppLogger.d(TAG, "Writing CCCD Notification descriptor for ${characteristic.uuid}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, value)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = value
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "enableBleNotification error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String): Boolean {
        AppLogger.d("TX", "> $command")
        val payload = (command + "\r").toByteArray(Charsets.US_ASCII)

        sppOutputStream?.let { stream ->
            return try {
                stream.write(payload)
                stream.flush()
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "SPP Write error: ${e.message}")
                false
            }
        }

        bleTxCharacteristic?.let { char ->
            bleGatt?.let { gatt ->
                return try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val writeType = if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        } else {
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        }
                        gatt.writeCharacteristic(char, payload, writeType) == BluetoothGatt.GATT_SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        char.value = payload
                        if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        }
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(char)
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "BLE Write error: ${e.message}")
                    false
                }
            }
        }

        AppLogger.w(TAG, "Cannot send '$command' - no active output stream")
        return false
    }

    fun disconnect() {
        try {
            AppLogger.d(TAG, "Closing previous connections...")
            sppOutputStream?.close()
            sppInputStream?.close()
            sppSocket?.close()
            bleGatt?.disconnect()
            bleGatt?.close()

            sppOutputStream = null
            sppInputStream = null
            sppSocket = null
            bleGatt = null
            bleTxCharacteristic = null
            bleRxCharacteristic = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
