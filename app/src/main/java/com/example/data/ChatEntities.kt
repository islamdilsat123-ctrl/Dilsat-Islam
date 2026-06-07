package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_rooms")
data class ChatRoom(
    @PrimaryKey val id: String,
    val name: String,
    val avatarEmoji: String = "💬",
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMsgTimestamp: Long = System.currentTimeMillis(),
    val typingStatus: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val roomId: String,
    val senderId: String, // "me" or contactId
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val mediaUri: String? = null,
    val type: String = "TEXT", // "TEXT", "IMAGE", "VIDEO", "VOICE"
    val voiceDuration: Int = 0, // In seconds, for voice notes
    val reactions: String = "" // e.g. "👍,❤️" comma separated or empty
)

@Entity(tableName = "status_stories")
data class StatusStory(
    @PrimaryKey(autoGenerate = true) val storyId: Long = 0,
    val userName: String,
    val userAvatarEmoji: String = "👤",
    val caption: String,
    val mediaUri: String? = null, // Path/Name for mock images
    val isTextOnly: Boolean = true,
    val backgroundColorHex: String = "#128C7E",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "me",
    val name: String = "আমার নাম",
    val phone: String = "+880 1712-345678",
    val statusText: String = "গল্প করি... সর্বদা সুরক্ষায়!",
    val avatarEmoji: String = "🇧🇩",
    val encryptionKeyId: String = "GK-E2E-99C12",
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true
)
