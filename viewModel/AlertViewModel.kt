package org.ecoguardian.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.ecoguardian.data.model.AlertPayload
import org.ecoguardian.data.model.AlertType
import org.ecoguardian.data.model.PeerInfo
import org.ecoguardian.data.repository.AlertRepository
import timber.log.Timber
import javax.inject.Inject

class AlertViewModel @Inject constructor(
    private val repository: AlertRepository
) : ViewModel() {

    // Estados de la UI
    private val _networkStatus = MutableStateFlow(NetworkStatus.DISCONNECTED)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus

    private val _peers = MutableStateFlow<List<PeerInfo>>(emptyList())
    val peers: StateFlow<List<PeerInfo>> = _peers

    private val _sendingAlert = MutableStateFlow(false)
    val sendingAlert: StateFlow<Boolean> = _sendingAlert

    private val _lastAlertResult = MutableStateFlow<AlertResult?>(null)
    val lastAlertResult: StateFlow<AlertResult?> = _lastAlertResult

    // Inicialización
    fun initialize() {
        viewModelScope.launch {
            _networkStatus.value = NetworkStatus.INITIALIZING
            val result = repository.initializeNetwork()
            _networkStatus.value = if (result.isSuccess) {
                NetworkStatus.CONNECTED
            } else {
                NetworkStatus.ERROR
            }
            observePeers()
            observeDeliveries()
        }
    }

    private fun observePeers() {
        repository.observePeers { peerList ->
            _peers.value = peerList
            _networkStatus.value = if (peerList.isNotEmpty()) {
                NetworkStatus.CONNECTED
            } else {
                NetworkStatus.DISCONNECTED
            }
        }
    }

    private fun observeDeliveries() {
        repository.observeDelivery { messageId, success ->
            viewModelScope.launch {
                _sendingAlert.value = false
                _lastAlertResult.value = AlertResult(
                    messageId = messageId,
                    success = success,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    // Acción principal: enviar alerta
    fun sendAlert(type: AlertType, location: org.ecoguardian.data.model.LocationData? = null) {
        if (_sendingAlert.value) return // Prevenir envíos duplicados
        
        viewModelScope.launch {
            _sendingAlert.value = true
            _lastAlertResult.value = null
            
            val payload = AlertPayload(
                type = type,
                location = location,
                metadata = mapOf("app_version" to "1.1.0", "device_model" to android.os.Build.MODEL)
            )
            
            val result = repository.sendAlert(payload)
            
            if (result.isFailure && _sendingAlert.value) {
                // Solo actualizar si no hubo callback de entrega
                _sendingAlert.value = false
                _lastAlertResult.value = AlertResult(
                    messageId = payload.id,
                    success = false,
                    timestamp = System.currentTimeMillis(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // Modo pánico: borrar datos locales sensibles
    fun triggerPanicMode() {
        viewModelScope.launch {
            Timber.w("Modo pánico activado - limpiando datos locales")
            // TODO: Implementar borrado seguro de caché, preferencias, logs
            // TODO: Cerrar conexión de red inmediatamente
            shutdown()
        }
    }

    fun shutdown() {
        repository.shutdown()
    }

    override fun onCleared() {
        shutdown()
        super.onCleared()
    }
}

// Estados para la UI
enum class NetworkStatus {
    INITIALIZING, CONNECTED, DISCONNECTED, ERROR
}

data class AlertResult(
    val messageId: String,
    val success: Boolean,
    val timestamp: Long,
    val error: String? = null
)
