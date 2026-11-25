
package com.bitacoragps

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
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
import android.widget.ImageView
import android.widget.Toast

import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
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
    private val SOLICITUD_CAMARA_COMBUSTIBLE = 201
    private val SOLICITUD_GALERIA_COMBUSTIBLE = 202

    private var fotoPendienteUri: Uri? = null
    private var cargaPendienteId: String? = null
    private var tipoFotoPendiente: String? = null

    // Las cinco evidencias obligatorias de cada carga
    private val tiposFotos = linkedMapOf(
        "ticketcar" to "1. Ticket CAR",
        "ticket_bomba" to "2. Ticket de bomba",
        "kilometraje" to "3. Fotografía del kilometraje",
        "antes_cargar" to "4. Fotografía antes de cargar",
        "respaldo_ticket" to "5. Respaldo del ticket (por si no es legible)"
    )

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

