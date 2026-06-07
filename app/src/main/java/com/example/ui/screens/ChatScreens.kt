package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ChatRoom
import com.example.data.Message
import com.example.data.StatusStory
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CallState
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ChatViewModel) {
    val currentScreen = viewModel.currentScreen
    val callState = viewModel.callState
    val activeStory = viewModel.activeViewingStory

    Box(modifier = Modifier.fillMaxSize()) {
        // Render current window state
        when (currentScreen) {
            is AppScreen.Home -> HomeScreen(viewModel)
            is AppScreen.ChatRoomDetail -> ChatDetailScreen(viewModel, currentScreen.roomId)
            is AppScreen.GroupMembersInfo -> GroupInfoScreen(viewModel, currentScreen.roomId)
            is AppScreen.ProfileEdit -> ProfileEditScreen(viewModel)
        }

        // Expanded Calling Screen Overlay
        if (callState is CallState.Active) {
            CallOverlayScreen(callState = callState, onHangUp = { viewModel.endCall() })
        }

        // Immersive Stories Stories overlay
        if (activeStory != null) {
            StoryViewerScreen(story = activeStory, onDismiss = { viewModel.viewStory(null) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ChatViewModel) {
    val rooms by viewModel.rooms.collectAsState()
    val stories by viewModel.stories.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Chats, 1: Status, 2: Calls
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                // Top App Bar
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = viewModel.searchQuery,
                                onValueChange = { viewModel.searchQuery = it },
                                placeholder = { Text("গল্প বা কন্টাক্ট খুঁজুন...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("app_search_field"),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        } else {
                            Text(
                                "গল্প করি (Golpo Kori)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 21.sp
                            )
                        }
                    },
                    actions = {
                        if (isSearching) {
                            IconButton(onClick = {
                                isSearching = false
                                viewModel.searchQuery = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search Chats", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.navigateTo(AppScreen.ProfileEdit) }) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isSearching) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
                    )
                )

                // Classic Custom Tabs Indicator
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color.White,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("গল্প সমূহ", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.7f)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("স্ট্যাটাস", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.7f)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("কলস", fontWeight = FontWeight.Bold, color = if (selectedTab == 2) Color.White else Color.White.copy(alpha = 0.7f)) }
                    )
                }
            }
        },
        bottomBar = {
            val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B141A)
            NavigationBar(
                containerColor = if (isDark) Color(0xFF111B21) else Color.White,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("app_bottom_nav")
            ) {
                // Chats
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Chats"
                        )
                    },
                    label = { Text("গল্প সমূহ", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (isDark) Color.White else Color(0xFF001D35),
                        selectedTextColor = if (isDark) Color.White else Color(0xFF001D35),
                        indicatorColor = if (isDark) Color(0xFF005C4B) else Color(0xFFD2E3FC),
                        unselectedIconColor = if (isDark) Color(0xFF8696A0) else Color(0xFF667781),
                        unselectedTextColor = if (isDark) Color(0xFF8696A0) else Color(0xFF667781)
                    )
                )

                // Status/Updates
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = "Updates"
                        )
                    },
                    label = { Text("স্ট্যাটাস", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (isDark) Color.White else Color(0xFF001D35),
                        selectedTextColor = if (isDark) Color.White else Color(0xFF001D35),
                        indicatorColor = if (isDark) Color(0xFF005C4B) else Color(0xFFD2E3FC),
                        unselectedIconColor = if (isDark) Color(0xFF8696A0) else Color(0xFF667781),
                        unselectedTextColor = if (isDark) Color(0xFF8696A0) else Color(0xFF667781)
                    )
                )

                // Calls
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Calls"
                        )
                    },
                    label = { Text("কলস", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (isDark) Color.White else Color(0xFF001D35),
                        selectedTextColor = if (isDark) Color.White else Color(0xFF001D35),
                        indicatorColor = if (isDark) Color(0xFF005C4B) else Color(0xFFD2E3FC),
                        unselectedIconColor = if (isDark) Color(0xFF8696A0) else Color(0xFF667781),
                        unselectedTextColor = if (isDark) Color(0xFF8696A0) else Color(0xFF667781)
                    )
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    // Start Chat Bot button
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.navigateTo(AppScreen.ChatRoomDetail("chatbot")) },
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Bot") },
                        text = { Text("এআই অ্যাসিস্ট্যান্ট") },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xFF00A884),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("ai_chatbot_fab")
                    )
                }
                1 -> {
                    var showStoryPosterDialog by remember { mutableStateOf(false) }
                    FloatingActionButton(
                        onClick = { showStoryPosterDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xFF00A884),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("add_story_fab")
                    ) {
                        Icon(Icons.Default.Create, contentDescription = "Writing Status")
                    }

                    if (showStoryPosterDialog) {
                        StoryPostDialog(
                            onDismiss = { showStoryPosterDialog = false },
                            onSubmit = { text, bgHex ->
                                viewModel.createStory(text, bgHex)
                                showStoryPosterDialog = false
                            }
                        )
                    }
                }
                2 -> {
                    var showNewCallDialog by remember { mutableStateOf(false) }
                    FloatingActionButton(
                        onClick = { showNewCallDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xFF00A884),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("start_call_fab")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Dial Call")
                    }

                    if (showNewCallDialog) {
                        DialCallPicker(
                            rooms = rooms,
                            onDismiss = { showNewCallDialog = false },
                            onSelectCall = { contactId, video ->
                                showNewCallDialog = false
                                viewModel.startCall(contactId, video)
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> ChatsTabContent(viewModel, rooms, stories)
                1 -> StatusTabContent(viewModel, stories)
                2 -> CallsTabContent(viewModel)
            }
        }
    }
}

// ---------------- Chats Tab ----------------
@Composable
fun ChatsTabContent(
    viewModel: ChatViewModel,
    rooms: List<ChatRoom>,
    stories: List<StatusStory>
) {
    val filteredRooms = remember(rooms, viewModel.searchQuery) {
        if (viewModel.searchQuery.isEmpty()) rooms
        else rooms.filter {
            it.name.contains(viewModel.searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(viewModel.searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Status Row at top of lists
        item {
            StoryThumbnailRow(stories = stories, onStoryClick = { viewModel.viewStory(it) })
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
        }

        // Encryption Banner info
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Safe Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "আপনার ব্যক্তিগত চ্যাটগুলো এন্ড-টু-এন্ড এনক্রিপ্টেড। গল্প করি ব্যতীত বাইরের কেউ এগুলো পড়তে পারে না।🔒",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        if (filteredRooms.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Forum,
                            contentDescription = "Empty Chats",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "কোনো চ্যাট খুঁজে পাওয়া যায়নি!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(filteredRooms) { room ->
                ChatRoomRow(room = room, onClick = {
                    viewModel.navigateTo(AppScreen.ChatRoomDetail(room.id))
                })
                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 72.dp)
                )
            }
        }
    }
}

@Composable
fun StoryThumbnailRow(stories: List<StatusStory>, onStoryClick: (StatusStory) -> Unit) {
    val activeBrandColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "সাম্প্রতিক স্ট্যাটাস আপডেট",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stories) { story ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onStoryClick(story) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .drawBehind {
                                drawCircle(
                                    color = activeBrandColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(30f, 12f),
                                            phase = 0f
                                        )
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(story.backgroundColorHex))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(story.userAvatarEmoji, fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = story.userName,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ChatRoomRow(room: ChatRoom, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B141A)
    val avatarBgColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        when (room.id) {
            "chatbot" -> Color(0xFFE0E7FF) // Indigo 100
            "room_mim" -> Color(0xFFFFE4E6) // Rose 100
            "room_sajib" -> Color(0xFFE0F2FE) // Sky 100
            "room_family" -> Color(0xFFFEF3C7) // Amber 100
            else -> Color(0xFFD1FAE5) // Emerald 100
        }
    }
    val avatarTextColor = if (isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        when (room.id) {
            "chatbot" -> Color(0xFF4F46E5) // Indigo 600
            "room_mim" -> Color(0xFFE11D48) // Rose 600
            "room_sajib" -> Color(0xFF0284C7) // Sky 600
            "room_family" -> Color(0xFFD97706) // Amber 600
            else -> Color(0xFF059669) // Emerald 600
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_room_item_${room.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded Avatar Circle
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(avatarBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(room.avatarEmoji, fontSize = 28.sp)
            if (room.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF25D366), CircleShape)
                        .drawBehind {
                            drawCircle(Color.White, style = Stroke(width = 1.5.dp.toPx()))
                        }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name, Status and Last Message text
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestampShort(room.lastMsgTimestamp),
                    fontSize = 11.sp,
                    fontWeight = if (room.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (room.unreadCount > 0) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (room.typingStatus.isNotEmpty()) {
                    Text(
                        text = "typing...",
                        color = Color(0xFF25D366),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkmarks for feedback
                        if (room.id != "chatbot" && room.unreadCount == 0) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = if (room.id == "room_sajib") Color(0xFF53BDEB) else Color.LightGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        } else if (room.id != "chatbot") {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Sent",
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }

                        Text(
                            text = room.lastMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (room.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .background(Color(0xFF25D366), CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Status Tab ----------------
@Composable
fun StatusTabContent(viewModel: ChatViewModel, stories: List<StatusStory>) {
    val currentProfile by viewModel.userProfile.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentProfile?.avatarEmoji ?: "🇧🇩", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "আমার স্ট্যাটাস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "স্ট্যাটাস সেট করতে নিচের পেন বাটনে ক্লিক করুন",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "সর্বশেষ আপডেট সমূূহ",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (stories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("কোনো স্ট্যাটাস আপডেট নেই!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(stories) { story ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.viewStory(story) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(story.backgroundColorHex))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(story.userAvatarEmoji, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            story.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            formatTimestampShort(story.timestamp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 82.dp)
                )
            }
        }
    }
}

// ---------------- Calls Tab ----------------
@Composable
fun CallsTabContent(viewModel: ChatViewModel) {
    val callList = viewModel.callHistoryList

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "সাম্প্রতিক কলসমূহ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (callList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("কোনো পূর্ববর্তী কল হিস্ট্রি নেই!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(callList) { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(log.avatar, fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                log.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (log.missed) Icons.Default.CallMissed else Icons.Default.CallMade,
                                    contentDescription = "Direction",
                                    tint = if (log.missed) Color.Red else Color(0xFF25D366),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    formatTimestampShort(log.timestamp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.startCall(log.name, !log.isVoice) }) {
                        Icon(
                            imageVector = if (log.isVoice) Icons.Default.Call else Icons.Default.Videocam,
                            contentDescription = "Redial",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 76.dp)
                )
            }
        }
    }
}

// ---------------- Dialogs & Pickers ----------------

@Composable
fun StoryPostDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var storyText by remember { mutableStateOf("") }
    val colors = listOf("#128C7E", "#075E54", "#3F51B5", "#E91E63", "#9C27B0", "#00BCD4", "#FF5722")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "নতুন স্ট্যাটাস লিখুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Write area with selected color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(android.graphics.Color.parseColor(selectedColor))),
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        placeholder = { Text("এখানে আপনার মনের কথা লিখুন...", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center) },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.fillMaxWidth().testTag("story_input_text"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Background Color Picker
                Text("ব্যাকগ্রাউন্ড কালার নির্বাচন করুন:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colors) { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedColor = hex }
                                .drawBehind {
                                    if (selectedColor == hex) {
                                        drawCircle(Color.White, style = Stroke(width = 2.dp.toPx()))
                                    }
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (storyText.trim().isNotEmpty()) {
                                onSubmit(storyText, selectedColor)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("submit_story_btn")
                    ) {
                        Text("পোস্ট করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun DialCallPicker(
    rooms: List<ChatRoom>,
    onDismiss: () -> Unit,
    onSelectCall: (String, Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "যাকে কল করতে চান নির্বাচন করুন:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(rooms) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* do nothing direct click handled */ }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(contact.avatarEmoji, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                                Text(contact.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Row {
                                IconButton(onClick = { onSelectCall(contact.id, false) }) {
                                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onSelectCall(contact.id, true) }) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("বাতিল")
                }
            }
        }
    }
}

// ---------------- Story Immersive Viewer ----------------
@Composable
fun StoryViewerScreen(story: StatusStory, onDismiss: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(story) {
        // Auto progress status slider over 4 seconds
        val steps = 100
        for (i in 1..steps) {
            delay(40)
            progress = i / steps.toFloat()
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
    ) {
        // Custom background container representing story
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(android.graphics.Color.parseColor(story.backgroundColorHex))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(story.userAvatarEmoji, fontSize = 72.sp, modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = story.caption,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )
            }
        }

        // Top Indicators and header info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Slider Progress indicator bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.35f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(story.userAvatarEmoji, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(story.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("অনলাইন গল্প • " + formatTimestampShort(story.timestamp), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }

        // Bottom Dismiss prompt label
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Text(
                "বন্ধ করতে যেকোনো জায়গায় স্পর্শ করুন",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

// ---------------- Call Immersive Simulator ----------------
@Composable
fun CallOverlayScreen(callState: CallState.Active, onHangUp: () -> Unit) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }
    var isCamOn by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF075E54)) // Green dialer theme
    ) {
        // If it is a video call, draw dynamic modern matrix design in background
        if (callState.isVideo && isCamOn) {
            // Draw radial pulsing rings or simple modern gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Drawing modern circle nodes to resemble high-fidelity WebRTC frames
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = size.minDimension / 1.5f,
                            center = Offset(size.width / 2, size.height / 3)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.03f),
                            radius = size.minDimension / 1.1f,
                            center = Offset(size.width / 2, size.height / 3)
                        )
                    }
            ) {
                // Top Mini simulated WebRTC view
                Box(
                    modifier = Modifier
                        .padding(top = 150.dp, end = 24.dp)
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                        .align(Alignment.TopEnd)
                ) {
                    Text("আমি (Self)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(6.dp).align(Alignment.BottomStart))
                    Icon(Icons.Default.Person, contentDescription = "", tint = Color.LightGray, modifier = Modifier.size(36.dp).align(Alignment.Center))
                }
            }
        }

        // Immersive Caller Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header information: Brand title and E2E shield note
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = "", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("এন্ড-টু-এন্ড এনক্রিপ্টেড কল", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar and calling states
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(callState.avatarEmoji, fontSize = 56.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    callState.contactName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (callState.isVideo) "ভিডিও কলিং..." else "ভয়েস কলিং...",
                    color = Color(0xFF25D366),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Timer ticking duration
                Text(
                    text = formatCallDuration(callState.durationSeconds),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Controls actions
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Feature keys row: Mute speaker and Camera Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Black else Color.White
                        )
                    }

                    IconButton(
                        onClick = { isSpeaker = !isSpeaker },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaker) Color.White else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker",
                            tint = if (isSpeaker) Color.Black else Color.White
                        )
                    }

                    if (callState.isVideo) {
                        IconButton(
                            onClick = { isCamOn = !isCamOn },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (!isCamOn) Color.White else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isCamOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Cam Toggle",
                                tint = if (!isCamOn) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Red Hangup button
                IconButton(
                    onClick = onHangUp,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .testTag("hangup_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hang Up",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}


// ---------------- Chat Detail Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: ChatViewModel, roomId: String) {
    val rooms by viewModel.rooms.collectAsState()
    val activeRoom = remember(rooms, roomId) { rooms.find { it.id == roomId } }
    val messages by viewModel.activeRoomMessages.collectAsState()
    val scope = rememberCoroutineScope()

    var textInput by remember { mutableStateOf("") }
    var activeReactionMessageId by remember { mutableStateOf<Long?>(null) }
    var showAttachments by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to bottom on load or new message
    LaunchedEffect(messages.size, activeRoom?.typingStatus) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (activeRoom?.isGroup == true) {
                                    viewModel.navigateTo(AppScreen.GroupMembersInfo(roomId))
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(activeRoom?.avatarEmoji ?: "👤", fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Room details and Online/Typing statuses
                        Column {
                            Text(
                                activeRoom?.name ?: "গল্প চ্যাট",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (activeRoom?.typingStatus?.isNotEmpty() == true) {
                                    activeRoom.typingStatus
                                } else if (activeRoom?.isOnline == true) {
                                    "অনলাইন"
                                } else {
                                    "যেকোনো সময় গল্প করুন"
                                },
                                fontSize = 11.sp,
                                color = if (activeRoom?.typingStatus?.isNotEmpty() == true) Color(0xFF25D366) else Color.White.copy(alpha = 0.82f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startCall(roomId, false) }) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.startCall(roomId, true) }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                    }
                    if (activeRoom?.isGroup == true) {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.GroupMembersInfo(roomId)) }) {
                            Icon(Icons.Default.Info, contentDescription = "Group Info", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    if (viewModel.currentThemeIsDark) Color(0xFF0B141A) else Color(0xFFE5DDD5) // Signature beige wallpaper background mockup
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        // Lock E2E system message at top
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2).copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "", tint = Color(0xFFE65100), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "এই চ্যাটের বার্তাগুলো এন্ড-টু-এন্ড এনক্রিপ্টেড। কেউ আড়ি পাততে পারবে না।",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE65100),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            onMessageTap = {
                                activeReactionMessageId = if (activeReactionMessageId == message.messageId) null else message.messageId
                            }
                        )
                    }

                    // Bottom typing bubble indicator of partner
                    if (activeRoom?.typingStatus?.isNotEmpty() == true) {
                        item {
                            PartnerTypingBubble(avatar = activeRoom.avatarEmoji)
                        }
                    }
                }

                // Floating Reactions Picker if open
                if (activeReactionMessageId != null) {
                    EmojiReactionPanel(
                        onSelectEmoji = { emoji ->
                            viewModel.reactToMessage(activeReactionMessageId!!, emoji)
                            activeReactionMessageId = null
                        },
                        onDismiss = { activeReactionMessageId = null }
                    )
                }

                // Floating picker menu slider for media sharing
                if (showAttachments) {
                    AttachmentPickerSliders(
                        onSelectType = { type ->
                            showAttachments = false
                            viewModel.sendMessage(
                                roomId = roomId,
                                content = when (type) {
                                    "IMAGE" -> "📷 ছবি পাঠানো হয়েছে"
                                    "VOICE" -> "🎤 ভয়েস নোট"
                                    else -> "📎 ফাইল সংযুক্ত"
                                },
                                type = type,
                                mediaUri = "simulated_uri_source",
                                voiceDuration = if (type == "VOICE") 18 else 0
                            )
                        }
                    )
                }

                // Bottom Input Shelf Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat Input Bubble Text Panel
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Smiley face button */ }) {
                            Icon(Icons.Default.Mood, contentDescription = "Emoji picker", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        TextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("বার্তা লিখুন...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_box"),
                            singleLine = false,
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        // Attachment Picker key
                        IconButton(onClick = { showAttachments = !showAttachments }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Green Microphone or Send trigger circle button
                    FloatingActionButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                viewModel.sendMessage(roomId, textInput)
                                textInput = ""
                            } else {
                                // Simulate recorded voice message sending
                                viewModel.sendMessage(roomId, "🎤 ভয়েস নোট", "VOICE", "simulated_voice_uri", 15)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = if (textInput.trim().isNotEmpty()) Icons.Default.Send else Icons.Default.Mic,
                            contentDescription = "Send/Record"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, onMessageTap: () -> Unit) {
    val isMe = message.senderId == "me"
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMessageTap)
            .testTag("message_bubble_${message.messageId}"),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isMe) 12.dp else 0.dp,
                    bottomEnd = if (isMe) 0.dp else 12.dp
                ),
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    // Sender Name if in Family Group
                    if (!isMe && message.roomId != "chatbot") {
                        Text(
                            text = message.senderName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    // Message Types formatting (IMAGE, VOICE, TEXT)
                    when (message.type) {
                        "IMAGE" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "", modifier = Modifier.size(40.dp), tint = Color.Gray)
                                Text("ছবি সংযুক্ত", fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp), color = Color.DarkGray)
                            }
                        }
                        "VOICE" -> {
                            VoiceNotePlayable(message.voiceDuration, isMe)
                        }
                        else -> {
                            Text(
                                text = message.content,
                                color = textColor,
                                fontSize = 14.5.sp,
                                style = LocalTextStyle.current.copy(lineHeight = 20.sp)
                            )
                        }
                    }

                    // Status double check verification, reactions, and Timestamp list row
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestampTime(message.timestamp),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            // Status Double gray and blue checkmarks
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read status",
                                tint = if (message.isRead) Color(0xFF34B7F1) else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Embedded Reaction render capsules below bubble
            if (message.reactions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset(y = (-6).dp, x = if (isMe) (-8).dp else 8.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .drawBehind {
                            drawRoundRect(
                                color = Color.Gray.copy(alpha = 0.2f),
                                style = Stroke(width = 1.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(message.reactions, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun VoiceNotePlayable(durationSeconds: Int, isMe: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val steps = durationSeconds * 10
            for (i in 1..steps) {
                if (!isPlaying) break
                delay(100)
                currentProgress = i / steps.toFloat()
            }
            isPlaying = false
            currentProgress = 0f
        }
    }

    Row(
        modifier = Modifier
            .width(230.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Voice playing controls",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Simulated Sound Visualizer Waves
        Box(
            modifier = Modifier
                .weight(1f)
                .height(30.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val waveCount = 20
                val barWidth = size.width / (waveCount * 2)
                for (i in 0 until waveCount) {
                    val waveHeight = (10..30).random().toDp().toPx()
                    val barX = i * (barWidth * 2)
                    val barColor = if (i / waveCount.toFloat() <= currentProgress) {
                        Color(0xFF25D366) // active green
                    } else {
                        Color.Gray.copy(alpha = 0.4f)
                    }

                    drawRect(
                        color = barColor,
                        topLeft = Offset(barX, (size.height - waveHeight) / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth, waveHeight)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "0:${String.format(Locale.US, "%02d", ((1f - currentProgress) * durationSeconds).toInt())}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PartnerTypingBubble(avatar: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("গল্প লিখছে ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Classic loading dot anim
                val infiniteTransition = rememberInfiniteTransition()
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Text("...", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.drawBehind { })
            }
        }
    }
}

@Composable
fun EmojiReactionPanel(onSelectEmoji: (String) -> Unit, onDismiss: () -> Unit) {
    val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🇧🇩")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                emojis.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable { onSelectEmoji(emoji) }
                            .animateContentSize()
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentPickerSliders(onSelectType: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelectType("IMAGE") }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF9C27B0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = "", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("গ্যালারি", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelectType("IMAGE") }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE91E63), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("ক্যামেরা", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelectType("VOICE") }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFF9800), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = "", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("অডিও", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


// ---------------- Group Information Profile View ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(viewModel: ChatViewModel, roomId: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("গ্রুপ ইনফরমেশন", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.ChatRoomDetail(roomId)) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("👨‍👩‍👧‍👦", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("আমরা ক’জন (Family)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("গ্রুপ আইডি • 👨‍👩‍👧‍👦-Room-Key", color = Color.Gray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("গ্রুপ বর্ণনা:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("আমাদের পারিবারিক আড্ডাঘর ও খোশগল্প। সবাই সব খবর এখানেই দেবেন। ❤️", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "গ্রুপ মেম্বার তালিকা (৪ জন)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val members = listOf(
                        Pair("আম্মু (Mom)", "👵"),
                        Pair("আমি (You)", "🇧🇩"),
                        Pair("ছোট ভাই", "🧒"),
                        Pair("সজীব বন্ধু", "🏎️")
                    )

                    members.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(member.second, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                            Text(member.first, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}


// ---------------- Profile settings Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(viewModel: ChatViewModel) {
    val currentProfile by viewModel.userProfile.collectAsState()

    var userNameInput by remember { mutableStateOf("") }
    var userPhoneInput by remember { mutableStateOf("") }
    var userStatusInput by remember { mutableStateOf("") }
    val avatarOptions = listOf("🇧🇩", "🤖", "🚀", "🏎️", "🙋‍♀️", "👤")
    var selectedAvatar by remember { mutableStateOf("🇧🇩") }

    // Init inputs with current states
    LaunchedEffect(currentProfile) {
        currentProfile?.let {
            userNameInput = it.name
            userPhoneInput = it.phone
            userStatusInput = it.statusText
            selectedAvatar = it.avatarEmoji
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রোফাইল ও সেটিংস", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedAvatar, fontSize = 56.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("ইমোজি প্রোফাইল ছবি নির্বাচন করুন:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(avatarOptions) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedAvatar == emoji) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.15f))
                                .clickable { selectedAvatar = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = { userNameInput = it },
                    label = { Text("নাম") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
            }

            item {
                OutlinedTextField(
                    value = userPhoneInput,
                    onValueChange = { userPhoneInput = it },
                    label = { Text("ফোন নম্বর") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                )
            }

            item {
                OutlinedTextField(
                    value = userStatusInput,
                    onValueChange = { userStatusInput = it },
                    label = { Text("স্ট্যাটাস বাণী") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_status_input")
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("সেটিংস ও নিরাপত্তা:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Theme switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ডার্ক মোড সক্রিয় করুন")
                            Switch(
                                checked = viewModel.currentThemeIsDark,
                                onCheckedChange = { viewModel.toggleDarkMode() },
                                modifier = Modifier.testTag("dark_mode_switch")
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Encrypt details
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Security, contentDescription = "", tint = Color(0xFF25D366))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("সুরক্ষা কি আইডি (GK-E2E)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(currentProfile?.encryptionKeyId ?: "GK-E2E-UNSPECIFIED", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.updateProfile(userNameInput, userPhoneInput, userStatusInput, selectedAvatar)
                        viewModel.navigateTo(AppScreen.Home)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_save_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("সংরক্ষণ করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}


// --- Helper Functions to Format Status Times & Call States ---

fun formatTimestampShort(timeMs: Long): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date(timeMs))
}

fun formatTimestampTime(timeMs: Long): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date(timeMs))
}

fun formatCallDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
