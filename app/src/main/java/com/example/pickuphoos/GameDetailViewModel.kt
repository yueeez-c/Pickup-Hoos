package com.example.pickuphoos.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pickuphoos.model.Game
import com.example.pickuphoos.model.toGame
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── UI State ─────────────────────────────────────────────────────────────────

data class GameDetailUiState(
    val game: Game? = null,
    val playerNames: List<String> = emptyList(),
    val playerUids: List<String> = emptyList(),
    val playerAvatarUrls: Map<String, String> = emptyMap(),
    val playerContactInfoVisibility: Map<String, Boolean> = emptyMap(),
    val currentUid: String = "",
    val isHost: Boolean = false,
    val isJoined: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class GameDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val gameId: String = savedStateHandle["gameId"] ?: ""
    private val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(GameDetailUiState(currentUid = currentUid))
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private var listenerReg: ListenerRegistration? = null

    init {
        if (gameId.isNotBlank()) startListening()
    }

    private fun startListening() {
        listenerReg = db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("GameDetailVM", "Listener error: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Game not found"
                    )
                    return@addSnapshotListener
                }

                val game = snapshot.data?.toGame(snapshot.id, currentUid)
                if (game == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@addSnapshotListener
                }

                _uiState.value = _uiState.value.copy(
                    game = game,
                    playerUids = game.playerUids,
                    isHost = game.hostUid == currentUid,
                    isJoined = currentUid in game.playerUids,
                    isLoading = false,
                    currentUid = currentUid
                )

                // Fetch player names and info asynchronously
                fetchPlayerInfo(game.playerUids, game.hostName, game.hostUid)
            }
    }

    private fun fetchPlayerInfo(playerUids: List<String>, hostName: String, hostUid: String) {
        viewModelScope.launch {
            try {
                // Order: host first, then others in order of joining
                val sortedUids = playerUids.sortedByDescending { it == hostUid }
                val names = mutableListOf<String>()
                val avatarUrls = mutableMapOf<String, String>()
                val contactInfoVisibility = mutableMapOf<String, Boolean>()

                sortedUids.forEach { uid ->
                    if (uid == hostUid) {
                        names.add(hostName)
                    } else {
                        try {
                            val doc = db.collection("users").document(uid).get().await()
                            names.add(doc.getString("name") ?: "Player")
                        } catch (_: Exception) {
                            names.add("Player")
                        }
                    }

                    // Fetch avatar and privacy settings for all players
                    try {
                        val userDoc = db.collection("users").document(uid).get().await()
                        if (userDoc.exists()) {
                            avatarUrls[uid] = userDoc.getString("avatarUrl") ?: ""
                            contactInfoVisibility[uid] = userDoc.getBoolean("showAvatar") ?: true
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GameDetailVM", "Failed to fetch user info for $uid: ${e.message}")
                        // Default to not showing contact info if we can't load it
                        contactInfoVisibility[uid] = false
                    }
                }

                _uiState.value = _uiState.value.copy(
                    playerNames = names,
                    playerUids = sortedUids,
                    playerAvatarUrls = avatarUrls,
                    playerContactInfoVisibility = contactInfoVisibility
                )
            } catch (e: Exception) {
                android.util.Log.e("GameDetailVM", "Failed to fetch player info: ${e.message}")
            }
        }
    }

    // ── Join / Leave ──────────────────────────────────────────────────────────

    fun toggleJoin() {
        val uid = currentUid.ifBlank { return }
        val gameRef = db.collection("games").document(gameId)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            try {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(gameRef)
                    val players = (snapshot.get("playerUids") as? List<*>)
                        ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                    val max = (snapshot.getLong("maxPlayers") ?: 10).toInt()

                    if (uid in players) {
                        players.remove(uid)
                    } else if (players.size < max) {
                        players.add(uid)
                    }
                    transaction.update(gameRef, "playerUids", players)
                }.await()
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Action failed: ${e.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerReg?.remove()
    }
}