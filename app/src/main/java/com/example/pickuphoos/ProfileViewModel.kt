package com.example.pickuphoos.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val cameraUri: Uri? = null,
    val sportPreferences: List<String> = emptyList(),
    val preferredTime: String = "",
    val preferredLocation: String = "",
    val showContactInfo: Boolean = true,
    val showMyGamesOnly: Boolean = false
)

class ProfileViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * Get the URI for saving a camera photo.
     * Creates a temporary file in the app's cache directory and stores it in state.
     * This URI can be passed to the camera intent for saving the captured photo.
     */
    fun getCameraUri(): Uri {
        if (_uiState.value.cameraUri == null) {
            val cacheDir = context.cacheDir
            val imageFile = File(
                cacheDir,
                "camera_${System.currentTimeMillis()}.jpg"
            )
            cacheDir.mkdirs()
            try { imageFile.createNewFile() } catch (e: IOException) { /* log */ }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )
            _uiState.value = _uiState.value.copy(cameraUri = uri)
        }
        return _uiState.value.cameraUri!!
    }

    /**
     * Upload the captured/selected photo to Firebase Storage and update Firestore.
     */
    fun uploadAvatar(photoUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("avatars/$userId/profile.jpg")

        storageRef.putFile(photoUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // Update Firestore with new avatar URL
                    firestore.collection("users").document(userId)
                        .update("avatarUrl", downloadUrl.toString())
                        .addOnSuccessListener {
                            // Update UI state
                            _uiState.value = _uiState.value.copy(
                                avatarUrl = downloadUrl.toString()
                            )
                        }
                }
            }
    }

    /**
     * Load user profile from Firestore.
     */
    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.let {
                    _uiState.value = ProfileUiState(
                        name = it.getString("name") ?: "",
                        email = it.getString("email") ?: "",
                        avatarUrl = it.getString("avatarUrl") ?: "",
                        sportPreferences = (it.get("sportPreferences") as? List<*>)?.map { it.toString() } ?: emptyList(),
                        preferredTime = it.getString("preferredTime") ?: "",
                        preferredLocation = it.getString("preferredLocation") ?: "",
                        showContactInfo = it.getBoolean("showContactInfo") ?: true,
                        showMyGamesOnly = it.getBoolean("showMyGamesOnly") ?: false
                    )
                }
            }
    }

    /**
     * Toggle a sport in the user's preferences.
     */
    fun toggleSport(sport: com.example.pickuphoos.model.SportType) {
        val userId = auth.currentUser?.uid ?: return
        val currentPrefs = _uiState.value.sportPreferences.toMutableList()

        if (sport.name in currentPrefs) {
            currentPrefs.remove(sport.name)
        } else {
            currentPrefs.add(sport.name)
        }

        _uiState.value = _uiState.value.copy(sportPreferences = currentPrefs)

        firestore.collection("users").document(userId)
            .update("sportPreferences", currentPrefs)
    }

    /**
     * Update showContactInfo preference.
     */
    fun setShowContactInfo(value: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(showContactInfo = value)
        firestore.collection("users").document(userId)
            .update("showContactInfo", value)
    }

    /**
     * Update showMyGamesOnly preference.
     */
    fun setShowMyGamesOnly(value: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(showMyGamesOnly = value)
        firestore.collection("users").document(userId)
            .update("showMyGamesOnly", value)
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
    }
}