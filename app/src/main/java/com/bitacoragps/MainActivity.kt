
package com.bitacoragps

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val titulo = TextView(this).apply {
            text = "BITÁCORA GPS"
            textSize = 30f
            setTextColor(Color.rgb(21, 101, 192))
            gravity = Gravity.CENTER
        }

        val descripcion = TextView(this).apply {
            text = "CONTROL VEHICULAR\n\n" +
                    "Registro GPS cada 30 segundos\n\n" +
                    "Seguimiento manual y automático\n\n" +
                    "Historial de recorridos\n\n" +
                    "Exportación a Excel"
            textSize = 18f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        layout.addView(titulo)
        layout.addView(descripcion)

        setContentView(layout)
    }
}

