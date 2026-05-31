package org.ecoguardian.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.ecoguardian.data.model.AlertPayload
import org.ecoguardian.data.model.PeerInfo
import org.ecoguardian.data.model.ConnectionType
import java.util.UUID

class MockMeshService : MeshNetworkService {
    
    private val peers = MutableStateFlow<List<PeerInfo>>(emptyList())
    private var peerChangeListener: ((List<PeerInfo>) -> Unit)? = null
    private var deliveryListener: ((String, Boolean) -> Unit)? = null
    
    private val mockPeers = listOf(
        PeerInfo(
            id = "peer_001",
            name = "María - Guardabosques",
            publicKeyFingerprint = "A1:B2:C3:D4:E5:F6",
            lastSeen = System.currentTimeMillis(),
            isTrusted = true,
            connectionType = ConnectionType.BLUETOOTH_LE
        ),
        PeerInfo(
            id = "peer_002",
            name = "Carlos - Comunidad",
            publicKeyFingerprint = "F6:E5:D4:C3:B2:A1",
            lastSeen = System.currentTimeMillis() - 30000,
            isTrusted = true,
            connectionType = ConnectionType.WIFI_DIRECT
        )
    )

    override suspend fun initialize(): Result<Boolean> {
        delay(800) // Simular inicialización
        peers.value = mockPeers
        peerChangeListener?.invoke(mockPeers)
        return Result.success(true)
    }

    override suspend fun sendAlert(payload: AlertPayload): Result<String> {
        // Simular envío con posible fallo
        delay(1200)
        val success = Math.random() > 0.1 // 90% de éxito en pruebas
        
        if (success) {
            val messageId = "msg_${UUID.randomUUID().toString().take(8)}"
            deliveryListener?.invoke(messageId, true)
            return Result.success(messageId)
        } else {
            deliveryListener?.invoke(payload.id, false)
            return Result.failure(Exception("Fallo de red simulado"))
        }
    }

    override fun getConnectedPeers(): List<PeerInfo> = peers.value

    override fun isNetworkReady(): Boolean = peers.value.isNotEmpty()

    override fun shutdown() {
        peers.value = emptyList()
    }

    override fun setPeerChangeListener(listener: (List<PeerInfo>) -> Unit) {
        peerChangeListener = listener
    }

    override fun setAlertDeliveryListener(listener: (String, Boolean) -> Unit) {
        deliveryListener = listener
    }
}
