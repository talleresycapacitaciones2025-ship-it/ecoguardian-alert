package org.ecoguardian.data.model

data class PeerInfo(
    val id: String,
    val name: String,
    val publicKeyFingerprint: String,
    val lastSeen: Long,
    val isTrusted: Boolean,
    val connectionType: ConnectionType
)

enum class ConnectionType {
    BLUETOOTH_LE,
    WIFI_DIRECT,
    BRIAR_RELAY
}
