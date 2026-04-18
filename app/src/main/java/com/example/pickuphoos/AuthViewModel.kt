package com.example.pickuphoos.viewmodel

import android.app.Application
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── Auth UI State ────────────────────────────────────────────────────────────

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
    object NewUser : AuthState()   // first-time user → route to Preference screen
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    // Expose current Firebase user so NavGraph can check session on launch
    val currentUser: FirebaseUser? get() = auth.currentUser

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // ── Email / Password Sign-In ──────────────────────────────────────────────

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Sign-in failed")
                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    friendlyAuthError(e.message ?: "Sign-in failed")
                )
            }
        }
    }

    // ── Email / Password Create Account ──────────────────────────────────────

    fun createAccountWithEmail(email: String, name: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Account creation failed")

                // Save user profile to Firestore
                saveUserProfile(
                    uid = user.uid,
                    name = name,
                    email = email,
                    isNewUser = true
                )

                _authState.value = AuthState.NewUser   // route to Preference setup
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    friendlyAuthError(e.message ?: "Account creation failed")
                )
            }
        }
    }

    // ── Google Sign-In (Credential Manager) ──────────────────────────────────
    //
    // Call this from your Activity/Composable, passing the Activity context.
    // Replace WEB_CLIENT_ID with your OAuth 2.0 Web Client ID from
    // Firebase Console → Authentication → Sign-in method → Google → Web SDK config

    fun signInWithGoogle(activityContext: android.content.Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentialManager = CredentialManager.create(activityContext)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)   // allow any Google account
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialResponse = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )

                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(credentialResponse.credential.data)
                    .idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val result = auth.signInWithCredential(firebaseCredential).await()
                val user = result.user ?: throw Exception("Google sign-in failed")
                val isNew = result.additionalUserInfo?.isNewUser == true

                if (isNew) {
                    saveUserProfile(
                        uid = user.uid,
                        name = user.displayName ?: "",
                        email = user.email ?: "",
                        isNewUser = true
                    )
                    _authState.value = AuthState.NewUser
                } else {
                    _authState.value = AuthState.Success(user)
                }

            } catch (e: GetCredentialException) {
                _authState.value = AuthState.Error("Google sign-in cancelled or unavailable")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    friendlyAuthError(e.message ?: "Google sign-in failed")
                )
            }
        }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    // ── Reset State ───────────────────────────────────────────────────────────

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    // ── Firestore: Save User Profile ─────────────────────────────────────────

    private suspend fun saveUserProfile(
        uid: String,
        name: String,
        email: String,
        isNewUser: Boolean
    ) {
        val userDoc = mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "avatarUrl" to "",
            "sportPreferences" to emptyList<String>(),
            "preferredTime" to "",
            "preferredLocation" to "",
            "showContactInfo" to false,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(userDoc).await()
    }

    // ── Error Message Mapper ──────────────────────────────────────────────────

    private fun friendlyAuthError(raw: String): String = when {
        raw.contains("no user record", ignoreCase = true) ->
            "No account found with this email."
        raw.contains("password is invalid", ignoreCase = true) ->
            "Incorrect password. Please try again."
        raw.contains("email address is already in use", ignoreCase = true) ->
            "An account already exists with this email."
        raw.contains("badly formatted", ignoreCase = true) ->
            "Please enter a valid email address."
        raw.contains("network", ignoreCase = true) ->
            "Network error. Check your connection and try again."
        else -> raw
    }

    companion object {
        // TODO: Replace with your actual Web Client ID from Firebase Console
        private const val WEB_CLIENT_ID =
            "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
    }
}
