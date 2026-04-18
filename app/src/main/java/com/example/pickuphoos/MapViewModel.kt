package com.example.pickuphoos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pickuphoos.model.Game
import com.example.pickuphoos.model.SportType
import com.example.pickuphoos.model.toGame
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val currentUid get() = auth.currentUser?.uid ?: ""

    // ── Raw games from Firestore (all sports) ─────────────────────────────────
    private val _allGames = MutableStateFlow<List<Game>>(emptyList())

    // ── Active sport filter (null = All) ─────────────────────────────────────
    private val _selectedSport = MutableStateFlow<SportType?>(null)
    val selectedSport: StateFlow<SportType?> = _selectedSport.asStateFlow()

    // ── Filtered games exposed to UI ──────────────────────────────────────────
    val filteredGames: StateFlow<List<Game>> = combine(_allGames, _selectedSport) { games, sport ->
        if (sport == null) games else games.filter { it.sport == sport }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Selected pin / game ───────────────────────────────────────────────────
    private val _selectedGame = MutableStateFlow<Game?>(null)
    val selectedGame: StateFlow<Game?> = _selectedGame.asStateFlow()

    // ── Firestore real-time listener ──────────────────────────────────────────
    private var listenerReg: ListenerRegistration? = null

    init {
        startListening()
    }

    private fun startListening() {
        listenerReg = db.collection("games")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val games = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.data?.toGame(docId = doc.id, currentUserUid = currentUid)
                    }.getOrNull()
                }
                _allGames.value = games
            }
    }

    // ── Sport filter ──────────────────────────────────────────────────────────

    fun setFilter(sport: SportType?) {
        _selectedSport.value = sport
        _selectedGame.value = null
    }

    // ── Pin selection ─────────────────────────────────────────────────────────

    fun selectGame(game: Game) {
        _selectedGame.value = if (_selectedGame.value?.id == game.id) null else game
    }

    // ── Join / Leave game ─────────────────────────────────────────────────────

    fun joinGame(game: Game) {
        viewModelScope.launch {
            val uid = currentUid.ifBlank { return@launch }
            val gameRef = db.collection("games").document(game.id)

            try {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(gameRef)
                    val players = (snapshot.get("playerUids") as? List<*>)
                        ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                    val max = (snapshot.getLong("maxPlayers") ?: 10).toInt()

                    if (uid in players) {
                        // Leave
                        players.remove(uid)
                    } else if (players.size < max) {
                        // Join
                        players.add(uid)
                    }
                    transaction.update(gameRef, "playerUids", players)
                }.await()
            } catch (e: Exception) {
                // Handle join error — surface via a separate error StateFlow if needed
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        listenerReg?.remove()
    }
}
