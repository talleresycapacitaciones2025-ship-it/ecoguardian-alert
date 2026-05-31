package org.ecoguardian.data.model

import java.util.UUID
import java.util.Date

data class AlertPayload(
    val id: String = UUID.randomUUID().toString(),
    val type: AlertType,
    val timestamp: Long = System.currentTimeMillis(),
    val location: LocationData? = null,
    val metadata: Map<String, String> = emptyMap(),
    val senderFingerprint: String? = null // Para verificación criptográfica
) {
    fun toEncryptedString(): String {
        // Aquí iría la lógica de serialización + cifrado
        // Por ahora, retornamos JSON simple para desarrollo
        return """{"id":"$id","type":"${type.name}","ts":$timestamp}"""
    }
}

enum class AlertType(val label: String, val priority: Int) {
    THREAT("Amenaza detectada", 1),
    SOS("EMERGENCIA - SOS", 2),
    CHECK_IN("Estoy a salvo", 0)
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val source: String = "manual" // gps, network, manual
)
