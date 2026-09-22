package com.marwadiuniversity.trustlens.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.marwadiuniversity.trustlens.domain.model.UserProfile
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(context: Context) : UserRepository {
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    private val db: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    private val prefs: SharedPreferences = context.getSharedPreferences("trustlens_user_session", Context.MODE_PRIVATE)

    override suspend fun getCurrentUser(): UserProfile? {
        val firebaseUser = auth?.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val profile = getUserProfile(uid)
            if (profile != null) return profile

            // If Auth exists but Firestore doc is missing, create default profile
            val name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@")?.replace(".", " ")?.capitalize() ?: "User"
            val email = firebaseUser.email ?: ""
            val newProfile = UserProfile(
                uid = uid,
                name = name,
                email = email,
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
                securityScore = 86,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            updateUserProfile(newProfile)
            return newProfile
        }

        // Fallback local session if any
        val localUid = prefs.getString("uid", null) ?: return null
        return getUserProfile(localUid) ?: UserProfile(
            uid = localUid,
            name = prefs.getString("name", "User") ?: "User",
            email = prefs.getString("email", "") ?: "",
            profileImageUrl = prefs.getString("profileImageUrl", "") ?: "",
            securityScore = prefs.getInt("securityScore", 86)
        )
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            if (db != null) {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    UserProfile(
                        uid = uid,
                        name = doc.getString("name") ?: "User",
                        email = doc.getString("email") ?: "",
                        profileImageUrl = doc.getString("profileImageUrl") ?: "",
                        securityScore = doc.getLong("securityScore")?.toInt() ?: 86,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Boolean {
        return try {
            prefs.edit()
                .putString("uid", profile.uid)
                .putString("name", profile.name)
                .putString("email", profile.email)
                .putString("profileImageUrl", profile.profileImageUrl)
                .putInt("securityScore", profile.securityScore)
                .apply()

            if (db != null) {
                val map = mapOf(
                    "uid" to profile.uid,
                    "name" to profile.name,
                    "email" to profile.email,
                    "profileImageUrl" to (profile.profileImageUrl ?: ""),
                    "securityScore" to profile.securityScore,
                    "createdAt" to profile.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(profile.uid).set(map).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return try {
            if (auth == null) {
                return Result.failure(Exception("Firebase Auth not initialized."))
            }
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Authentication failed."))
            val uid = firebaseUser.uid
            
            val profile = getUserProfile(uid) ?: UserProfile(
                uid = uid,
                name = firebaseUser.displayName ?: email.substringBefore("@").replace(".", " "),
                email = email,
                profileImageUrl = "",
                securityScore = 86,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            updateUserProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseException(e)))
        }
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<UserProfile> {
        return try {
            if (auth == null) {
                return Result.failure(Exception("Firebase Auth not initialized."))
            }
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Registration failed."))
            val uid = firebaseUser.uid

            val profile = UserProfile(
                uid = uid,
                name = name,
                email = email,
                profileImageUrl = "",
                securityScore = 86,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            updateUserProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseException(e)))
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            if (auth == null) {
                return Result.failure(Exception("Firebase Auth not initialized."))
            }
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseException(e)))
        }
    }

    override fun signOut() {
        try { auth?.signOut() } catch (e: Exception) {}
        prefs.edit().clear().apply()
    }

    private fun mapFirebaseException(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
            is FirebaseAuthInvalidUserException -> "No user found with this email."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException -> "Please check your internet connection."
            else -> e.localizedMessage ?: "An unexpected error occurred. Please try again."
        }
    }
}
