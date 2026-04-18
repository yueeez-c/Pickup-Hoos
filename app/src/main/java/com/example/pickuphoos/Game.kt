package com.example.pickuphoos.model

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp

// ─── Sport Type ───────────────────────────────────────────────────────────────

enum class SportType(
    val displayName: String,
    val emoji: String,
    val pinColor: Color
) {
    SOCCER("Soccer", "⚽", Color(0xFFFF6B2B)),
    BASKETBALL("Basketball", "🏀", Color(0xFFE8A04A)),
    TENNIS("Tennis", "🎾", Color(0xFF4AC26B)),
    FRISBEE("Frisbee", "🥏", Color(0xFF7B6EF6)),
    PICKLEBALL("Pickleball", "🏓", Color(0xFF4A90D9)),
    VOLLEYBALL("Volleyball", "🏐", Color(0xFFD94A6B));

    companion object {
        fun fromString(value: String): SportType =
            entries.firstOrNull { it.name == value } ?: SOCCER
    }
}

// ─── Game Model ───────────────────────────────────────────────────────────────

data class Game(
    val id: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val sport: SportType = SportType.SOCCER,
    val locationName: String = "",
    val location: LatLng = LatLng(38.0336, -78.5080),
    val format: String = "",           // e.g. "3v3", "5v5", "Open"
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val maxPlayers: Int = 10,
    val playersJoined: Int = 1,
    val playerUids: List<String> = emptyList(),
    val thumbnailUrl: String = "",     // Firebase Storage URL
    val isJoined: Boolean = false,     // local computed flag
    val timeLabel: String = ""         // e.g. "Now – 2:00 PM", "Tomorrow 3 PM"
)

// ─── Firestore Mapper ─────────────────────────────────────────────────────────
//
// Used to convert Firestore document snapshots → Game objects.

fun Map<String, Any>.toGame(docId: String, currentUserUid: String): Game {
    val lat = (this["latitude"] as? Double) ?: 38.0336
    val lng = (this["longitude"] as? Double) ?: -78.5080
    val playerUids = (this["playerUids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    return Game(
        id = docId,
        hostUid = this["hostUid"] as? String ?: "",
        hostName = this["hostName"] as? String ?: "",
        sport = SportType.fromString(this["sport"] as? String ?: ""),
        locationName = this["locationName"] as? String ?: "",
        location = LatLng(lat, lng),
        format = this["format"] as? String ?: "",
        startTime = this["startTime"] as? Timestamp,
        endTime = this["endTime"] as? Timestamp,
        maxPlayers = (this["maxPlayers"] as? Long)?.toInt() ?: 10,
        playersJoined = playerUids.size,
        playerUids = playerUids,
        thumbnailUrl = this["thumbnailUrl"] as? String ?: "",
        isJoined = playerUids.contains(currentUserUid),
        timeLabel = this["timeLabel"] as? String ?: ""
    )
}

// ─── Firestore Serializer ─────────────────────────────────────────────────────
//
// Used when creating a new game document.

fun Game.toFirestoreMap(): Map<String, Any?> = mapOf(
    "hostUid" to hostUid,
    "hostName" to hostName,
    "sport" to sport.name,
    "locationName" to locationName,
    "latitude" to location.latitude,
    "longitude" to location.longitude,
    "format" to format,
    "startTime" to startTime,
    "endTime" to endTime,
    "maxPlayers" to maxPlayers,
    "playerUids" to playerUids,
    "thumbnailUrl" to thumbnailUrl,
    "timeLabel" to timeLabel,
    "createdAt" to Timestamp.now()
)
