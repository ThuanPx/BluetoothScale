package com.hyperion.blescaleexample

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.preference.Preference
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.hyperion.blescaleexample.core.OpenScale
import com.hyperion.blescaleexample.core.bluetooth.BluetoothCommunication
import com.hyperion.blescaleexample.core.bluetooth.BluetoothFactory
import com.hyperion.blescaleexample.core.datatypes.ScaleMeasurement
import com.welie.blessed.BluetoothCentral
import com.welie.blessed.BluetoothCentralCallback
import com.welie.blessed.BluetoothPeripheral
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.HashMap
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest


class MainActivity : AppCompatActivity() {

    private val foundDevices = HashMap<String, BluetoothDevice>()
    private lateinit var central: BluetoothCentral
    private lateinit var progressHandler: Handler

    private val ENABLE_BLUETOOTH_REQUEST = 102
    private val PREFERENCE_KEY_BLUETOOTH_SCANNER = "btScanner"
    val PREFERENCE_KEY_BLUETOOTH_DEVICE_NAME = "btDeviceName"
    val PREFERENCE_KEY_BLUETOOTH_HW_ADDRESS = "btHwAddress"
    private val ADDRESS_TEST = "FA:A0:F7:11:12:46"
    private val DEVICE_TEST = "MIBCS"

    private val mainAdapter: MainAdapter by lazy {
        MainAdapter {
            val key = foundDevices.keys.toTypedArray()[it]
            val address = foundDevices[key]
            Timber.e("Address %s", address)
        }
    }

    private fun formatDeviceName(name: String, address: String?): String {
        return if (name.isEmpty() || address!!.isEmpty()) {
            "-"
        } else String.format("%s [%s]", name, address)
    }

    private fun formatDeviceName(device: BluetoothDevice): String {
        return formatDeviceName(device.name, device.address)
    }

    private val bluetoothCentralCallback = object : BluetoothCentralCallback() {
        override fun onDiscoveredPeripheral(
            peripheral: BluetoothPeripheral?,
            scanResult: ScanResult?
        ) {
            scanResult?.let { onDeviceFound(it) }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvItem.layoutManager = LinearLayoutManager(this)
        rvItem.adapter = mainAdapter

        requestPermission()
        btSelectBluetooth.setOnClickListener {
            startBluetoothDiscovery()
        }

        btStop.setOnClickListener {
            stopBluetoothDiscovery()
        }

        btConnectDevice.setOnClickListener {
            invokeConnectToBluetoothDevice()
        }

        btDisconnectDevice.setOnClickListener {
            OpenScale.getInstance().disconnectFromBluetoothDevice()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun startBluetoothDiscovery() {
        showLoading(true)
        Timber.e("startBluetoothDiscovery")
        foundDevices.clear()
        mainAdapter.clearData()

        central = BluetoothCentral(
            this, bluetoothCentralCallback, Handler(
                Looper.getMainLooper()
            )
        )
        central.scanForPeripherals()

    }

    private fun stopBluetoothDiscovery() {
        showLoading(false)
        Timber.e("stopBluetoothDiscovery")
        if (::progressHandler.isInitialized)
            progressHandler.removeCallbacksAndMessages(null)
        central.stopScan()
    }

    private fun onDeviceFound(bleScanResult: ScanResult) {
        val device = bleScanResult.device

        if (device.name == null || foundDevices.containsKey(device.address)) {
            return
        }

        val prefBtDevice = Preference(this)
        prefBtDevice.title = formatDeviceName(bleScanResult.device)

        val btDevice = BluetoothFactory.createDeviceDriver(this, device.name)
        if (btDevice != null) {
            Timber.d(
                "Found supported device %s (driver: %s)",
                formatDeviceName(device), btDevice.driverName()
            )

            mainAdapter.updateData(btDevice)
            foundDevices[device.address] = device
        } else {
            Timber.d("Found unsupported device %s", formatDeviceName(device))
        }
    }

    private fun requestPermission() {
        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, 101, Manifest.permission.ACCESS_FINE_LOCATION)
                .setPositiveButtonText(android.R.string.ok)
                .setNegativeButtonText(android.R.string.cancel)
                .build()
        )
    }

    private fun invokeConnectToBluetoothDevice() {
        val openScale = OpenScale.getInstance()

        val deviceName = DEVICE_TEST
        val hwAddress = ADDRESS_TEST

        if (!BluetoothAdapter.checkBluetoothAddress(hwAddress)) {
            Timber.e("No Bluetooth device selected")
            return
        }

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        if (!bluetoothManager.adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST)
            return
        }

        Toast.makeText(
            applicationContext,
            resources.getString(R.string.info_bluetooth_try_connection) + " " + deviceName,
            Toast.LENGTH_SHORT
        ).show()

        if (!openScale.connectToBluetoothDevice(deviceName, hwAddress, callbackBtHandler)) {
            Timber.e("device not supported")
            Toast.makeText(
                applicationContext,
                deviceName + " " + resources.getString(R.string.label_bt_device_no_support),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val callbackBtHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {

            when (BluetoothCommunication.BT_STATUS.values()[msg.what]) {
                BluetoothCommunication.BT_STATUS.RETRIEVE_SCALE_DATA -> {
                    Timber.e("Bluetooth connection success")
                    val scaleBtData = msg.obj as ScaleMeasurement

                    val openScale = OpenScale.getInstance()

                    Timber.e("Test Data %s", scaleBtData.toString())
                    tvData.text = scaleBtData.weight.toString()
                    openScale.addScaleData(scaleBtData)
                }
                BluetoothCommunication.BT_STATUS.INIT_PROCESS -> {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.info_bluetooth_init),
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.d("Bluetooth initializing")
                }
                BluetoothCommunication.BT_STATUS.CONNECTION_LOST -> {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.info_bluetooth_connection_lost),
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.e("Bluetooth connection lost")
                }
                BluetoothCommunication.BT_STATUS.NO_DEVICE_FOUND -> {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.info_bluetooth_no_device),
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.e("No Bluetooth device found")
                }
                BluetoothCommunication.BT_STATUS.CONNECTION_RETRYING -> {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.info_bluetooth_no_device_retrying),
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.e("No Bluetooth device found retrying")
                }
                BluetoothCommunication.BT_STATUS.CONNECTION_ESTABLISHED -> {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.info_bluetooth_connection_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.d("Bluetooth connection successful established")
                }
                BluetoothCommunication.BT_STATUS.CONNECTION_DISCONNECT -> {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.info_bluetooth_connection_disconnected),
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.d("Bluetooth connection successful disconnected")
                }
                BluetoothCommunication.BT_STATUS.UNEXPECTED_ERROR -> {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.info_bluetooth_connection_error) + ": " + msg.obj,
                        Toast.LENGTH_SHORT
                    ).show()
                    Timber.e("Bluetooth unexpected error: %s", msg.obj)
                }
                BluetoothCommunication.BT_STATUS.SCALE_MESSAGE -> {
                    val toastMessage = String.format(resources.getString(msg.arg1), msg.obj)
                    Toast.makeText(applicationContext, toastMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}
