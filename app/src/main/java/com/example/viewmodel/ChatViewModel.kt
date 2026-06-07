package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiManager
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AppScreen {
    object Home : AppScreen()
    data class ChatRoomDetail(val roomId: String) : AppScreen()
    data class GroupMembersInfo(val roomId: String) : AppScreen()
    object ProfileEdit : AppScreen()
}

sealed class CallState {
    object Idle : CallState()
    data class Active(
        val contactId: String,
        val contactName: String,
        val avatarEmoji: String,
        val isVideo: Boolean,
        val isIncoming: Boolean = false,
        val durationSeconds: Int = 0
    ) : CallState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChatDatabase.getDatabase(application)
    private val repository = ChatRepository(database.chatDao())
    private val geminiManager = GeminiManager()

    // Screen navigation state
    var currentScreen by mutableStateOf<AppScreen>(AppScreen.Home)
        private set

    // Active Call state
    var callState by mutableStateOf<CallState>(CallState.Idle)
        private set

    // Active Story Viewer State
    var activeViewingStory by mutableStateOf<StatusStory?>(null)
        private set

    // UI Input / UI State caches
    var searchQuery by mutableStateOf("")
    var currentThemeIsDark by mutableStateOf(false)

    // Flows from database
    val rooms: StateFlow<List<ChatRoom>> = repository.allRooms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<StatusStory>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Currently opened room's messages
    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomMessages: StateFlow<List<Message>> = _activeRoomId
        .flatMapLatest { roomId ->
            if (roomId != null) repository.getMessagesForRoom(roomId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated Call History list (Local State for caller tab)
    var callHistoryList by mutableStateOf<List<CallLog>>(
        listOf(
            CallLog("সজীব", "🏎️", System.currentTimeMillis() - 43200000, true, false),
            CallLog("মিম আপু", "🙋‍♀️", System.currentTimeMillis() - 172800000, false, true)
        )
    )
        private set

    data class CallLog(
        val name: String,
        val avatar: String,
        val timestamp: Long,
        val isVoice: Boolean,
        val missed: Boolean
    )

    init {
        viewModelScope.launch {
            // Seed base configuration data in database
            repository.checkAndPrepopulateData()
            
            // Sync dark mode preference
            userProfile.collect { profile ->
                profile?.let {
                    currentThemeIsDark = it.isDarkMode
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
        if (screen is AppScreen.ChatRoomDetail) {
            _activeRoomId.value = screen.roomId
            viewModelScope.launch {
                repository.markRoomAsRead(screen.roomId)
            }
        } else {
            _activeRoomId.value = null
        }
    }

    // Toggle Dark Mode
    fun toggleDarkMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = repository.getUserProfileDirect()
            val updated = profile.copy(isDarkMode = !profile.isDarkMode)
            repository.saveUserProfile(updated)
        }
    }

    // Save profile edits
    fun updateProfile(name: String, phone: String, statusText: String, avatarEmoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = repository.getUserProfileDirect().copy(
                name = name,
                phone = phone,
                statusText = statusText,
                avatarEmoji = avatarEmoji
            )
            repository.saveUserProfile(profile)
        }
    }

    // Message sending & handling
    fun sendMessage(roomId: String, content: String, type: String = "TEXT", mediaUri: String? = null, voiceDuration: Int = 0) {
        if (content.trim().isEmpty() && mediaUri == null) return

        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val msg = Message(
                roomId = roomId,
                senderId = "me",
                senderName = "আমি",
                content = content,
                timestamp = timestamp,
                type = type,
                mediaUri = mediaUri,
                voiceDuration = voiceDuration,
                isRead = true
            )
            repository.insertMessage(msg)

            // Trigger AI Bot reaction or mock contact reply
            if (roomId == "chatbot") {
                triggerGeminiReply(content)
            } else if (!roomId.startsWith("room_family")) {
                triggerMockReply(roomId)
            }
        }
    }

    // Trigger AI response using Gemini API from Client
    private fun triggerGeminiReply(userPrompt: String) {
        viewModelScope.launch {
            // Set typing indicator
            repository.updateRoomTypingStatus("chatbot", "গল্প বট লিখছে...")
            delay(1500) // Simulated typing delay

            val reply = geminiManager.generateBengaliReply(userPrompt)
            
            val timestamp = System.currentTimeMillis()
            val botMsg = Message(
                roomId = "chatbot",
                senderId = "chatbot",
                senderName = "গল্প বট",
                content = reply,
                timestamp = timestamp
            )
            repository.insertMessage(botMsg)
            repository.updateRoomTypingStatus("chatbot", "")
        }
    }

    // Trigger simulated quick replies for other contacts
    private fun triggerMockReply(roomId: String) {
        viewModelScope.launch {
            val contactRoom = database.chatDao().getRoomById(roomId) ?: return@launch
            repository.updateRoomTypingStatus(roomId, "${contactRoom.name} লিখছে...")
            delay(2000) // typing wait time

            val replyOptions = listOf(
                "ওয়াও দোস্ত! দারুণ খবর। 👍",
                "আমি একটু ব্যস্ত আছি, পরে গল্প করি? 😊",
                "ইনশাআল্লাহ্ কাল দেখা হবে!",
                "আরে না না, তুমি ঠিকই বলেছো! হা হা 😂",
                "গল্প করি মেসেঞ্জার আসলেই অনেক চমৎকার আর সিকিউর! 🇧🇩"
            )
            val randomReply = replyOptions.random()
            
            val timestamp = System.currentTimeMillis()
            val mockMsg = Message(
                roomId = roomId,
                senderId = roomId,
                senderName = contactRoom.name,
                content = randomReply,
                timestamp = timestamp
            )
            repository.insertMessage(mockMsg)
            repository.updateRoomTypingStatus(roomId, "")
        }
    }

    // Message reaction addition
    fun reactToMessage(messageId: Long, emoji: String) {
        viewModelScope.launch {
            repository.updateMessageReactions(messageId, emoji)
        }
    }

    // Create custom user status/story
    fun createStory(caption: String, bgHex: String) {
        viewModelScope.launch {
            val profile = repository.getUserProfileDirect()
            val story = StatusStory(
                userName = profile.name + " (আমি)",
                userAvatarEmoji = profile.avatarEmoji,
                caption = caption,
                isTextOnly = true,
                backgroundColorHex = bgHex
            )
            repository.addStory(story)
        }
    }

    // Story viewer handling
    fun viewStory(story: StatusStory?) {
        activeViewingStory = story
    }

    // Simulated calls trigger
    fun startCall(contactId: String, isVideo: Boolean) {
        viewModelScope.launch {
            val room = database.chatDao().getRoomById(contactId)
            val name = room?.name ?: "অপরিচিত নম্বর"
            val emoji = room?.avatarEmoji ?: "👤"
            
            callState = CallState.Active(
                contactId = contactId,
                contactName = name,
                avatarEmoji = emoji,
                isVideo = isVideo,
                isIncoming = false,
                durationSeconds = 0
            )

            // Log Call
            callHistoryList = listOf(
                CallLog(name, emoji, System.currentTimeMillis(), !isVideo, false)
            ) + callHistoryList

            // Start simple ticking time coroutine for call duration
            tickCallTime()
        }
    }

    private var tickingJob: kotlinx.coroutines.Job? = null
    private fun tickCallTime() {
        tickingJob?.cancel()
        tickingJob = viewModelScope.launch {
            while (callState is CallState.Active) {
                delay(1000)
                val current = callState
                if (current is CallState.Active) {
                    callState = current.copy(durationSeconds = current.durationSeconds + 1)
                }
            }
        }
    }

    fun endCall() {
        tickingJob?.cancel()
        callState = CallState.Idle
    }
}
