package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.daos.ChatDao
import com.example.data.local.daos.FlashcardDao
import com.example.data.local.daos.TaskDao
import com.example.data.local.daos.UserProfileDao
import com.example.data.local.daos.UserAccountDao
import com.example.data.local.entities.AchievementEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.FlashcardEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.local.entities.UserAccountEntity

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        TaskEntity::class,
        FlashcardEntity::class,
        UserProfileEntity::class,
        AchievementEntity::class,
        UserAccountEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun taskDao(): TaskDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nikhil_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
