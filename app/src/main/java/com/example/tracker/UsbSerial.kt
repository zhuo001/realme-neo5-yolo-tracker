package com.example.tracker

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsbSerial(private val context: Context) {
    private var serialPort: UsbSerialPort? = null
    private var connection: UsbDeviceConnection? = null
    
    companion object {
        private const val TAG = "UsbSerial"
        private const val BAUD_RATE = 115200
        private const val DATA_BITS = 8
        private const val STOP_BITS = UsbSerialPort.STOPBITS_1
        private const val PARITY = UsbSerialPort.PARITY_NONE
    }

    /**
     * 查找并连接 USB 串口设备
     * @return 是否成功连接
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            
            if (availableDrivers.isEmpty()) {
                Log.w(TAG, "No USB devices found")
                return@withContext false
            }

            // 选择第一个可用设备
            val driver = availableDrivers[0]
            val device = driver.device
            
            Log.i(TAG, "Found USB device: ${device.deviceName}")
            
            val connection = manager.openDevice(device)
            if (connection == null) {
                Log.e(TAG, "Failed to open device connection")
                return@withContext false
            }

            val port = driver.ports[0]
            port.open(connection)
            port.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY)
            
            this@UsbSerial.connection = connection
            this@UsbSerial.serialPort = port
            
            Log.i(TAG, "USB serial connected successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect USB serial", e)
            false
        }
    }

    /**
     * 发送数据到 STM32
     * @param data 要发送的字节数组
     * @return 发送的字节数，-1 表示失败
     */
    suspend fun sendData(data: ByteArray): Int = withContext(Dispatchers.IO) {
        val port = serialPort
        if (port == null) {
            Log.e(TAG, "Serial port not connected")
            return@withContext -1
        }

        try {
            val sent = port.write(data, 1000) as? Int ?: -1 // 1秒超时
            Log.d(TAG, "Sent ${sent}/${data.size} bytes")
            sent
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data", e)
            -1
        }
    }

    /**
     * 断开连接
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            serialPort?.close()
            connection?.close()
            serialPort = null
            connection = null
            Log.i(TAG, "USB serial disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting USB serial", e)
        }
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean = serialPort != null

    /**
     * 获取连接的设备信息
     */
    fun getDeviceInfo(): String? {
        val port = serialPort ?: return null
        val device = port.driver.device
        return "Device: ${device.deviceName}, Product: ${device.productName}, Vendor: ${device.vendorId}"
    }
}
