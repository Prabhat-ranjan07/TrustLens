package com.marwadiuniversity.trustlens.data.repository

import com.marwadiuniversity.trustlens.domain.model.UserProfile

interface UserRepository {
    suspend fun getCurrentUser(): UserProfile?
    suspend fun getUserProfile(uid: String): UserProfile?
    suspend fun updateUserProfile(profile: UserProfile): Boolean
    suspend fun signIn(email: String, password: String): Result<UserProfile>
    suspend fun signUp(name: String, email: String, password: String): Result<UserProfile>
    suspend fun resetPassword(email: String): Result<Unit>
    fun signOut()
}
