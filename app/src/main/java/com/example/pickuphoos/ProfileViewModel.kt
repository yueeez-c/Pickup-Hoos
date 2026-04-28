package com.example.pickuphoos.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pickuphoos.model.SportType
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
import java.util.UUID

// ─── UI State ─────────────────────────────────────────────────────────────────

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val sportPreferences: List<String> = emptyList(),
    val preferredTime: String = "",
    val preferredLocation: String = "",
    val showContactInfo: Boolean = false,
    val showMyGamesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private val uid get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Temp file URI for camera captures
    private var cameraImageUri: Uri? = null

    init {
        loadProfile()
    }

    // ── Load from Firestore ───────────────────────────────────────────────────

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val doc = db.collection("users").document(uid).get().await()
                val data = doc.data ?: return@launch
                val sports = (data["sportPreferences"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()

                _uiState.value = ProfileUiState(
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: auth.currentUser?.email ?: "",
                    avatarUrl = data["avatarUrl"] as? String ?: "",
                    sportPreferences = sports,
                    preferredTime = data["preferredTime"] as? String ?: "",
                    preferredLocation = data["preferredLocation"] as? String ?: "",
                    showContactInfo = data["showContactInfo"] as? Boolean ?: false,
                    showMyGamesOnly = data["showMyGamesOnly"] as? Boolean ?: false,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // ── Toggle sport preference ───────────────────────────────────────────────

    fun toggleSport(sport: SportType) {
        val current = _uiState.value.sportPreferences.toMutableList()
        if (sport.name in current) current.remove(sport.name)
        else current.add(sport.name)
        _uiState.value = _uiState.value.copy(sportPreferences = current)
        saveField("sportPreferences", current)
    }

    // ── Toggle settings ───────────────────────────────────────────────────────

    fun setShowContactInfo(value: Boolean) {
        _uiState.value = _uiState.value.copy(showContactInfo = value)
        saveField("showContactInfo", value)
    }

    fun setShowMyGamesOnly(value: Boolean) {
        _uiState.value = _uiState.value.copy(showMyGamesOnly = value)
        saveField("showMyGamesOnly", value)
    }

    fun setPreferredTime(value: String) {
        _uiState.value = _uiState.value.copy(preferredTime = value)
        saveField("preferredTime", value)
    }

    fun setPreferredLocation(value: String) {
        _uiState.value = _uiState.value.copy(preferredLocation = value)
        saveField("preferredLocation", value)
    }

    // ── Save single field to Firestore ────────────────────────────────────────

    private fun saveField(field: String, value: Any) {
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .update(field, value).await()
            } catch (_: Exception) {}
        }
    }

    // ── Camera URI provider ───────────────────────────────────────────────────
    //
    // Creates a temp file in cache and returns a content:// URI via FileProvider.
    // Add this to AndroidManifest.xml inside <application>:
    //
    //   <provider
    //     android:name="androidx.core.content.FileProvider"
    //     android:authorities="${applicationId}.provider"
    //     android:exported="false"
    //     android:grantUriPermissions="true">
    //     <meta-data
    //       android:name="android.support.FILE_PROVIDER_PATHS"
    //       android:resource="@xml/file_paths" />
    //   </provider>
    //
    // And create res/xml/file_paths.xml:
    //   <paths>
    //     <cache-path name="camera_images" path="camera_images/" />
    //   </paths>

    fun getCameraUri(): Uri {
        val context: Context = getApplication()
        val dir = File(context.cacheDir, "camera_images").also { it.mkdirs() }
        val file = File(dir, "avatar_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        cameraImageUri = uri
        return uri
    }

    // ── Upload avatar to Firebase Storage ─────────────────────────────────────

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val ref = storage.reference
                    .child("avatars/$uid/${UUID.randomUUID()}.jpg")

                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                // Update Firestore and local state
                db.collection("users").document(uid)
                    .update("avatarUrl", downloadUrl).await()

                _uiState.value = _uiState.value.copy(
                    avatarUrl = downloadUrl,
                    isSaving = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
