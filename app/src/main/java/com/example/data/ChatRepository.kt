package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allRooms: Flow<List<ChatRoom>> = chatDao.getAllRooms()
    val allStories: Flow<List<StatusStory>> = chatDao.getAllStories()
    val userProfile: Flow<UserProfile?> = chatDao.getUserProfileFlow()

    fun getMessagesForRoom(roomId: String): Flow<List<Message>> = chatDao.getMessagesForRoom(roomId)

    suspend fun insertMessage(message: Message): Long = withContext(Dispatchers.IO) {
        val msgId = chatDao.insertMessage(message)
        // Also update the room's last message
        val room = chatDao.getRoomById(message.roomId)
        if (room != null) {
            val updatedRoom = room.copy(
                lastMessage = message.content,
                lastMsgTimestamp = message.timestamp,
                unreadCount = if (message.senderId == "me") room.unreadCount else room.unreadCount + 1
            )
            chatDao.insertRoom(updatedRoom)
        }
        msgId
    }

    suspend fun updateMessageReactions(messageId: Long, reactions: String) = withContext(Dispatchers.IO) {
        chatDao.updateMessageReactions(messageId, reactions)
    }

    suspend fun createRoom(chatRoom: ChatRoom) = withContext(Dispatchers.IO) {
        chatDao.insertRoom(chatRoom)
    }

    suspend fun updateRoomTypingStatus(roomId: String, typingStatus: String) = withContext(Dispatchers.IO) {
        chatDao.updateRoomTypingStatus(roomId, typingStatus)
    }

    suspend fun markRoomAsRead(roomId: String) = withContext(Dispatchers.IO) {
        chatDao.markRoomAsRead(roomId)
    }

    suspend fun addStory(story: StatusStory) = withContext(Dispatchers.IO) {
        chatDao.insertStory(story)
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        chatDao.saveUserProfile(profile)
    }

    suspend fun getUserProfileDirect(): UserProfile = withContext(Dispatchers.IO) {
        chatDao.getUserProfileDirect() ?: UserProfile()
    }

    suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteMessage(messageId)
    }

    // Initialize Default Seed Data if Empty
    suspend fun checkAndPrepopulateData() = withContext(Dispatchers.IO) {
        val currentProfile = chatDao.getUserProfileDirect()
        if (currentProfile == null) {
            // Save initial profile
            chatDao.saveUserProfile(UserProfile())
        }

        // Just check if rooms are empty
        val roomsList = chatDao.getAllRooms().let {
            // we check rooms inside a suspend block
            // Note: Since Flow isn't immediately terminal, we can query direct or count via a quick check
        }

        // Let's force load initial items to ensure smooth onboarding
        if (chatDao.getRoomById("chatbot") == null) {
            // Seed Chat Rooms
            chatDao.insertRoom(ChatRoom(
                id = "chatbot",
                name = "গল্প বট (Golpo Bot AI)",
                avatarEmoji = "🤖",
                isGroup = false,
                lastMessage = "আসসালামু আলাইকুম! আমি গল্প বট। আপনার সাথে বাংলায় চমৎকার গল্প করতে পারি। লিখুন!",
                lastMsgTimestamp = System.currentTimeMillis() - 5000,
                isOnline = true
            ))

            chatDao.insertRoom(ChatRoom(
                id = "room_family",
                name = "আমরা ক’জন (Family Group)",
                avatarEmoji = "👨‍👩‍👧‍👦",
                isGroup = true,
                lastMessage = "আজ রাতে গল্প করার পর কি বিরিয়ানি রান্না হবে? 😋",
                lastMsgTimestamp = System.currentTimeMillis() - 3600000,
                isOnline = false
            ))

            chatDao.insertRoom(ChatRoom(
                id = "room_sajib",
                name = "সজীব (Sajib)",
                avatarEmoji = "🏎️",
                isGroup = false,
                lastMessage = "দোস্ত, নতুন আপডেটটা অস্থির হইছে! অনেক ফাস্ট!",
                lastMsgTimestamp = System.currentTimeMillis() - 7200000,
                isOnline = true
            ))

            chatDao.insertRoom(ChatRoom(
                id = "room_mim",
                name = "মিম আপু (Mim)",
                avatarEmoji = "🙋‍♀️",
                isGroup = false,
                lastMessage = "ভয়েস নোটটা শুনো আর রিভিউ দাও।",
                lastMsgTimestamp = System.currentTimeMillis() - 86400000,
                isOnline = false,
                unreadCount = 1
            ))

            // Seed Initial Messages
            // 1. Chatbot Room
            chatDao.insertMessage(Message(
                roomId = "chatbot",
                senderId = "chatbot",
                senderName = "গল্প বট",
                content = "আসসালামু আলাইকুম! গল্প করি মেসেঞ্জারে আপনাকে স্বাগতম। এই চ্যাটটি এন্ড-টু-এন্ড এনক্রিপ্টেড এবং সম্পূর্ণ সুরক্ষিত।",
                timestamp = System.currentTimeMillis() - 100000
            ))
            chatDao.insertMessage(Message(
                roomId = "chatbot",
                senderId = "chatbot",
                senderName = "গল্প বট",
                content = "আমি একটি এডভান্সড জেমিনি এআই অ্যাসিস্ট্যান্ট। বাংলায় আপনার সাথে চ্যাট বা অনুবাদ করতে পারি। যেকোনো প্রশ্ন করতে পারেন!",
                timestamp = System.currentTimeMillis() - 80000
            ))

            // 2. Family Group Room
            chatDao.insertMessage(Message(
                roomId = "room_family",
                senderId = "mom01",
                senderName = "আম্মু",
                content = "আজ রাতে সবাই জলদি বাসায় এসো। জরুরি গল্প আছে। ❤️",
                timestamp = System.currentTimeMillis() - 15000000
            ))
            chatDao.insertMessage(Message(
                roomId = "room_family",
                senderId = "me",
                senderName = "আমি",
                content = "ঠিক আছে আম্মু, আমি ৮টার মধ্যে ইনশাআল্লাহ চলে আসব।",
                timestamp = System.currentTimeMillis() - 14000000
            ))
            chatDao.insertMessage(Message(
                roomId = "room_family",
                senderId = "brother02",
                senderName = "ছোট ভাই",
                content = "আজ রাতে গল্প করার পর কি বিরিয়ানি রান্না হবে? 😋",
                timestamp = System.currentTimeMillis() - 3600000
            ))

            // 3. Sajib Room
            chatDao.insertMessage(Message(
                roomId = "room_sajib",
                senderId = "room_sajib",
                senderName = "সজীব",
                content = "দোস্ত, আজকে বিকালে মাঠে আসবি না ফুটবল খেলতে?",
                timestamp = System.currentTimeMillis() - 14400000
            ))
            chatDao.insertMessage(Message(
                roomId = "room_sajib",
                senderId = "me",
                senderName = "আমি",
                content = "হ্যাঁ, একটু কাজ আছে শেষ করেই আসতাছি।",
                timestamp = System.currentTimeMillis() - 13000000
            ))
            chatDao.insertMessage(Message(
                roomId = "room_sajib",
                senderId = "room_sajib",
                senderName = "সজীব",
                content = "দোস্ত, নতুন আপডেটটা অস্থির হইছে! অনেক ফাস্ট!",
                timestamp = System.currentTimeMillis() - 7200000
            ))

            // 4. Mim Appu Room
            chatDao.insertMessage(Message(
                roomId = "room_mim",
                senderId = "room_mim",
                senderName = "মিম আপু",
                content = "আমার নতুন গানের রেকর্ডিং করেছি!",
                timestamp = System.currentTimeMillis() - 90000000
            ))
            chatDao.insertMessage(Message(
                roomId = "room_mim",
                senderId = "room_mim",
                senderName = "মিম আপু",
                content = "",
                timestamp = System.currentTimeMillis() - 86400000,
                type = "VOICE",
                voiceDuration = 12
            ))

            // Seed Initial Stories
            chatDao.insertStory(StatusStory(
                userName = "সজীব",
                userAvatarEmoji = "🏎️",
                caption = "কিং অফ ক্রিকেট! আজ হাফ-সেঞ্চুরি করলাম! 🏏🔥",
                isTextOnly = true,
                backgroundColorHex = "#075E54",
                timestamp = System.currentTimeMillis() - 7200000
            ))

            chatDao.insertStory(StatusStory(
                userName = "মিম আপু",
                userAvatarEmoji = "🙋‍♀️",
                caption = "সকালের সুন্দর আকাশ! একরাশ শান্তি। 🌅☁️",
                isTextOnly = true,
                backgroundColorHex = "#FF4081",
                timestamp = System.currentTimeMillis() - 14400000
            ))

            chatDao.insertStory(StatusStory(
                userName = "গল্প বট AI",
                userAvatarEmoji = "🤖",
                caption = "জেমিনি এআই এখন গল্প করি অ্যাপ্লিকেশনে সংযুক্ত! 🇧🇩🤖",
                isTextOnly = true,
                backgroundColorHex = "#128C7E",
                timestamp = System.currentTimeMillis() - 28800000
            ))
        }
    }
}
