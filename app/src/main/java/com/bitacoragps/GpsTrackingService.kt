
package com.bitacoragps

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Localeimport android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper

class GpsTrackingService : Service(), LocationListener {

    companion object {
        const val ACTION_START = "com.bitacoragps.START"
        const val ACTION_STOP = "com.bitacoragps.STOP"

        private const val CHANNEL_ID = "bitacoragps_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val INTERVALO = 30000L
    }

    private lateinit var locationManager: LocationManager
    private var ultimaUbicacion: Location? = null
    private var distanciaMetros = 0f
    private var inicioRecorrido = 0L
    private var ubicacionInicial = ""
    
    override fun onCreate() {
        super.onCreate()
        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {
            ACTION_STOP -> {
                detenerSeguimiento()
                return START_NOT_STICKY
            }

            ACTION_START -> iniciarSeguimiento()
        }

        return START_NOT_STICKY
    }

    private fun iniciarSeguimiento() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        inicioRecorrido = System.currentTimeMillis()
distanciaMetros = 0f
ultimaUbicacion = null
ubicacionInicial = "Pendiente"

getSharedPreferences("BitacoraGPS", MODE_PRIVATE)
    .edit()
    .putFloat("distancia_metros", 0f)
    .putFloat("latitud", 0f)
    .putFloat("longitud", 0f)
    .apply()
        crearNotificacion()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notificacion(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notificacion())
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                INTERVALO,
                0f,
                this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            detenerSeguimiento()
        }
    }

    override fun onLocationChanged(location: Location) {
        ultimaUbicacion?.let { anterior ->
            if (location.accuracy <= 50f) {
                distanciaMetros += anterior.distanceTo(location)
            }
        }

        if (location.accuracy <= 50f) {
            if (ubicacionInicial == "Pendiente") {
    ubicacionInicial =
        "${location.latitude}, ${location.longitude}"
}
            ultimaUbicacion = location

            getSharedPreferences("BitacoraGPS", MODE_PRIVATE)
                .edit()
                .putFloat("distancia_metros", distanciaMetros)
                .putFloat("latitud", location.latitude.toFloat())
                .putFloat("longitud", location.longitude.toFloat())
                .apply()
        }
    }

    private fun detenerSeguimiento() {
        try {
            locationManager.removeUpdates(this)
        } catch (_: SecurityException) {
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun crearNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento GPS",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(canal)
        }
    }

    private fun notificacion(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Bitácora GPS")
            .setContentText("Registrando recorrido del vehículo")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(
        provider: String?,
        status: Int,
        extras: Bundle?
    ) {}
}

