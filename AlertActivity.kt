package org.ecoguardian

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.ecoguardian.data.model.AlertType
import org.ecoguardian.data.model.LocationData
import org.ecoguardian.databinding.ActivityAlertBinding
import org.ecoguardian.viewModel.AlertViewModel
import org.ecoguardian.viewModel.AlertResult
import org.ecoguardian.viewModel.NetworkStatus
import timber.log.Timber

@AndroidEntryPoint
class AlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding
    private val viewModel: AlertViewModel by viewModels()

    // Solicitante de permisos Bluetooth (Android 12+)
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.d("Permisos Bluetooth concedidos")
            viewModel.initialize()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        checkPermissionsAndInitialize()
    }

    private fun setupUI() {
        // Botón de amenaza
        binding.btnSendAlert.setOnClickListener {
            confirmAndSend(AlertType.THREAT)
        }

        // Botón SOS (con confirmación para evitar activación accidental)
        binding.btnSendSos.setOnClickListener {
            showSOSConfirmation()
        }

        // Botón de modo pánico (oculto por defecto, activable con triple tap)
        var tapCount = 0
        binding.appLogo.setOnClickListener {
            tapCount++
            if (tapCount >= 3) {
                viewModel.triggerPanicMode()
                Toast.makeText(this, "Modo pánico activado", Toast.LENGTH_SHORT).show()
                tapCount = 0
            }
            // Resetear contador después de 2 segundos
            binding.appLogo.postDelayed({ tapCount = 0 }, 2000)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.networkStatus.collect { status ->
                        updateNetworkStatusUI(status)
                    }
                }
                launch {
                    viewModel.peers.collect { peers ->
                        binding.txtPeerCount.text = "${peers.size} contactos activos"
                    }
                }
                launch {
                    viewModel.sendingAlert.collect { sending ->
                        binding.progressBar.visibility = if (sending) View.VISIBLE else View.GONE
                        binding.btnSendAlert.isEnabled = !sending
                        binding.btnSendSos.isEnabled = !sending
                    }
                }
                launch {
                    viewModel.lastAlertResult.collect { result ->
                        result?.let { showAlertResult(it) }
                    }
                }
            }
        }
    }

    private fun updateNetworkStatusUI(status: NetworkStatus) {
        val (text, color) = when (status) {
            NetworkStatus.INITIALIZING -> "Conectando..." to "#FF9800"
            NetworkStatus.CONNECTED -> "Red mesh: activa" to "#4CAF50"
            NetworkStatus.DISCONNECTED -> "Red mesh: buscando" to "#757575"
            NetworkStatus.ERROR -> "Error de conexión" to "#F44336"
        }
        binding.txtNetworkStatus.text = text
        binding.txtNetworkStatus.setTextColor(android.graphics.Color.parseColor(color))
    }

    private fun showAlertResult(result: AlertResult) {
        if (!isFinishing && !isDestroyed) {
            val message = if (result.success) {
                "✅ Alerta enviada (ID: ${result.messageId.take(8)})"
            } else {
                "❌ Fallo al enviar: ${result.error ?: "Sin conexión"}"
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmAndSend(type: AlertType) {
        // En producción, aquí podrías agregar ubicación automática
        val location = getCurrentLocationIfAvailable()
        viewModel.sendAlert(type, location)
    }

    private fun showSOSConfirmation() {
        // Diálogo simple para confirmar SOS (evitar activación accidental)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Confirmar Emergencia")
            .setMessage("¿Estás en peligro inminente? Esta alerta se enviará a todos tus contactos de confianza.")
            .setPositiveButton("ENVIAR SOS") { _, _ ->
                confirmAndSend(AlertType.SOS)
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .show()
    }

    private fun getCurrentLocationIfAvailable(): LocationData? {
        // TODO: Implementar obtención de ubicación con permisos
        // Por ahora, retornamos null para no bloquear funcionalidad
        return null
    }

    @OptIn(ExperimentalPermissionsApi::class)
    private fun checkPermissionsAndInitialize() {
        // Verificar Bluetooth encendido
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Activa Bluetooth para usar EcoGuardian", Toast.LENGTH_LONG).show()
            return
        }

        // Verificar permisos según versión de Android
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+: permisos granulares de Bluetooth
                val permissions = arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
                val notGranted = permissions.filter {
                    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }
                
                if (notGranted.isEmpty()) {
                    viewModel.initialize()
                } else {
                    bluetoothPermissionLauncher.launch(permissions)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-11: ubicación fina para Bluetooth scanning
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.initialize()
                } else {
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                        if (granted) viewModel.initialize() else showPermissionDenied()
                    }.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else -> {
                // Android <10: permisos básicos ya deberían estar concedidos
                viewModel.initialize()
            }
        }
    }

    private fun showPermissionDenied() {
        if (!isFinishing && !isDestroyed) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permisos requeridos")
                .setMessage("EcoGuardian necesita acceso a Bluetooth para funcionar sin internet. Puedes habilitarlo en Configuración > Aplicaciones.")
                .setPositiveButton("Ir a Configuración") { _, _ ->
                    // TODO: Abrir pantalla de configuración de la app
                }
                .setNegativeButton("Salir", { _, _ -> finish() })
                .setCancelable(false)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.shutdown()
    }
}
