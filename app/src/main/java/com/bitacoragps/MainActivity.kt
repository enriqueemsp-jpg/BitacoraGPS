
package com.bitacoragps

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.json.JSONArray
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var estado: TextView
    private lateinit var distancia: TextView
    private lateinit var ubicacion: TextView
    private lateinit var historialContainer: LinearLayout
    private lateinit var kilometrajeActual: TextView

    private val azul = Color.rgb(13, 71, 161)
    private val handler = Handler(Looper.getMainLooper())
    private var ultimoHistorial = ""

    private val actualizar = object : Runnable {
        override fun run() {
            actualizarDatos()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val principal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(Color.rgb(245, 247, 250))
        }

        val titulo = crearTexto("BITÁCORA GPS", 28f, azul, true)
        titulo.gravity = Gravity.CENTER
        principal.addView(titulo)

        val subtitulo = crearTexto("CONTROL VEHICULAR", 14f, Color.GRAY, true)
        subtitulo.gravity = Gravity.CENTER
        principal.addView(subtitulo)

        estado = crearTexto("Vehículo detenido", 19f, Color.RED, true)
        estado.setPadding(0, 35, 0, 20)
        principal.addView(estado)

        val etiqueta = crearTexto(
            "DISTANCIA RECORRIDA", 14f, Color.GRAY, true
        )
        principal.addView(etiqueta)

        distancia = crearTexto("0.00 km", 38f, azul, true)
        principal.addView(distancia)

        // MÓDULO DE KILOMETRAJE DE LA UNIDAD

        val tituloKm = crearTexto(
            "KILOMETRAJE DE LA UNIDAD",
            18f,
            azul,
            true
        )
        tituloKm.setPadding(0, 30, 0, 8)
        principal.addView(tituloKm)

        kilometrajeActual = crearTexto(
            "Configura el kilometraje inicial",
            26f,
            Color.rgb(46, 125, 50),
            true
        )
        principal.addView(kilometrajeActual)

        val configurarKm = crearBoton(
            "CONFIGURAR KILOMETRAJE INICIAL",
            Color.rgb(46, 125, 50)
        )
        configurarKm.setOnClickListener {
            mostrarConfiguracionKilometraje()
        }
        principal.addView(configurarKm)

        // BOTONES DE RECORRIDO

        val iniciar = crearBoton("INICIAR RECORRIDO", azul)
        iniciar.setOnClickListener {
            solicitarPermisos()
        }
        principal.addView(iniciar)

        val finalizar = crearBoton(
            "FINALIZAR RECORRIDO",
            Color.rgb(198, 40, 40)
        )
        finalizar.setOnClickListener {
            val intent = Intent(this, GpsTrackingService::class.java)
            intent.action = GpsTrackingService.ACTION_STOP
            startService(intent)

            estado.text = "Vehículo detenido"
            estado.setTextColor(Color.RED)
        }
        principal.addView(finalizar)

        ubicacion = crearTexto(
            "Ubicación pendiente", 14f, Color.DKGRAY, false
        )
        ubicacion.setPadding(0, 25, 0, 10)
        principal.addView(ubicacion)

        val tituloHistorial = crearTexto(
            "HISTORIAL DE TRASLADOS",
            20f,
            azul,
            true
        )
        tituloHistorial.setPadding(0, 35, 0, 15)
        principal.addView(tituloHistorial)

        historialContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        principal.addView(historialContainer)

        val scroll = ScrollView(this)
        scroll.addView(principal)
        setContentView(scroll)

        actualizarDatos()
        handler.post(actualizar)
    }

    private fun mostrarConfiguracionKilometraje() {

        val datos = getSharedPreferences(
            "BitacoraGPS", MODE_PRIVATE
        )

        val actual = datos.getString("kilometraje_base", "")

        val entrada = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Ejemplo: 125450"
            setSingleLine(true)
            setText(actual)
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 10, 50, 0)
            addView(entrada)
        }

        AlertDialog.Builder(this)
            .setTitle("Kilometraje inicial de la unidad")
            .setMessage(
                "Ingresa el kilometraje que marca actualmente " +
                        "el tablero del vehículo."
            )
            .setView(contenedor)
            .setPositiveButton("GUARDAR", null)
            .setNegativeButton("CANCELAR", null)
            .create()
            .also { dialog ->

                dialog.setOnShowListener {
                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val valor = entrada.text.toString()
                            .trim()
                            .replace(",", ".")
                            .toDoubleOrNull()

                        if (valor == null || !valor.isFinite() ||
                            valor < 0) {

                            entrada.error =
                                "Ingresa un kilometraje válido"
                            return@setOnClickListener
                        }

                        // Guardamos el kilometraje inicial.
                        // Los recorridos anteriores quedan excluidos.

                        val historialTexto = datos.getString(
                            "historial_recorridos", "[]"
                        ) ?: "[]"

                        val cantidadActual = try {
                            JSONArray(historialTexto).length()
                        } catch (e: Exception) {
                            0
                        }

                        datos.edit()
                            .putString(
                                "kilometraje_base",
                                valor.toString()
                            )
                            .putInt(
                                "kilometraje_base_historial_count",
                                cantidadActual
                            )
                            .putBoolean(
                                "kilometraje_configurado",
                                true
                            )
                            .apply()

                        actualizarKilometraje()

                        Toast.makeText(
                            this,
                            "Kilometraje inicial guardado",
                            Toast.LENGTH_LONG
                        ).show()

                        dialog.dismiss()
                    }
                }

                dialog.show()
            }
    }

    private fun actualizarKilometraje() {

        val datos = getSharedPreferences(
            "BitacoraGPS", MODE_PRIVATE
        )

        if (!datos.getBoolean(
                "kilometraje_configurado", false
            )) {
            kilometrajeActual.text =
                "Configura el kilometraje inicial"
            return
        }

        val base = datos.getString(
            "kilometraje_base", "0"
        )?.toDoubleOrNull() ?: 0.0

        val cantidadInicial = datos.getInt(
            "kilometraje_base_historial_count", 0
        )

        val historialTexto = datos.getString(
            "historial_recorridos", "[]"
        ) ?: "[]"

        try {
            val historial = JSONArray(historialTexto)

            var kilometrosNuevos = 0.0

            for (i in cantidadInicial until historial.length()) {
                val recorrido = historial.getJSONObject(i)

                kilometrosNuevos += recorrido.optDouble(
                    "kilometros", 0.0
                )
            }

            val total = base + kilometrosNuevos

            kilometrajeActual.text = String.format(
                Locale.getDefault(),
                "%,.2f km",
                total
            )

        } catch (e: Exception) {
            kilometrajeActual.text =
                "Error al calcular kilometraje"
        }
    }

    private fun solicitarPermisos() {
        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permisos = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (Build.VERSION.SDK_INT >= 33) {
                permisos.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            requestPermissions(permisos.toTypedArray(), 100)
        } else {
            iniciarGPS()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 100) {
            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                iniciarGPS()
            } else {
                estado.text = "Permiso GPS necesario"
                estado.setTextColor(Color.RED)
            }
        }
    }

    private fun iniciarGPS() {
        val intent = Intent(this, GpsTrackingService::class.java)
        intent.action = GpsTrackingService.ACTION_START

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            estado.text = "Recorrido en seguimiento"
            estado.setTextColor(Color.rgb(46, 125, 50))

        } catch (e: Exception) {
            estado.text = "No se pudo iniciar el GPS"
            estado.setTextColor(Color.RED)
        }
    }

    private fun actualizarDatos() {
        val datos = getSharedPreferences(
            "BitacoraGPS", MODE_PRIVATE
        )

        val metros = datos.getFloat("distancia_metros", 0f)
        val lat = datos.getFloat("latitud", 0f)
        val lon = datos.getFloat("longitud", 0f)

        distancia.text = String.format(
            Locale.getDefault(),
            "%.2f km",
            metros / 1000f
        )

        if (lat != 0f || lon != 0f) {
            ubicacion.text = "Latitud: $lat\nLongitud: $lon"
        }

        cargarHistorial()
        actualizarKilometraje()
    }

    private fun cargarHistorial() {
        val datos = getSharedPreferences(
            "BitacoraGPS", MODE_PRIVATE
        )

        val historialTexto = datos.getString(
            "historial_recorridos", "[]"
        ) ?: "[]"

        if (historialTexto == ultimoHistorial) return

        ultimoHistorial = historialTexto
        historialContainer.removeAllViews()

        try {
            val historial = JSONArray(historialTexto)

            if (historial.length() == 0) {
                val vacio = crearTexto(
                    "Todavía no hay traslados registrados.",
                    15f,
                    Color.GRAY,
                    false
                )
                vacio.setPadding(0, 10, 0, 20)
                historialContainer.addView(vacio)
                return
            }

            for (i in historial.length() - 1 downTo 0) {
                val recorrido = historial.getJSONObject(i)

                val inicio = recorrido.optString(
                    "fechaInicio", "No disponible"
                )
                val fin = recorrido.optString(
                    "fechaFin", "No disponible"
                )
                val km = recorrido.optDouble(
                    "kilometros", 0.0
                )

                val tarjeta = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(18, 16, 18, 16)
                    setBackgroundColor(Color.WHITE)
                }

                val numero = crearTexto(
                    "TRASLADO ${i + 1}",
                    16f,
                    azul,
                    true
                )
                tarjeta.addView(numero)

                val fecha = crearTexto(
                    "Inicio: $inicio\nFinalización: $fin",
                    14f,
                    Color.DKGRAY,
                    false
                )
                fecha.setPadding(0, 10, 0, 8)
                tarjeta.addView(fecha)

                val kilometros = crearTexto(
                    String.format(
                        Locale.getDefault(),
                        "Distancia recorrida: %.2f km",
                        km
                    ),
                    18f,
                    Color.rgb(46, 125, 50),
                    true
                )
                tarjeta.addView(kilometros)

                historialContainer.addView(
                    tarjeta,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 16
                    }
                )
            }

        } catch (e: Exception) {
            historialContainer.addView(
                crearTexto(
                    "No se pudo leer el historial.",
                    14f,
                    Color.RED,
                    false
                )
            )
        }
    }

    private fun crearTexto(
        contenido: String,
        tamano: Float,
        color: Int,
        negrita: Boolean
    ): TextView {
        return TextView(this).apply {
            text = contenido
            textSize = tamano
            setTextColor(color)
            if (negrita) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun crearBoton(
        texto: String,
        color: Int
    ): Button {
        val densidad = resources.displayMetrics.density
        val alto = (56 * densidad).toInt()
        val margen = (12 * densidad).toInt()

        return Button(this).apply {
            text = texto
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)

            backgroundTintList =
                android.content.res.ColorStateList.valueOf(color)

            minHeight = alto
            minimumHeight = alto

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                alto
            ).apply {
                topMargin = margen
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(actualizar)
        super.onDestroy()
    }
}
