package com.marwadiuniversity.trustlens.domain.model

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val profileImageUrl: String?,
    val securityScore: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
