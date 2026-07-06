package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.rotate
import com.example.R
import com.example.data.MatchRecord
import java.text.SimpleDateFormat
import java.util.*

// Farsi digits converter
fun String.toPersianDigits(): String {
    val persianDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return this.map { char ->
        if (char.isDigit()) persianDigits[char.toString().toInt()] else char
    }.joinToString("")
}

fun Int.toPersianDigits(): String {
    return this.toString().toPersianDigits()
}

fun Long.toPersianDateTime(): String {
    val date = Date(this)
    val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
    val formatted = sdf.format(date)
    val persianDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return formatted.map { char ->
        if (char.isDigit()) persianDigits[char.toString().toInt()] else char
    }.joinToString("")
}

// Avatar helper utilities
fun getAvatarEmoji(id: Int): String {
    return when (id) {
        1 -> "🕵️"
        2 -> "🦊"
        3 -> "🦁"
        4 -> "🦉"
        5 -> "🦅"
        6 -> "🐺"
        else -> "👤"
    }
}

fun getAvatarColor(id: Int): Color {
    return when (id) {
        1 -> Color(0xFF1E88E5) // Blue
        2 -> Color(0xFFE53935) // Red
        3 -> Color(0xFFFFB300) // Gold
        4 -> Color(0xFF43A047) // Green
        5 -> Color(0xFF8E24AA) // Purple
        6 -> Color(0xFF00ACC1) // Teal
        else -> Color(0xFF90A4AE) // Slate
    }
}

// Theme colors
val MafiaBg = Color(0xFF090A0E)
val MafiaCardBg = Color(0xFF13151F)
val MafiaBorder = Color(0xFF222636)
val MafiaRed = Color(0xFFE53935)
val MafiaGold = Color(0xFFFFB300)
val MafiaBlue = Color(0xFF1E88E5)
val MafiaGreen = Color(0xFF43A047)
val MafiaTextIce = Color(0xFFECEFF1)
val MafiaTextMuted = Color(0xFF90A4AE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MafiaScreen(
    viewModel: MafiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaBg),
        containerColor = MafiaBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MafiaRed,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "بازی مافیای سرخ (هوشمند)",
                            color = MafiaTextIce,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.testTag("app_title")
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MafiaBg,
                    titleContentColor = MafiaTextIce
                ),
                actions = {
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "تاریخچه بازی‌ها",
                            tint = MafiaRed
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val isTablet = maxWidth > 600.dp
            val layoutModifier = if (isTablet) {
                Modifier
                    .widthIn(max = 600.dp)
                    .align(Alignment.TopCenter)
            } else {
                Modifier.fillMaxSize()
            }

            Crossfade(
                targetState = state.gamePhase,
                animationSpec = tween(500),
                label = "phase_crossfade"
            ) { phase ->
                when (phase) {
                    GamePhase.LOBBY -> {
                        if (state.activeRoomId != null) {
                            ActiveRoomLobbyView(
                                state = state,
                                onAccept = { viewModel.acceptJoinRequest(it) },
                                onReject = { viewModel.rejectJoinRequest(it) },
                                onStartGame = { viewModel.startNewGameFromLobby() },
                                onSendChatMessage = { viewModel.sendUserChatMessage(it) },
                                onLeave = { viewModel.leaveRoom() },
                                modifier = layoutModifier
                            )
                        } else {
                            LobbyMainView(
                                state = state,
                                onUpdateProfile = { nickname, avatar, bio ->
                                    viewModel.updateProfile(nickname, avatar, bio)
                                },
                                onCreateRoom = { viewModel.createRoom() },
                                onJoinRoom = { viewModel.joinRoom(it) },
                                onToggleOnlineMode = { viewModel.toggleOnlineMode(it) },
                                modifier = layoutModifier
                            )
                        }
                    }
                    GamePhase.ROLE_REVEAL -> RoleRevealView(
                        state = state,
                        onProceed = { viewModel.proceedFromRoleRevealToDay() },
                        modifier = layoutModifier
                    )
                    GamePhase.NIGHT_MAFIA -> NightActionView(
                        state = state,
                        title = "فاز شب: شلیک مافیا 💀",
                        instruction = "به عنوان مافیا، یکی از شهروندان زنده را برای شلیک دیشب انتخاب کنید:",
                        icon = Icons.Default.Adjust,
                        accentColor = MafiaRed,
                        onSelectPlayer = { viewModel.performUserMafiaAction(it) },
                        modifier = layoutModifier
                    )
                    GamePhase.NIGHT_DETECTIVE -> NightActionView(
                        state = state,
                        title = "فاز شب: استعلام کارآگاه 🔍",
                        instruction = "یک بازیکن زنده را برای گرفتن استعلام نقش انتخاب کنید:",
                        icon = Icons.Default.Search,
                        accentColor = MafiaBlue,
                        onSelectPlayer = { viewModel.performUserDetectiveAction(it) },
                        modifier = layoutModifier
                    )
                    GamePhase.NIGHT_DOCTOR -> NightActionView(
                        state = state,
                        title = "فاز شب: نجات پزشک 🩺",
                        instruction = "یک بازیکن زنده (از جمله خودتان) را برای مداوا و نجات انتخاب کنید:",
                        icon = Icons.Default.Healing,
                        accentColor = MafiaGreen,
                        onSelectPlayer = { viewModel.performUserDoctorAction(it) },
                        modifier = layoutModifier
                    )
                    GamePhase.DAY_SUMMARY -> DaySummaryView(
                        state = state,
                        onProceed = { viewModel.startSpeakingPhase() },
                        modifier = layoutModifier
                    )
                    GamePhase.DAY_SPEAKING -> DaySpeakingView(
                        state = state,
                        onPassTurn = { viewModel.passSpeakingTurn() },
                        onSendReaction = { viewModel.sendReactionFromUser(it) },
                        onToggleUserMic = { viewModel.setUserMicEnabled(it) },
                        modifier = layoutModifier
                    )
                    GamePhase.DAY_VOTING -> DayVotingView(
                        state = state,
                        onCastVote = { viewModel.castUserVote(it) },
                        onProceed = { viewModel.skipVotingTimer() },
                        modifier = layoutModifier
                    )
                    GamePhase.DAY_TRIAL -> DayTrialView(
                        state = state,
                        onPassCourtSpeaking = { viewModel.passCourtSpeakingTurn() },
                        onCastCourtVote = { courtPlayerId, shouldEliminate -> viewModel.castUserCourtVote(courtPlayerId, shouldEliminate) },
                        onConfirmCourtVotes = { viewModel.resolveCourtVoting() },
                        modifier = layoutModifier
                    )
                    GamePhase.GAME_OVER -> GameOverView(
                        state = state,
                        onPlayAgain = { viewModel.restartGame() },
                        modifier = layoutModifier
                    )
                }
            }
        }
    }

    if (showHistoryDialog) {
        HistoryDialog(
            history = state.matchHistory,
            onDismiss = { showHistoryDialog = false },
            onClearHistory = { viewModel.clearHistory() }
        )
    }
}

// 1. Tabbed Lobby + Profile setup + Room directory
@Composable
fun LobbyMainView(
    state: MafiaUiState,
    onUpdateProfile: (String, Int, String) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: (GameRoom) -> Unit,
    onToggleOnlineMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("اتاق‌های بازی 🏢", "پروفایل من 🕵️")

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MafiaBorder)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_mafia_banner_1783283040901),
                    contentDescription = "بنر مافیای سرخ",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "مافیای سرخ",
                        color = MafiaRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "کلوب شبیه‌سازی آنلاین بازی مافیا با راوی پیشرفته هوش مصنوعی",
                        color = MafiaTextIce,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MafiaBg,
            contentColor = MafiaRed,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MafiaRed
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) MafiaRed else MafiaTextMuted
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> RoomsDirectoryTab(
                state = state,
                onCreateRoom = onCreateRoom,
                onJoinRoom = onJoinRoom,
                onToggleOnlineMode = onToggleOnlineMode
            )
            1 -> ProfileSettingsTab(
                state = state,
                onUpdateProfile = onUpdateProfile
            )
        }
    }
}

// Sub-Tab 1: Rooms Directory
@Composable
fun RoomsDirectoryTab(
    state: MafiaUiState,
    onCreateRoom: () -> Unit,
    onJoinRoom: (GameRoom) -> Unit,
    onToggleOnlineMode: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Firebase Online Mode Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isOnlineMode) MafiaCardBg else Color(0x1F2A2222)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (state.isOnlineMode) MafiaRed.copy(alpha = 0.5f) else MafiaBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حالت چندنفره آنلاین تجاری (Firebase)",
                            color = if (state.isOnlineMode) MafiaRed else MafiaTextIce,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (state.isOnlineMode) "متصل به پایگاه داده واقعی فایربیس 🌐" else "در حال استفاده از شبیه‌ساز آفلاین بازیکنان",
                            color = MafiaTextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = state.isOnlineMode,
                        onCheckedChange = onToggleOnlineMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MafiaRed,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                if (state.firebaseError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x3DFF3B30)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = state.firebaseError,
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "راهنمای راه‌اندازی تجاری فایربیس:",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "۱. فایل تنظیمات google-services.json را دریافت و در پوشه app پروژه قرار دهید.\n۲. پلاگین فایربیس را به گریدل اضافه کنید.\n۳. سرویس Realtime Database فایربیس را فعال نموده و قوانین امنیتی آن را برای خواندن/نوشتن باز بگذارید.",
                                color = MafiaTextMuted,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "اتاق‌های آنلاین فعال",
                color = MafiaTextIce,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onCreateRoom,
                colors = ButtonDefaults.buttonColors(containerColor = MafiaRed),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ایجاد اتاق جدید", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.availableRooms) { room ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MafiaBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = room.name,
                                color = MafiaTextIce,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "میزبان: ${room.hostName}",
                                    color = MafiaTextMuted,
                                    fontSize = 11.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(MafiaTextMuted)
                                )
                                Text(
                                    text = "${room.currentPlayers.toPersianDigits()}/${room.maxPlayers.toPersianDigits()} بازیکن",
                                    color = MafiaGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { onJoinRoom(room) },
                            enabled = room.status != "در حال بازی",
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MafiaGreen,
                                disabledContainerColor = MafiaBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (room.status == "در حال بازی") "در حال بازی" else "ورود",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sub-Tab 2: Profile Settings
@Composable
fun ProfileSettingsTab(
    state: MafiaUiState,
    onUpdateProfile: (String, Int, String) -> Unit
) {
    var nickname by remember { mutableStateOf(state.userNickname) }
    var bio by remember { mutableStateOf(state.profileBio) }
    var selectedAvatar by remember { mutableStateOf(state.profileAvatarId) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MafiaBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "تصویر آواتار شما",
                        color = MafiaTextIce,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Avatar Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..6).forEach { id ->
                            val isSelected = selectedAvatar == id
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) getAvatarColor(id).copy(alpha = 0.2f)
                                        else MafiaBorder
                                    )
                                    .border(
                                        2.dp,
                                        if (isSelected) getAvatarColor(id) else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedAvatar = id },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getAvatarEmoji(id),
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("نام مستعار شما") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nickname_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MafiaRed,
                            unfocusedBorderColor = MafiaBorder,
                            focusedTextColor = MafiaTextIce,
                            unfocusedTextColor = MafiaTextIce,
                            focusedLabelColor = MafiaRed,
                            unfocusedLabelColor = MafiaTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("بیوگرافی یا شعار شما") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MafiaRed,
                            unfocusedBorderColor = MafiaBorder,
                            focusedTextColor = MafiaTextIce,
                            unfocusedTextColor = MafiaTextIce,
                            focusedLabelColor = MafiaRed,
                            unfocusedLabelColor = MafiaTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { onUpdateProfile(nickname, selectedAvatar, bio) },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره و بروزرسانی پروفایل 💾", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// 2. Inside Active Room Lobby Screen (Handles up to 12 players)
@Composable
fun ActiveRoomLobbyView(
    state: MafiaUiState,
    onAccept: (MafiaPlayer) -> Unit,
    onReject: (String) -> Unit,
    onStartGame: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var activeLobbyTab by remember { mutableStateOf(0) } // 0 = Chat, 1 = Requests

    // Auto-scroll chat of the room
    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Room header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MafiaBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = state.activeRoomName,
                        color = MafiaTextIce,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "کد امنیتی اتاق: ${state.roomCode.toPersianDigits()}",
                        color = MafiaGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onLeave,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MafiaRed.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "خروج", tint = MafiaRed)
                }
            }
        }

        // Current Players in the lobby (Grid layout, up to 12 slots)
        Text(
            text = "بازیکنان حاضر در لابی (${state.activeRoomPlayers.size.toPersianDigits()}/۱۲)",
            color = MafiaTextIce,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MafiaBorder)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.activeRoomPlayers) { player ->
                    val isHost = player.isUser && state.isHost || (player.id == "host_bot")
                    Box(
                        modifier = Modifier
                            .height(84.dp)
                    ) {
                        // 3D Shadow Layer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(x = 2.dp, y = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (player.isUser) MafiaRed.copy(alpha = 0.2f)
                                    else Color.Black.copy(alpha = 0.45f)
                                )
                        )

                        // Card Front Layer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = if (player.isUser) {
                                            listOf(Color(0xFF2E1B1B), Color(0xFF140D0D))
                                        } else {
                                            listOf(Color(0xFF1B1E2E), Color(0xFF0F111B))
                                        }
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (player.isUser) MafiaRed else MafiaBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(getAvatarColor(player.avatarId).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(getAvatarEmoji(player.avatarId), fontSize = 16.sp)
                                }

                                Text(
                                    text = player.name + if (player.isUser) " (شما)" else "",
                                    color = MafiaTextIce,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isHost) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MafiaGold.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("میزبان", color = MafiaGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Empty slots simulation
                val emptySlotsCount = 12 - state.activeRoomPlayers.size
                if (emptySlotsCount > 0) {
                    items(emptySlotsCount) {
                        Box(
                            modifier = Modifier
                                .height(84.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 1.dp, y = 1.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Transparent)
                                    .border(1.dp, MafiaBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("خالی 👤", color = MafiaTextMuted.copy(alpha = 0.3f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Panel: Chat and/or Requests (Unified card layout)
        val showRequestsTab = state.isHost && activeLobbyTab == 1

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f),
            colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MafiaBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // If Host, show tab headers to switch between Chat and Requests
                if (state.isHost) {
                    TabRow(
                        selectedTabIndex = activeLobbyTab,
                        containerColor = Color.Transparent,
                        contentColor = MafiaGold,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeLobbyTab]),
                                color = MafiaGold
                            )
                        }
                    ) {
                        Tab(
                            selected = activeLobbyTab == 0,
                            onClick = { activeLobbyTab = 0 },
                            text = { Text("گپ و گفت اتاق 💬", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeLobbyTab == 1,
                            onClick = { activeLobbyTab = 1 },
                            text = { 
                                Text(
                                    text = "درخواست‌ها 👥 (${state.joinRequests.size.toPersianDigits()})", 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.joinRequests.isNotEmpty()) MafiaRed else MafiaTextIce
                                ) 
                            }
                        )
                    }
                }

                if (showRequestsTab) {
                    // Show Requests list
                    if (state.joinRequests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "در انتظار ارسال درخواست بازیکنان جدید...",
                                color = MafiaTextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.joinRequests) { request ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MafiaBg)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(getAvatarColor(request.avatarId).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(getAvatarEmoji(request.avatarId), fontSize = 13.sp)
                                        }
                                        Text(
                                            text = request.name,
                                            color = MafiaTextIce,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { onAccept(request) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MafiaGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("پذیرش", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { onReject(request.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MafiaRed),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("رد", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Show Chat panel for Guest or Host
                    var typedText by remember { mutableStateOf("") }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Chat List
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.chatMessages) { msg ->
                                if (msg.isSystem) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MafiaBorder.copy(alpha = 0.3f))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = MafiaGold,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    // Player message
                                    val isMe = msg.isUser || (msg.senderName == state.userNickname)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isMe) Arrangement.Start else Arrangement.End,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        if (isMe) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(getAvatarColor(state.profileAvatarId).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(getAvatarEmoji(state.profileAvatarId), fontSize = 11.sp)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Column(
                                            horizontalAlignment = if (isMe) Alignment.Start else Alignment.End
                                        ) {
                                            Text(
                                                text = msg.senderName,
                                                color = if (isMe) MafiaGold else MafiaTextMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStart = if (isMe) 0.dp else 12.dp,
                                                            topEnd = if (isMe) 12.dp else 0.dp,
                                                            bottomStart = 12.dp,
                                                            bottomEnd = 12.dp
                                                        )
                                                    )
                                                    .background(if (isMe) MafiaBorder.copy(alpha = 0.4f) else MafiaBg)
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    color = MafiaTextIce,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        if (!isMe) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val senderPlayer = state.activeRoomPlayers.find { it.name == msg.senderName }
                                            val senderAvatarId = senderPlayer?.avatarId ?: 1
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(getAvatarColor(senderAvatarId).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(getAvatarEmoji(senderAvatarId), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Text Field Input Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MafiaBg)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = typedText,
                                onValueChange = { typedText = it },
                                placeholder = { Text("پیامی بنویسید...", fontSize = 11.sp, color = MafiaTextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                textStyle = TextStyle(fontSize = 12.sp, color = MafiaTextIce),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MafiaGold,
                                    unfocusedBorderColor = MafiaBorder,
                                    focusedContainerColor = MafiaCardBg,
                                    unfocusedContainerColor = MafiaCardBg
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (typedText.isNotBlank()) {
                                        onSendChatMessage(typedText)
                                        typedText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MafiaGold)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "ارسال",
                                    tint = MafiaBg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Start button for BOTH host and guest (with different labels)
        val canStart = state.activeRoomPlayers.size >= 4
        Button(
            onClick = onStartGame,
            enabled = canStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isHost) MafiaRed else MafiaGold,
                disabledContainerColor = MafiaBorder
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("start_game_button")
        ) {
            Text(
                text = if (state.activeRoomPlayers.size < 12) {
                    if (state.isHost) "در انتظار تکمیل ظرفیت (${state.activeRoomPlayers.size.toPersianDigits()}/۱۲)" 
                    else "در انتظار تکمیل ظرفیت (${state.activeRoomPlayers.size.toPersianDigits()}/۱۲)"
                } else {
                    if (state.isHost) "شروع رسمی بازی مافیای سرخ 🚀" 
                    else "اعلام آمادگی به راوی و شروع بازی 📢"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (state.isHost) Color.White else MafiaBg
            )
        }
    }
}

// 3. Role Reveal Layout
@Composable
fun RoleRevealView(
    state: MafiaUiState,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableStateOf(10) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            secondsLeft--
        }
        onProceed()
    }

    val cardRotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(600),
        label = "card_flip"
    )

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تخصیص مخفیانه نقش",
                color = MafiaTextIce,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Timer Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MafiaRed.copy(alpha = 0.15f))
                    .border(1.dp, MafiaRed, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ورود خودکار: ${secondsLeft.toPersianDigits()} ثانیه",
                    color = MafiaGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { revealed = !revealed }
                .testTag("role_card_clickable")
        ) {
            if (cardRotation < 90f) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, MafiaRed)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MafiaRed,
                            modifier = Modifier
                                .size(64.dp)
                                .scale(1.1f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جهت افشای نقش مخفی لمس کنید",
                            color = MafiaTextIce,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "اطمینان حاصل کنید کسی گوشی شما را نمی‌بیند",
                            color = MafiaTextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val role = state.currentRole
                val factionColor = if (role.faction == Faction.MAFIA) MafiaRed else MafiaBlue
                val backgroundRes = when (role) {
                    Role.GODFATHER -> R.drawable.img_role_godfather_1783360445952
                    Role.MAFIA, Role.LEKTOR -> R.drawable.img_role_mafia_1783360461941
                    Role.DETECTIVE -> R.drawable.img_role_detective_1783360490119
                    Role.DOCTOR -> R.drawable.img_role_doctor_1783360477419
                    Role.SNIPER -> R.drawable.img_role_sniper_1783360501568
                    else -> R.drawable.img_role_citizen_1783360513077
                }

                // Beautiful custom decorative frame exactly matching the requested playing card style
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .background(Color(0xFF070707), RoundedCornerShape(24.dp))
                        .border(BorderStroke(4.dp, factionColor), RoundedCornerShape(24.dp))
                        .padding(8.dp)
                ) {
                    // Thin inner ornamental line with notches in the 4 corners
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 1.5.dp.toPx()
                        val inset = 14.dp.toPx()
                        val size = this.size
                        val color = factionColor.copy(alpha = 0.8f)

                        // Path for inner line with decorative clipped corners
                        val path = Path().apply {
                            moveTo(inset, strokeWidth)
                            lineTo(size.width - inset, strokeWidth)
                            lineTo(size.width - strokeWidth, inset)
                            lineTo(size.width - strokeWidth, size.height - inset)
                            lineTo(size.width - inset, size.height - strokeWidth)
                            lineTo(inset, size.height - strokeWidth)
                            lineTo(strokeWidth, size.height - inset)
                            lineTo(strokeWidth, inset)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = strokeWidth)
                        )

                        // Elegant small dots at the corner notches
                        val dotRadius = 3.dp.toPx()
                        drawCircle(color = color, radius = dotRadius, center = Offset(inset, inset))
                        drawCircle(color = color, radius = dotRadius, center = Offset(size.width - inset, inset))
                        drawCircle(color = color, radius = dotRadius, center = Offset(inset, size.height - inset))
                        drawCircle(color = color, radius = dotRadius, center = Offset(size.width - inset, size.height - inset))

                        // Corner lines for high-quality playing card detail
                        drawLine(color = color, start = Offset(strokeWidth, inset), end = Offset(inset, strokeWidth), strokeWidth = strokeWidth * 1.5f)
                        drawLine(color = color, start = Offset(size.width - strokeWidth, inset), end = Offset(size.width - inset, strokeWidth), strokeWidth = strokeWidth * 1.5f)
                        drawLine(color = color, start = Offset(strokeWidth, size.height - inset), end = Offset(inset, size.height - strokeWidth), strokeWidth = strokeWidth * 1.5f)
                        drawLine(color = color, start = Offset(size.width - strokeWidth, size.height - inset), end = Offset(size.width - inset, size.height - strokeWidth), strokeWidth = strokeWidth * 1.5f)
                    }

                    // Content layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Character Portrait Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.8f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(BorderStroke(2.dp, factionColor.copy(alpha = 0.6f)), RoundedCornerShape(14.dp))
                        ) {
                            Image(
                                painter = painterResource(id = backgroundRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Dark dramatic vignette/gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                            )
                        }

                        // 2. Custom Divider with diamond decoration in the middle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(factionColor.copy(alpha = 0.4f)))
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .size(8.dp)
                                    .rotate(45f)
                                    .background(factionColor)
                            )
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(factionColor.copy(alpha = 0.4f)))
                        }

                        // 3. Text and Info Panel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.2f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Role Name (پدرخوانده / کارآگاه / ...)
                            Text(
                                text = role.title,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Faction Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(factionColor.copy(alpha = 0.15f))
                                    .border(1.dp, factionColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "تیم: ${role.faction.persianTitle}",
                                    color = factionColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Description
                            Text(
                                text = role.description,
                                color = MafiaTextIce,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("reveal_confirm_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MafiaRed,
                disabledContainerColor = MafiaBorder
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "شروع بازی (ورود به شب اول) ⏭️",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 4. Night Actions targeting layout
@Composable
fun NightActionView(
    state: MafiaUiState,
    title: String,
    instruction: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onSelectPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = MafiaTextIce,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "فاز فعال",
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = instruction,
            color = MafiaTextMuted,
            fontSize = 12.sp
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val aliveOtherPlayers = state.players.filter { it.isAlive }

            items(aliveOtherPlayers) { player ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPlayer(player.id) }
                        .testTag("player_card_${player.id}"),
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MafiaBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(getAvatarColor(player.avatarId).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(getAvatarEmoji(player.avatarId), fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = player.name + if (player.isUser) " (شما)" else "",
                                    color = MafiaTextIce,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "بازیکن آنلاین زنده",
                                    color = MafiaTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = "انتخاب",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// 5. Day Summary (Narrator announcement)
@Composable
fun DaySummaryView(
    state: MafiaUiState,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "طلوع روز ${state.dayNumber.toPersianDigits()}",
            color = MafiaTextIce,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MafiaBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isNarratorLoading) {
                    CircularProgressIndicator(color = MafiaRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "راوی هوشمند هوش مصنوعی در حال نگارش وقایع شب...",
                        color = MafiaTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MafiaGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "بیانیه رسمی راوی بازی",
                        color = MafiaGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.narrationText,
                        color = MafiaTextIce,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("narrator_speech")
                    )
                }
            }
        }

        Button(
            onClick = onProceed,
            enabled = !state.isNarratorLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_chat_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MafiaRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "ورود به مرحله نوبت سخنرانی 🎤",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 6. Day Speaking View (Turn-based dashboard with timers & reaction emojis)
@Composable
fun DaySpeakingView(
    state: MafiaUiState,
    onPassTurn: () -> Unit,
    onSendReaction: (String) -> Unit,
    onToggleUserMic: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSpeaker = state.players.find { it.id == state.currentSpeakingPlayerId }
    val isUserSpeaking = currentSpeaker?.isUser == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10284F), // Light Navy glow at center
                        Color(0xFF061022), // Medium Navy
                        Color(0xFF02060F)  // Dark/black Navy at edges
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active Speaker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, if (isUserSpeaking) MafiaRed else MafiaGold)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF12284C).copy(alpha = 0.9f),
                                    Color(0xFF081426).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(getAvatarColor(currentSpeaker?.avatarId ?: 1).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(getAvatarEmoji(currentSpeaker?.avatarId ?: 1), fontSize = 14.sp)
                            }
                            Text(
                                text = (currentSpeaker?.name ?: "") + if (isUserSpeaking) " (نوبت شما)" else "",
                                color = MafiaTextIce,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Timer badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.speakingTimerSeconds <= 5) MafiaRed.copy(alpha = 0.2f) else MafiaBorder)
                                .border(1.dp, if (state.speakingTimerSeconds <= 5) MafiaRed else MafiaBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${state.speakingTimerSeconds.toPersianDigits()} ثانیه",
                                color = if (state.speakingTimerSeconds <= 5) MafiaRed else MafiaGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Dynamic Audio Animation + Speech Bubble
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AudioVisualizerWave(isActive = if (isUserSpeaking) state.isUserMicEnabled else true)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isUserSpeaking) {
                                    if (state.isUserMicEnabled) "میکروفون فعال است. لطفاً صحبت کنید..." else "میکروفون خاموش است. دکمه پایین را فشار دهید."
                                } else {
                                    "« ${state.currentSpeakerText} »"
                                },
                                color = MafiaTextIce,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontStyle = if (!isUserSpeaking) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                            )
                        }
                    }

                    // Controls for User
                    if (isUserSpeaking) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onToggleUserMic(!state.isUserMicEnabled) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isUserMicEnabled) MafiaRed else MafiaGreen
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (state.isUserMicEnabled) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (state.isUserMicEnabled) "قطع میکروفون" else "روشن کردن میکروفون 🎤",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = onPassTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = MafiaBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Text("رد کردن ⏭️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MafiaTextIce)
                            }
                        }
                    }
                }
            }

        // 12 Players Speaking States Grid
        Text(
            text = "وضعیت زنده اعضای بازی",
            color = MafiaTextIce,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.players) { player ->
                val isSpeaking = player.id == state.currentSpeakingPlayerId
                val playerReactions = state.activeReactions.filter { it.playerId == player.id }

                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .alpha(if (player.isAlive) 1f else 0.4f)
                ) {
                    // 3D Shadow Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 3.dp, y = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSpeaking) Color.White.copy(alpha = 0.2f)
                                else Color.Black.copy(alpha = 0.45f)
                            )
                    )

                    // Card Main Front Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isSpeaking) {
                                        listOf(Color(0xFF2E3440), Color(0xFF1B1E26))
                                    } else {
                                        listOf(Color(0xFF1B1E2E), Color(0xFF0F111B))
                                    }
                                )
                            )
                            .border(
                                width = if (isSpeaking) 2.dp else 1.dp,
                                color = if (isSpeaking) Color.White else if (player.isAlive) MafiaBorder else Color.DarkGray,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(54.dp)
                                    .background(getAvatarColor(player.avatarId).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(getAvatarEmoji(player.avatarId), fontSize = 28.sp)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = player.name + if (player.isUser) " (شما)" else "",
                                    color = if (isSpeaking) Color.White else MafiaTextIce,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = null,
                                        tint = if (isSpeaking) Color.White else MafiaTextMuted,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = if (!player.isAlive) "حذف شده" else if (isSpeaking) "گویا" else "ساکت",
                                        color = if (isSpeaking) Color.White else MafiaTextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Floating Reaction bubble
                    if (playerReactions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .clip(CircleShape)
                                .background(MafiaBg)
                                .border(1.dp, MafiaGold.copy(alpha = 0.4f), CircleShape)
                                .size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(playerReactions.last().emoji, fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // Floating Reaction Bar at the Bottom
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MafiaBorder)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ارسال واکنش سریع به سخنگو 👇", color = MafiaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val emojis = listOf("👍", "👎", "🤫", "😮", "😠", "💀", "👏", "😂")
                    emojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MafiaBg)
                                .border(1.dp, MafiaBorder, CircleShape)
                                .clickable { onSendReaction(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
}

// Speech Visualizer Animation component
@Composable
fun AudioVisualizerWave(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulsate")
    val size1 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "size1"
    )
    val size2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "size2"
    )

    if (isActive) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(size2)
                    .clip(CircleShape)
                    .background(MafiaRed.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .scale(size1)
                    .clip(CircleShape)
                    .background(MafiaRed.copy(alpha = 0.3f))
            )
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = MafiaRed,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = null,
            tint = MafiaTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

// 7. Day Voting Layout
@Composable
fun DayVotingView(
    state: MafiaUiState,
    onCastVote: (String) -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentNominee = state.players.find { it.id == state.currentNomineePlayerId }
    val yesVoters = state.nomineeVotes[state.currentNomineePlayerId] ?: emptyList()
    val alivePlayers = state.players.filter { it.isAlive }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP CARD: Current Nominee for Voting (در کادر بالای بازی قرار بگیرند)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MafiaRed.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, MafiaGold),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚖️ نوبت‌دهی انفرادی دادگاه",
                        color = MafiaGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Timer Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.votingTimerSeconds <= 3) MafiaRed.copy(alpha = 0.2f) else MafiaBorder)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "زمان: ${state.votingTimerSeconds.toPersianDigits()} ثانیه",
                            color = if (state.votingTimerSeconds <= 3) MafiaRed else MafiaGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (currentNominee != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nominee Avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(currentNominee.avatarId).copy(alpha = 0.15f))
                                .border(2.dp, MafiaGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(getAvatarEmoji(currentNominee.avatarId), fontSize = 32.sp)
                        }

                        // Nominee Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentNominee.name + if (currentNominee.isUser) " (شما)" else "",
                                color = MafiaTextIce,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مجموع آرای موافق: ${yesVoters.size.toPersianDigits()} رأی",
                                color = MafiaTextMuted,
                                fontSize = 12.sp
                            )
                        }

                        // Cast Vote Action Button for the User
                        val hasUserVotedForCurrent = "user" in yesVoters
                        val isUserAlreadyVotedAny = state.userSelectedVoteId != null

                        Button(
                            onClick = { onCastVote(currentNominee.id) },
                            enabled = !isUserAlreadyVotedAny && currentNominee.id != "user",
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasUserVotedForCurrent) MafiaGreen else MafiaGold,
                                disabledContainerColor = if (hasUserVotedForCurrent) MafiaGreen.copy(alpha = 0.5f) else MafiaBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hasUserVotedForCurrent) "رأی دادم 👍" else "رأی موافق 👍",
                                    color = if (hasUserVotedForCurrent) Color.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "در حال آماده‌سازی نامزدها...",
                        color = MafiaTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Subtitle instructions
        Text(
            text = "در این مرحله بازیکنان زنده تک‌تک کاندید دفاع می‌شوند. با انتخاب دکمه رأی موافق 👍 به آنها رأی دهید. رأی سایر اعضا نیز با استیکر 👍 روی پروفایلشان نمایش داده می‌شود:",
            color = MafiaTextMuted,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // GRID/LIST OF ALL PLAYERS (2 columns of 6, side-by-side) showing their "Like" 👍 sticker in real-time
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.players) { player ->
                val isNominee = player.id == state.currentNomineePlayerId
                val hasVotedYes = player.id in yesVoters

                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .testTag("vote_player_${player.id}")
                        .alpha(if (player.isAlive) 1f else 0.4f)
                ) {
                    // 3D Shadow Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 3.dp, y = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isNominee) Color.White.copy(alpha = 0.2f)
                                else Color.Black.copy(alpha = 0.45f)
                            )
                    )

                    // Card Main Front Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isNominee) {
                                        listOf(Color(0xFF2E3440), Color(0xFF1B1E26))
                                    } else {
                                        listOf(Color(0xFF1B1E2E), Color(0xFF0F111B))
                                    }
                                )
                            )
                            .border(
                                width = if (isNominee) 2.dp else 1.dp,
                                color = if (isNominee) Color.White else if (player.isAlive) MafiaBorder else Color.DarkGray,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(54.dp)
                                    .background(getAvatarColor(player.avatarId).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(getAvatarEmoji(player.avatarId), fontSize = 28.sp)

                                // LIKES STICKER DISPLAY
                                if (hasVotedYes && player.isAlive) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = (-2).dp, y = (-2).dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MafiaGreen)
                                            .border(1.dp, Color.Black, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👍", fontSize = 14.sp)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = player.name + if (player.isUser) " (شما)" else "",
                                    color = if (isNominee) Color.White else MafiaTextIce,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Status / Vote Status
                                if (!player.isAlive) {
                                    Text("💀 حذف شده", color = MafiaTextMuted, fontSize = 9.sp)
                                } else if (isNominee) {
                                    Text("⚖️ کاندیدای دفاع", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                } else if (hasVotedYes) {
                                    Text("👍 رأی موافق", color = MafiaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("منتظر رأی...", color = MafiaTextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fast-forward / Skip current turn button
        Button(
            onClick = onProceed,
            colors = ButtonDefaults.buttonColors(containerColor = MafiaBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text("عبور از نوبت نامزد فعلی ⏭️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 8. Day Trial Layout
@Composable
fun DayTrialView(
    state: MafiaUiState,
    onPassCourtSpeaking: () -> Unit,
    onCastCourtVote: (String, Boolean) -> Unit,
    onConfirmCourtVotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚖️ دادگاه عالی شهر ⚖️",
            color = MafiaTextIce,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // 1. DEDICATED COURT CANDIDATES BOX AT THE TOP (در کادر بالا مجزا بشن به عنوان دادگاه)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MafiaRed.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, MafiaGold.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "لیست متهمان حاضر در دادگاه 🏛️",
                    color = MafiaGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.courtPlayers.forEach { accused ->
                        val isCurrentlySpeaking = state.courtSpeakingPlayerId == accused.id && state.courtPhase == CourtPhase.SPEECHES
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                        ) {
                            // 3D Shadow Layer
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCurrentlySpeaking) Color.White.copy(alpha = 0.2f)
                                        else Color.Black.copy(alpha = 0.45f)
                                    )
                            )

                            // Main Card Front Layer
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = if (isCurrentlySpeaking) {
                                                listOf(Color(0xFF2E3440), Color(0xFF1B1E26))
                                            } else {
                                                listOf(Color(0xFF1B1E2E), Color(0xFF0F111B))
                                            }
                                        )
                                    )
                                    .border(
                                        width = if (isCurrentlySpeaking) 1.5.dp else 1.dp,
                                        color = if (isCurrentlySpeaking) Color.White else MafiaBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(getAvatarColor(accused.avatarId).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(getAvatarEmoji(accused.avatarId), fontSize = 10.sp)
                                    }
                                    Text(
                                        text = accused.name,
                                        color = if (isCurrentlySpeaking) Color.White else MafiaTextIce,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrentlySpeaking) {
                                        Text(
                                            text = "🎤",
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. MAIN WORKSPACE AREA BY COURT PHASE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MafiaBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (state.courtPhase) {
                    CourtPhase.SPEECHES -> {
                        val activeSpeaker = state.courtPlayers.find { it.id == state.courtSpeakingPlayerId }
                        if (activeSpeaker != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Speaker Header with Timer
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(getAvatarColor(activeSpeaker.avatarId).copy(alpha = 0.15f))
                                            .border(2.dp, MafiaGold, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(getAvatarEmoji(activeSpeaker.avatarId), fontSize = 32.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "نوبت صحبت: ${activeSpeaker.name}",
                                        color = MafiaTextIce,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "زمان دفاع: ${state.courtSpeakingTimerSeconds.toPersianDigits()} ثانیه",
                                        color = if (state.courtSpeakingTimerSeconds <= 5) MafiaRed else MafiaGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Speech Content Bubble
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(vertical = 12.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MafiaBg)
                                        .border(1.dp, MafiaBorder, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (activeSpeaker.isUser) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "🎤 نوبت صحبت و دفاع شماست!",
                                                color = MafiaGold,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "با فکت و دلایل محکم بی گناهی خود را به شهر ثابت کنید.",
                                                color = MafiaTextMuted,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "« ${state.courtSpeakerText} »",
                                            color = MafiaTextIce,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontStyle = FontStyle.Italic,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }

                                // Pass Speaker Button
                                if (activeSpeaker.isUser) {
                                    Button(
                                        onClick = onPassCourtSpeaking,
                                        colors = ButtonDefaults.buttonColors(containerColor = MafiaBorder),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Text("پایان دفاع (پاس نوبت) ⏭️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // Simulated audio waves for speaking bot
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text("در حال صحبت و دفاع...", color = MafiaTextMuted, fontSize = 10.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(5) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.dp, (8..24).random().dp)
                                                        .clip(RoundedCornerShape(1.dp))
                                                        .background(MafiaGold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CourtPhase.VOTING -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "رأی‌گیری نهایی دادگاه 🗳️",
                                color = MafiaTextIce,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "برای هر متهم تصمیم بگیرید که آیا باید اعدام و اخراج شود یا تبرئه گردد. متهمانی که بیش از ۵۰٪ رأی اعدام کسب کنند از بازی حذف خواهند شد:",
                                color = MafiaTextMuted,
                                fontSize = 10.sp,
                                lineHeight = 16.sp
                            )

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.courtPlayers) { player ->
                                    val votesToEliminate = state.courtVotes[player.id]?.size ?: 0
                                    val totalVotersCount = state.players.filter { it.isAlive }.size
                                    val userVotedEliminate = state.userTrialVotes[player.id] == true

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MafiaBg),
                                        border = BorderStroke(1.dp, MafiaBorder),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(getAvatarColor(player.avatarId).copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(getAvatarEmoji(player.avatarId), fontSize = 12.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = player.name,
                                                        color = MafiaTextIce,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // Voting options buttons for the user on this defendant
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Button(
                                                        onClick = { onCastCourtVote(player.id, false) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (!userVotedEliminate && state.userTrialVotes[player.id] != null) MafiaGreen.copy(alpha = 0.2f) else MafiaBorder,
                                                            contentColor = if (!userVotedEliminate && state.userTrialVotes[player.id] != null) MafiaGreen else MafiaTextIce
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("تبرئه 🟢", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Button(
                                                        onClick = { onCastCourtVote(player.id, true) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (userVotedEliminate) MafiaRed.copy(alpha = 0.2f) else MafiaBorder,
                                                            contentColor = if (userVotedEliminate) MafiaRed else MafiaTextIce
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("حذف 🔴", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            // Real-time vote ratio bar
                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "آرای موافق حذف: ${votesToEliminate.toPersianDigits()} از ${totalVotersCount.toPersianDigits()}",
                                                        color = MafiaTextMuted,
                                                        fontSize = 9.sp
                                                    )
                                                    val percentage = (votesToEliminate.toFloat() / totalVotersCount * 100).toInt()
                                                    Text(
                                                        text = "${percentage.toPersianDigits()}%",
                                                        color = if (percentage >= 50) MafiaRed else MafiaGold,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                LinearProgressIndicator(
                                                    progress = { votesToEliminate.toFloat() / totalVotersCount },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(CircleShape),
                                                    color = if (votesToEliminate.toFloat() / totalVotersCount >= 0.5f) MafiaRed else MafiaGold,
                                                    trackColor = MafiaBorder.copy(alpha = 0.3f)
                                                )
                                            }

                                            // Real-time voter profiles display with 👍 stickers (رای اعضا با استیکر لایک مشخص شود)
                                            val yesVotersToEliminate = state.courtVotes[player.id] ?: emptyList()
                                            if (yesVotersToEliminate.isNotEmpty()) {
                                                HorizontalDivider(color = MafiaBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = "رأی‌دهندگان به حذف (موافقان اعدام):",
                                                        color = MafiaTextMuted,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        yesVotersToEliminate.forEach { voterId ->
                                                            val voter = state.players.find { it.id == voterId }
                                                            if (voter != null) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .background(MafiaBg)
                                                                        .border(1.dp, MafiaBorder, RoundedCornerShape(6.dp))
                                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                                    ) {
                                                                        Text("👍", fontSize = 10.sp)
                                                                        Text(getAvatarEmoji(voter.avatarId), fontSize = 10.sp)
                                                                        Text(
                                                                            text = voter.name + if (voter.isUser) " (شما)" else "",
                                                                            color = MafiaTextIce,
                                                                            fontSize = 9.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = onConfirmCourtVotes,
                                colors = ButtonDefaults.buttonColors(containerColor = MafiaRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تایید نهایی و مشاهده نتایج ⚖️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    CourtPhase.RESULTS -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MafiaGold.copy(alpha = 0.12f))
                                        .border(2.dp, MafiaGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = MafiaGold,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "احکام صادر شده از دادگاه",
                                    color = MafiaGold,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "با پایان زمان رأی‌گیری نهایی دادگاه، آرا تجمیع شدند:",
                                    color = MafiaTextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Summary list of court player fates
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.courtPlayers) { player ->
                                    val yesVotesList = state.courtVotes[player.id] ?: emptyList()
                                    val totalYesVotes = yesVotesList.size
                                    val totalVotersCount = state.players.filter { it.isAlive }.size + 1 // including user or total alive
                                    val isEliminated = totalYesVotes > (state.players.filter { it.isAlive }.size / 2.0)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isEliminated) MafiaRed.copy(alpha = 0.08f) else MafiaGreen.copy(alpha = 0.08f))
                                            .border(
                                                1.dp, 
                                                if (isEliminated) MafiaRed.copy(alpha = 0.4f) else MafiaGreen.copy(alpha = 0.4f), 
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(getAvatarColor(player.avatarId).copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(getAvatarEmoji(player.avatarId), fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = player.name,
                                                    color = MafiaTextIce,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "${totalYesVotes.toPersianDigits()} رأی موافق اخراج",
                                                    color = MafiaTextMuted,
                                                    fontSize = 10.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isEliminated) MafiaRed else MafiaGreen)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (isEliminated) "اخراج و حذف 🔴" else "تبرئه و ماندگار 🟢",
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

                            Text(
                                text = "انتقال خودکار به فاز شب تا لحظاتی دیگر...",
                                color = MafiaTextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 9. Game Over Layout
@Composable
fun GameOverView(
    state: MafiaUiState,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val won = state.winningFaction == state.currentRole.faction
    val victoryText = if (won) "تبریک! شما پیروز شدید 🎉" else "شما شکست خوردید 💀"
    val winnerFactionTitle = state.winningFaction?.persianTitle ?: ""

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "پایان بازی مافیا",
            color = MafiaTextIce,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, if (won) MafiaGreen else MafiaRed)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = victoryText,
                    color = if (won) MafiaGreen else MafiaRed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "جناح پیروز بازی: $winnerFactionTitle",
                    color = MafiaTextIce,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "نقش نهایی شما: ${state.currentRole.title}",
                    color = MafiaGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "لیست کامل نقش‌ها و وضعیت نهایی بازیکنان:",
            color = MafiaTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.players) { player ->
                val factionColor = if (player.role.faction == Faction.MAFIA) MafiaRed else MafiaBlue
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MafiaBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(getAvatarColor(player.avatarId).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(getAvatarEmoji(player.avatarId), fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = player.name + if (player.isUser) " (شما)" else "",
                                    color = MafiaTextIce,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = player.role.title,
                                    color = factionColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (player.isAlive) MafiaGreen.copy(alpha = 0.15f) else MafiaRed.copy(alpha = 0.15f))
                                .border(1.dp, if (player.isAlive) MafiaGreen else MafiaRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (player.isAlive) "زنده" else "حذف شده",
                                color = if (player.isAlive) MafiaGreen else MafiaRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("play_again_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MafiaRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("بازگشت به لابی اصلی و بازی مجدد 🔄", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// 10. History Dialog
@Composable
fun HistoryDialog(
    history: List<MatchRecord>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تاریخچه مسابقات شما",
                color = MafiaTextIce,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (history.isEmpty()) {
                    Text(
                        text = "هنوز هیچ مسابقه‌ای ثبت نشده است.",
                        color = MafiaTextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(history) { record ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MafiaBg),
                                border = BorderStroke(1.dp, MafiaBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = record.mode,
                                            color = MafiaGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = record.timestamp.toPersianDateTime(),
                                            color = MafiaTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "نام بازیکن: ${record.playerX} | جناح برنده: ${record.playerO}",
                                        color = MafiaTextIce,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "نتیجه شما: " + if (record.winner == "X") "پیروزی 🎉" else "شکست 💀",
                                        color = if (record.winner == "X") MafiaGreen else MafiaRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = MafiaTextIce)
            }
        },
        dismissButton = {
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("پاک کردن تاریخچه", color = MafiaRed)
                }
            }
        },
        containerColor = MafiaCardBg
    )
}
