package com.satya.uno.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/** Google and email/password sign-in through Firebase Auth. */
object Auth {
    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user: StateFlow<FirebaseUser?> = _user

    private val auth get() = FirebaseAuth.getInstance()

    fun init() {
        if (!Cloud.enabled) return
        auth.addAuthStateListener { _user.value = it.currentUser }
    }

    fun displayName(u: FirebaseUser?): String =
        u?.displayName?.takeIf { it.isNotBlank() } ?: u?.email?.substringBefore('@') ?: "Player"

    suspend fun signIn(email: String, password: String) = guard {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUp(name: String, email: String, password: String) = guard {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.updateProfile(userProfileChangeRequest { displayName = name.trim() })?.await()
        result.user?.reload()?.await()
        _user.value = auth.currentUser
    }

    suspend fun resetPassword(email: String) = guard { auth.sendPasswordResetEmail(email.trim()).await() }

    suspend fun google(context: Context) = guard {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) throw RoomError("Google sign-in isn't configured for this build")
        val option = GetSignInWithGoogleOption.Builder(context.getString(id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw RoomError("Google sign-in failed")
        }
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    }

    fun signOut() {
        if (Cloud.enabled) auth.signOut()
    }

    private suspend fun guard(block: suspend () -> Unit): Result<Unit> = try {
        if (!Cloud.enabled) throw RoomError("Online features aren't configured in this build")
        block()
        Result.success(Unit)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
        Result.failure(RoomError("Sign-in cancelled"))
    } catch (e: androidx.credentials.exceptions.NoCredentialException) {
        Result.failure(RoomError("No Google account found on this device"))
    } catch (e: FirebaseAuthException) {
        Result.failure(RoomError(authMessage(e)))
    } catch (e: Exception) {
        Result.failure(RoomError(e.friendly()))
    }

    private fun authMessage(e: FirebaseAuthException) = when (e.errorCode) {
        "ERROR_INVALID_EMAIL" -> "That email address looks wrong"
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND" -> "Wrong email or password"
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists"
        "ERROR_WEAK_PASSWORD" -> "Use at least 6 characters for the password"
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later"
        "ERROR_USER_DISABLED" -> "This account has been disabled"
        else -> e.message ?: "Sign-in failed"
    }
}
