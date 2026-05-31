package org.ecoguardian.data.repository

import org.ecoguardian.data.model.AlertPayload
import org.ecoguardian.data.network.MeshNetworkService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val meshService: MeshNetworkService
) {
    
    suspend fun sendAlert(payload: AlertPayload): Result<String> {
        if (!meshService.isNetworkReady()) {
            return Result.failure(Exception("Red mesh no disponible"))
        }
        return meshService.sendAlert(payload)
    }
    
    fun getConnectedPeers() = meshService.getConnectedPeers()
    
    fun observePeers(callback: (List<org.ecoguardian.data.model.PeerInfo>) -> Unit) {
        meshService.setPeerChangeListener(callback)
    }
    
    fun observeDelivery(callback: (String, Boolean) -> Unit) {
        meshService.setAlertDeliveryListener(callback)
    }
    
    suspend fun initializeNetwork(): Result<Boolean> = meshService.initialize()
    
    fun shutdown() = meshService.shutdown()
}
