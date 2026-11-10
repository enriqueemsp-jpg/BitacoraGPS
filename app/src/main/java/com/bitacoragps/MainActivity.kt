
package com.bitacoragps

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var estado: TextView
    private lateinit var distancia: TextView
    private var iniciado = false

    private val azul = Color.rgb(13, 71, 161)
    private val fondo = Color.rgb(244, 246, 250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val principal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fondo)
        }

        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(30), dp(24), dp(30))
            setBackgroundColor(azul)
        }

        encabezado.addView(texto("BITÁCORA GPS", 28f, Color.WHITE, true))
        encabezado.addView(texto("CONTROL VEHICULAR", 14f, Color.WHITE, false))

        principal.addView(encabezado)

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(24))
        }

        val tarjetaEstado = tarjeta()
        tarjetaEstado.addView(texto("ESTADO DEL VEHÍCULO", 13f, Color.GRAY, true))

        estado = texto("● Vehículo detenido", 20f, Color.rgb(198, 40, 40), true)
        tarjetaEstado.addView(estado)

        contenido.addView(tarjetaEstado)

        val tarjetaDistancia = tarjeta()
        tarjetaDistancia.addView(texto("DISTANCIA DEL RECORRIDO", 13f, Color.GRAY, true))

        distancia = texto("0.00 km", 36f, azul, true)
        tarjetaDistancia.addView(distancia)

        contenido.addView(tarjetaDistancia)

        val iniciar = boton("INICIAR RECORRIDO", azul)
        iniciar.setOnClickListener {
            iniciado = true
            estado.text = "● Recorrido iniciado"
            estado.setTextColor(Color.rgb(46, 125, 50))
        }

        val finalizar = boton("FINALIZAR RECORRIDO", Color.rgb(198, 40, 40))
        finalizar.setOnClickListener {
            iniciado = false
            estado.text = "● Vehículo detenido"
            estado.setTextColor(Color.rgb(198, 40, 40))
        }

        contenido.addView(iniciar)
        contenido.addView(finalizar)

        val ubicacion = boton("MI UBICACIÓN", Color.rgb(55, 71, 79))
        ubicacion.setOnClickListener {
            estado.text = "Ubicación GPS pendiente de activar"
            estado.setTextColor(Color.DKGRAY)
        }
        contenido.addView(ubicacion)

        val historial = tarjeta()
        historial.addView(texto("HISTORIAL DE RECORRIDOS", 16f, azul, true))
        historial.addView(texto("Aquí aparecerán tus recorridos guardados.", 14f, Color.DKGRAY, false))
        contenido.addView(historial)

        contenido.addView(texto(
            "BitácoraGPS v1.0",
            12f,
            Color.GRAY,
            false
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        })

        val scroll = ScrollView(this)
        scroll.addView(contenido)
        principal.addView(scroll)

        setContentView(principal)
    }

    private fun texto(
        contenido: String,
        tamano: Float,
        color: Int,
        negrita: Boolean
    ): TextView {
        return TextView(this).apply {
            text = contenido
            textSize = tamano
            setTextColor(color)
            if (negrita) setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(6))
        }
    }

    private fun tarjeta(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
            }
            elevation = dp(3).toFloat()
            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            parametros.bottomMargin = dp(16)
            layoutParams = parametros
        }
    }

    private fun boton(titulo: String, color: Int): Button {
        return Button(this).apply {
            text = titulo
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            )
            parametros.bottomMargin = dp(12)
            layoutParams = parametros
        }
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}

