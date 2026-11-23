
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
import android.util.Xml
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

import java.util.Locale
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class MainActivity : Activity() {

    private lateinit var estado: TextView
    private lateinit var distancia: TextView
    private lateinit var ubicacion: TextView
    private lateinit var historialContainer: LinearLayout
    private lateinit var kilometrajeActual: TextView

    private val azul = Color.rgb(13, 71, 161)
    private val handler = Handler(Looper.getMainLooper())
    private var ultimoHistorial = ""

    private val SOLICITUD_RESPALDO = 200

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
            mostrarSelectorMotivo()
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

        // BOTÓN PARA RECUPERAR HISTORIAL

        val importarHistorial = crearBoton(
            "IMPORTAR HISTORIAL RECUPERADO",
            Color.rgb(0, 121, 107)
        )

        importarHistorial.setOnClickListener {
            seleccionarRespaldo()
        }

        principal.addView(importarHistorial)

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

    // ELEGIR MOTIVO DEL RECORRIDO

    private fun mostrarSelectorMotivo() {

        val motivos = arrayOf(
            "Trabajo",
            "Personal",
            "Visita a cliente",
            "Trámite",
            "Compras",
            "Otro"
        )

        AlertDialog.Builder(this)
            .setTitle("¿Cuál es el motivo del recorrido?")
            .setItems(motivos) { _, seleccion ->

                if (motivos[seleccion] == "Otro") {
                    mostrarOtroMotivo()
                } else {
                    guardarMotivoEIniciar(motivos[seleccion])
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    // ESCRIBIR OTRO MOTIVO

    private fun mostrarOtroMotivo() {

        val entrada = EditText(this).apply {
            hint = "Ejemplo: visita familiar"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 10, 50, 0)
            addView(entrada)
        }

        AlertDialog.Builder(this)
            .setTitle("Motivo del recorrido")
            .setView(contenedor)
            .setPositiveButton("CONTINUAR") { _, _ ->

                val motivo = entrada.text.toString().trim()

                if (motivo.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Escribe el motivo del recorrido",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                guardarMotivoEIniciar(motivo)
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    // GUARDAR MOTIVO Y SOLICITAR GPS

    private fun guardarMotivoEIniciar(motivo: String) {

        getSharedPreferences("BitacoraGPS", MODE_PRIVATE)
            .edit()
            .putString("motivo_actual", motivo)
            .apply()

        solicitarPermisos()
    }

    // CONFIGURAR KILOMETRAJE INICIAL

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

    // ACTUALIZAR KILOMETRAJE ACUMULADO

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

    // PERMISOS GPS

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

    // ACTUALIZAR INFORMACIÓN DE PANTALLA

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

    // MOSTRAR HISTORIAL

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

                val motivo = recorrido.optString(
                    "motivo", "Sin motivo registrado"
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

                val etiquetaMotivo = crearTexto(
                    motivo,
                    18f,
                    Color.rgb(0, 121, 107),
                    true
                )
                etiquetaMotivo.setPadding(0, 8, 0, 4)
                tarjeta.addView(etiquetaMotivo)

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

    // SELECCIONAR RESPALDO

    private fun seleccionarRespaldo() {

        val selector = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(selector, SOLICITUD_RESPALDO)
    }

    // RECIBIR ARCHIVO SELECCIONADO

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SOLICITUD_RESPALDO &&
            resultCode == RESULT_OK
        ) {
            val uri = data?.data ?: return

            try {
                val bytes = contentResolver.openInputStream(uri)?.use {
                    it.readBytes()
                } ?: throw Exception("No se pudo abrir el respaldo")

                val xmlBytes = extraerXmlRespaldo(bytes)
                val historialRecuperado = leerHistorialXml(xmlBytes)

                importarRecorridos(historialRecuperado)

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error al recuperar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // EXTRAER XML DEL ARCHIVO ZIP

    private fun extraerXmlRespaldo(bytes: ByteArray): ByteArray {

        if (bytes.size >= 2 &&
            bytes[0] == 'P'.code.toByte() &&
            bytes[1] == 'K'.code.toByte()
        ) {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->

                while (true) {
                    val entrada = zip.nextEntry ?: break

                    if (entrada.name.endsWith("sp/BitacoraGPS.xml") ||
                        entrada.name.endsWith("BitacoraGPS.xml")
                    ) {
                        val salida = ByteArrayOutputStream()
                        zip.copyTo(salida)
                        return salida.toByteArray()
                    }
                }
            }

            throw Exception("No se encontró BitacoraGPS.xml")
        }

        return bytes
    }

    // LEER HISTORIAL DEL XML SIN FALLAR POR CARACTERES ESPECIALES

    private fun leerHistorialXml(bytes: ByteArray): JSONArray {

        val xml = String(bytes, Charsets.UTF_8)
            .removePrefix("\uFEFF")

        val etiqueta = Regex(
            """<string\b[^>]*\bname\s*=\s*["']historial_recorridos["'][^>]*>""",
            RegexOption.IGNORE_CASE
        ).find(xml) ?: throw Exception(
            "No se encontró historial_recorridos en el XML"
        )

        val inicio = etiqueta.range.last + 1
        val final = xml.indexOf("</string>", inicio)

        if (final < 0) {
            throw Exception("El contenido del historial está incompleto")
        }

        val contenido = xml.substring(inicio, final)

        val json = decodificarEntidadesXml(contenido).trim()

        if (json.isEmpty()) {
            return JSONArray()
        }

        return try {
            JSONArray(json)
        } catch (e: Exception) {
            throw Exception(
                "Se encontró el historial, pero no se pudo interpretar: ${e.message}"
            )
        }
    }

    // DECODIFICAR ENTIDADES XML CONOCIDAS

    private fun decodificarEntidadesXml(texto: String): String {

        var resultado = Regex(
            "&(amp|lt|gt|quot|apos);",
            RegexOption.IGNORE_CASE
        ).replace(texto) { coincidencia ->

            when (coincidencia.groupValues[1].lowercase(Locale.ROOT)) {
                "amp" -> "&"
                "lt" -> "<"
                "gt" -> ">"
                "quot" -> "\""
                "apos" -> "'"
                else -> coincidencia.value
            }
        }

        resultado = Regex(
            "&#(x[0-9a-fA-F]+|[0-9]+);"
        ).replace(resultado) { coincidencia ->

            try {
                val numero = coincidencia.groupValues[1]

                val codigo = if (numero.startsWith("x")) {
                    numero.substring(1).toInt(16)
                } else {
                    numero.toInt()
                }

                String(Character.toChars(codigo))

            } catch (e: Exception) {
                coincidencia.value
            }
        }

        return resultado
    }

    // IDENTIFICAR RECORRIDOS REPETIDOS

    private fun claveRecorrido(objeto: JSONObject): String {

        return listOf(
            objeto.optString("fechaInicio"),
            objeto.optString("fechaFin"),
            String.format(
                Locale.US,
                "%.5f",
                objeto.optDouble("kilometros", 0.0)
            ),
            objeto.optString("ubicacionInicial"),
            objeto.optString("ubicacionFinal")
        ).joinToString("|")
    }

    // IMPORTAR Y COMBINAR HISTORIALES

    private fun importarRecorridos(recuperados: JSONArray) {

        val datos = getSharedPreferences(
            "BitacoraGPS", MODE_PRIVATE
        )

        val actuales = JSONArray(
            datos.getString("historial_recorridos", "[]") ?: "[]"
        )

        val combinado = JSONArray()
        val claves = mutableSetOf<String>()

        var nuevos = 0

        // Primero incorporamos los recorridos del respaldo.

        for (i in 0 until recuperados.length()) {

            val recorrido = recuperados.getJSONObject(i)
            val clave = claveRecorrido(recorrido)

            if (claves.add(clave)) {
                combinado.put(recorrido)

                var existe = false

                for (j in 0 until actuales.length()) {
                    if (claveRecorrido(
                            actuales.getJSONObject(j)
                        ) == clave
                    ) {
                        existe = true
                        break
                    }
                }

                if (!existe) nuevos++
            }
        }

        // Después conservamos los recorridos actuales.

        for (i in 0 until actuales.length()) {

            val recorrido = actuales.getJSONObject(i)
            val clave = claveRecorrido(recorrido)

            if (claves.add(clave)) {
                combinado.put(recorrido)
            }
        }

        if (nuevos == 0) {
            Toast.makeText(
                this,
                "El respaldo no contiene recorridos nuevos",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val editor = datos.edit()

        editor.putString(
            "historial_recorridos",
            combinado.toString()
        )

        // Mantener fuera del cálculo los recorridos históricos.

        if (datos.getBoolean("kilometraje_configurado", false)) {

            val base = datos.getInt(
                "kilometraje_base_historial_count", 0
            )

            editor.putInt(
                "kilometraje_base_historial_count",
                base + nuevos
            )
        }

        editor.apply()

        ultimoHistorial = ""
        cargarHistorial()
        actualizarKilometraje()

        Toast.makeText(
            this,
            "$nuevos recorridos recuperados correctamente",
            Toast.LENGTH_LONG
        ).show()
    }

    // CREAR TEXTOS

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

    // CREAR BOTONES

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
