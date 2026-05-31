package org.ecoguardian

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AlertActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        val btnAlert = findViewById<Button>(R.id.btn_send_alert)
        val btnSos = findViewById<Button>(R.id.btn_send_sos)

        btnAlert.setOnClickListener { sendAlert(1) }
        btnSos.setOnClickListener { sendAlert(2) }
    }

    private fun sendAlert(type: Int) {
        // Simulación de envío por red en malla (en producción usar Briar)
        Toast.makeText(this, "Alerta $type enviada a la red", Toast.LENGTH_SHORT).show()
    }
}
