package org.ecoguardian.data.network

import org.ecoguardian.data.model.AlertPayload
import org.ecoguardian.data.model.PeerInfo

interface MeshNetworkService {
    suspend fun initialize(): Result<Boolean>
    suspend fun sendAlert(payload: AlertPayload): Result<String> // Retorna ID de mensaje
    fun getConnectedPeers(): List<PeerInfo>
    fun isNetworkReady(): Boolean
    fun shutdown()
    
    // Callbacks para observación de estado
    fun setPeerChangeListener(listener: (List<PeerInfo>) -> Unit)
    fun setAlertDeliveryListener(listener: (String, Boolean) -> Unit)
}
