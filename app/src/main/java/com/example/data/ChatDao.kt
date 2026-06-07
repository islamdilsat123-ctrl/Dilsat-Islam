package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Rooms
    @Query("SELECT * FROM chat_rooms ORDER BY lastMsgTimestamp DESC")
    fun getAllRooms(): Flow<List<ChatRoom>>

    @Query("SELECT * FROM chat_rooms WHERE id = :id LIMIT 1")
    suspend fun getRoomById(id: String): ChatRoom?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(chatRoom: ChatRoom)

    @Update
    suspend fun updateRoom(chatRoom: ChatRoom)

    @Query("UPDATE chat_rooms SET lastMessage = :lastMessage, lastMsgTimestamp = :timestamp, unreadCount = unreadCount + :unreadIncrement WHERE id = :roomId")
    suspend fun updateRoomLastMessage(roomId: String, lastMessage: String, timestamp: Long, unreadIncrement: Int)

    @Query("UPDATE chat_rooms SET typingStatus = :typingStatus WHERE id = :roomId")
    suspend fun updateRoomTypingStatus(roomId: String, typingStatus: String)

    @Query("UPDATE chat_rooms SET unreadCount = 0 WHERE id = :roomId")
    suspend fun markRoomAsRead(roomId: String)

    // Messages
    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("UPDATE messages SET reactions = :reactions WHERE messageId = :messageId")
    suspend fun updateMessageReactions(messageId: Long, reactions: String)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: Long)

    // Stories
    @Query("SELECT * FROM status_stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StatusStory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StatusStory)

    // User Profile
    @Query("SELECT * FROM user_profiles WHERE id = 'me' LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = 'me' LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)
}
