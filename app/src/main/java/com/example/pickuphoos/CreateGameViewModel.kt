package com.example.pickuphoos.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pickuphoos.model.SportType
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ─── UI State ─────────────────────────────────────────────────────────────────

data class CreateGameUiState(
    val sport: SportType? = null,
    val locationName: String = "The Lawn, UVA",
    val location: LatLng = LatLng(38.0356, -78.5036),
    val calendar: Calendar = Calendar.getInstance().apply {
        // Default to next hour, minute=0
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
    },
    val maxPlayers: Int = 6,
    val thumbnailUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdGameId: String? = null
) {
    val dateLabel: String get() {
        val today = Calendar.getInstance()
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        return when {
            isSameDay(calendar, today) -> "Today"
            isSameDay(calendar, tomorrow) -> "Tomorrow"
            else -> SimpleDateFormat("MMM d", Locale.US).format(calendar.time)
        }
    }

    val timeLabel: String get() = SimpleDateFormat("h:mm a", Locale.US).format(calendar.time)

    val isFormValid: Boolean get() = sport != null && locationName.isNotBlank() && maxPlayers >= 2
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
    a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class CreateGameViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private val _uiState = MutableStateFlow(CreateGameUiState())
    val uiState: StateFlow<CreateGameUiState> = _uiState.asStateFlow()

    fun setSport(sport: SportType) {
        _uiState.value = _uiState.value.copy(sport = sport)
    }

    fun setLocation(name: String, latLng: LatLng) {
        _uiState.value = _uiState.value.copy(locationName = name, location = latLng)
    }

    fun setDate(year: Int, month: Int, day: Int) {
        val cal = _uiState.value.calendar.clone() as Calendar
        cal.set(year, month, day)
        _uiState.value = _uiState.value.copy(calendar = cal)
    }

    fun setTime(hour: Int, minute: Int) {
        val cal = _uiState.value.calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        _uiState.value = _uiState.value.copy(calendar = cal)
    }

    fun adjustMaxPlayers(delta: Int) {
        val newVal = (_uiState.value.maxPlayers + delta).coerceIn(2, 30)
        _uiState.value = _uiState.value.copy(maxPlayers = newVal)
    }

    fun setThumbnail(uri: Uri) {
        _uiState.value = _uiState.value.copy(thumbnailUri = uri)
    }

    // ── Camera URI ────────────────────────────────────────────────────────────

    fun prepareCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_images").also { it.mkdirs() }
        val file = File(dir, "game_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    fun createGame() {
        val uid = auth.currentUser?.uid
        val state = _uiState.value
        if (uid == null) {
            _uiState.value = state.copy(errorMessage = "You must be signed in")
            return
        }
        if (!state.isFormValid) {
            _uiState.value = state.copy(errorMessage = "Please fill in all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, errorMessage = null)
            try {
                // 1. Upload thumbnail if provided
                var thumbnailUrl = ""
                state.thumbnailUri?.let { uri ->
                    val ref = storage.reference.child("game_thumbs/$uid/${UUID.randomUUID()}.jpg")
                    ref.putFile(uri).await()
                    thumbnailUrl = ref.downloadUrl.await().toString()
                }

                // 2. Get user's display name from Firestore
                val userDoc = db.collection("users").document(uid).get().await()
                val hostName = userDoc.getString("name") ?: "Host"

                // 3. Build game doc
                val gameDoc = mapOf(
                    "hostUid" to uid,
                    "hostName" to hostName,
                    "sport" to state.sport!!.name,
                    "locationName" to state.locationName,
                    "latitude" to state.location.latitude,
                    "longitude" to state.location.longitude,
                    "format" to "Open",
                    "startTime" to Timestamp(state.calendar.time),
                    "maxPlayers" to state.maxPlayers,
                    "playerUids" to listOf(uid),  // host auto-joins
                    "thumbnailUrl" to thumbnailUrl,
                    "timeLabel" to formatTimeLabel(state.calendar),
                    "createdAt" to Timestamp.now()
                )

                // 4. Write to Firestore
                val docRef = db.collection("games").add(gameDoc).await()

                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    createdGameId = docRef.id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Failed to create game: ${e.message}"
                )
            }
        }
    }

    private fun formatTimeLabel(cal: Calendar): String {
        val today = Calendar.getInstance()
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
        return when {
            isSameDay(cal, today) -> "Today $timeStr"
            isSameDay(cal, tomorrow) -> "Tomorrow $timeStr"
            else -> "${SimpleDateFormat("MMM d", Locale.US).format(cal.time)} $timeStr"
        }
    }
}
