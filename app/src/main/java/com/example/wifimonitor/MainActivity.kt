package com.example.wifimonitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.InetAddress
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var statusText: TextView
    private lateinit var detailsText: TextView
    private lateinit var pingText: TextView
    private val executor = Executors.newSingleThreadExecutor()

    private val permissionRequest = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "📶 WiFi Monitor"
            textSize = 28f
            setPadding(0, 0, 0, 24)
        }

        statusText = TextView(this).apply {
            textSize = 20f
            setPadding(0, 8, 0, 16)
        }

        detailsText = TextView(this).apply {
            textSize = 16f
            setPadding(0, 8, 0, 16)
        }

        pingText = TextView(this).apply {
            textSize = 18f
            setPadding(0, 8, 0, 24)
        }

        val refresh = Button(this).apply {
            text = "Refresh"
            setOnClickListener { refreshInfo() }
        }

        root.addView(title)
        root.addView(statusText)
        root.addView(detailsText)
        root.addView(pingText)
        root.addView(refresh)

        setContentView(root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                permissionRequest
            )
        } else {
            refreshInfo()
        }
    }

    private fun refreshInfo() {
        val info: WifiInfo = wifiManager.connectionInfo

        val ssid = info.ssid?.removeSurrounding("\"") ?: "Unknown"
        val rssi = info.rssi
        val level = WifiManager.calculateSignalLevel(rssi, 5)
        val linkSpeed = info.linkSpeed
        val frequency = info.frequency

        val signal = when {
            rssi >= -50 -> "Sangat kuat"
            rssi >= -60 -> "Kuat"
            rssi >= -70 -> "Sedang"
            rssi >= -80 -> "Lemah"
            else -> "Sangat lemah"
        }

        statusText.text = "Wi-Fi: $ssid\nSinyal: $signal ($rssi dBm)\nLevel: $level/4"

        val ip = Formatter.formatIpAddress(info.ipAddress)
        val gateway = try {
            val dhcp = wifiManager.dhcpInfo
            Formatter.formatIpAddress(dhcp.gateway)
        } catch (_: Exception) {
            "Tidak tersedia"
        }

        detailsText.text = """
            IP Address : $ip
            Gateway    : $gateway
            Link Speed : $linkSpeed Mbps
            Frequency  : $frequency MHz
        """.trimIndent()

        pingGateway(gateway)
    }

    private fun pingGateway(gateway: String) {
        if (gateway == "Tidak tersedia") {
            pingText.text = "Ping: tidak tersedia"
            return
        }

        pingText.text = "Ping gateway: mengukur..."

        executor.execute {
            val result = try {
                val address = InetAddress.getByName(gateway)
                val start = System.currentTimeMillis()
                val reachable = address.isReachable(1500)
                val elapsed = System.currentTimeMillis() - start
                if (reachable) "$elapsed ms" else "Timeout"
            } catch (e: Exception) {
                "Error"
            }

            runOnUiThread {
                pingText.text = "Ping gateway: $result"
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
