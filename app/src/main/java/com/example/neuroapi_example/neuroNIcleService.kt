package com.jam.dentsu.neuroapi_example

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.experimental.and

class neuroNicleService private constructor(context: Context) {

    var isConnected = false
    var isFitting = false
    var batteryAlert = false
    var isDestroyed = false

    companion object {

        private var _instance: neuroNicleService? = null

        private var listener: NNListener? = null
        interface NNListener {
            fun onDataReceived(ch1: Int, ch2: Int)
        }
        fun setListener(listener: NNListener?) {
            this.listener = listener
        }

        private val REQUEST_ENABLE_BT = 3

        internal var prev1 = 0
        internal var _prev1 = 0
        internal var _curt1 = 0

        internal var prev2 = 0
        internal var _prev2 = 0
        internal var _curt2 = 0

        internal var mBluetoothAdapter: BluetoothAdapter? = null

        internal var MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        internal var btDevice: BluetoothDevice? = null
        internal var btSocket: BluetoothSocket? = null



        private class BTFoundDevice(val deviceMac: String, val deviceName: String)

        var times = mutableListOf<Date>()
        val format = SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS", Locale.getDefault())

        var ch1 = 0
        var ch2 = 0

        var pcd: Byte = 0
        var packetCount = 0

        private var raw_count = 0

        fun onCreateApplication(applicationContext: Context) {

            _instance = neuroNicleService(applicationContext)
        }

        val instance: neuroNicleService
            get() {
                _instance?.let {
                    return it
                } ?: run {
                    throw RuntimeException("nnService should be initialized.")
                }
            }

//        fun setContext(con: Context) {
//            context=con
//        }

        fun StartNN() {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter == null) {
                // Bluetoothをサポートしていないデバイス
                //Toast.makeText(this, "Bluetoothを使用することができません", Toast.LENGTH_LONG).show()
                return
            }

            Timer().schedule(500, 1000) {
                if (!neuroNicleService.instance.isConnected) {
                    Log.d("connect","now connecting...")
                    connectDevice()
                }
            }
        }

        fun connectDevice() {
            val pairedDevices = mBluetoothAdapter!!.bondedDevices
            if (pairedDevices.size > 0) { // 複数検出された場合

                for (device in pairedDevices) {
                    val deviceClass = BTFoundDevice(device.address, device.name)

                    if (deviceClass.deviceName == "neuroNicle E2") {
                        btDevice = mBluetoothAdapter!!.getRemoteDevice(deviceClass.deviceMac)
                        btSocket = null
                        try {
                            btSocket = btDevice?.createRfcommSocketToServiceRecord(MY_UUID)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        try {
                            btSocket!!.connect()
                        } catch (connectException: IOException) {
                            connectException.printStackTrace()
                            try {
                                btSocket!!.close()
                            } catch (closeException: IOException) {
                            }

                            return
                        }

                        val connectedThread = ConnectedThread(btSocket!!)
                        connectedThread.start()
                    }
                }
            }
        }

        private fun BitJudge(data: Int, flag_place: Int): Boolean {
            try {
                val mask = 1 shl flag_place
                //            Log.d("bit",data.toString())
                //            Log.d("bit",mask.toString())
                val judge = data and mask != 0
                return judge
            } catch (e: IOException) {
                return false
            }
        }

        private class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
            private val mmInStream: InputStream?

            init {
                var tmpIn: InputStream? = null

                // BluetoothSocketの inputstream と outputstreamを得る
                try {
                    tmpIn = mmSocket.inputStream
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                Log.d("connect","connected!")
                neuroNicleService.instance.isConnected = true

                mmInStream = tmpIn
            }

            override fun run() {
                var packet = mutableListOf<String>()
                var firstFlag = false

                while (true) {
                    try {
                        val data = Integer.toHexString(mmInStream!!.read())
                        if (data == "ff") {
                            firstFlag = true
                        } else if (data == "fe"){
                            if (firstFlag) {
                                if (packet.count() == 20) {

                                    try {
                                        val bit = Integer.parseInt(packet[3],16)
                                        neuroNicleService.instance.isFitting = bit == 56

                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }

                                    //i==4
                                    packetCount = Integer.parseInt(packet[4], 16)

                                    //i==6
                                    pcd = Integer.parseInt(packet[6], 16).toByte()

                                    if (packetCount == 0) {
                                        neuroNicleService.instance.batteryAlert =
                                            BitJudge(Integer.parseInt(packet[6], 16), 2)
                                    }

                                    //i==7 || i==8
                                    _prev1 = Integer.parseInt(packet[7], 16)
                                    _prev1 = _prev1 * 256

                                    _curt1 = Integer.parseInt(packet[8], 16)
                                    ch1 = _curt1 + _prev1

                                    //i==9 || i==10
                                    _prev2 = Integer.parseInt(packet[9], 16)
                                    _prev2 = _prev2 * 256
                                    _curt2 = Integer.parseInt(packet[10], 16)

                                    ch2 = _curt2 + _prev2
                                    if (listener != null) {
                                        // 値を通知
                                        listener!!.onDataReceived(ch1, ch2)
                                    }
                                }
                                else {
                                    Log.d("buf", "packet count is not 20")
                                }

                                packet.clear()
                                packet.add("ff")
                            }
                            firstFlag = false
                        }
                        else {
                            firstFlag = false
                        }
                        packet.add(data)
                    } catch (e: IOException) {
                        println("切断")
                        neuroNicleService.instance.isConnected = false
                        break
                    }
                }
            }
        }

        private fun getByteBinaryStr( byte: Byte ): String {

            var result = ""

            var _byte = byte

            var counter = java.lang.Byte.SIZE
            val mask: Byte = (0b10000000).toByte()

            while ( counter > 0 ) {

                val c = if ( _byte.and(mask) == mask ) '1' else '0'
                result += c

                _byte = _byte.toInt().shl(1).toByte()
                counter -= 1
            }

            return result
        }

        private fun getByteArrayFromInt(number: Int): Array<Byte> {

            val result = Array<Byte>(java.lang.Integer.BYTES, {0})
            var _number = number
            var mask = 0xFF // binary 1111 1111

            for (i in 0 until result.size) {

                result[i] = _number.and(mask).toByte()
                _number = _number.shr(8)
            }

            result.reverse()

            return result
        }
    }
}