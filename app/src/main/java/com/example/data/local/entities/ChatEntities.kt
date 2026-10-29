package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val personality: String = "JARVIS",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val imageUri: String? = null
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: String = "Today",
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val isCompleted: Boolean = false,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topic: String,
    val question: String,
    val answer: String,
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    val isMastered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "Commander",
    val userTitle: String = "Quantum Initiate",
    val xp: Int = 120,
    val level: Int = 1,
    val coins: Int = 50,
    val streakDays: Int = 1,
    val lastCheckInDate: String = "",
    val totalMessagesCount: Int = 0,
    val quizzesSolvedCount: Int = 0
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String = "",
    val phoneNumber: String = "",
    val password: String,
    val planType: String = "FREE_TRIAL", // "FREE_TRIAL", "PRO", "PREMIUM", "OWNER"
    val trialExpiryDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L), // 30 days
    val subscriptionExpiryDate: Long = 0L,
    val isVerified: Boolean = true,
    val utrPaymentId: String = "",
    val registeredAt: Long = System.currentTimeMillis()
)

