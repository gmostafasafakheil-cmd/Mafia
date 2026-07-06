package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.MatchRecord
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class GamePhase {
    LOBBY,
    ROLE_REVEAL,
    NIGHT_MAFIA,
    NIGHT_DETECTIVE,
    NIGHT_DOCTOR,
    DAY_SUMMARY,
    DAY_SPEAKING, // Turn-based speaking round with timers
    DAY_VOTING,
    DAY_TRIAL,
    GAME_OVER
}

enum class Faction(val persianTitle: String) {
    CITIZENS("شهروندان"),
    MAFIA("مافیا")
}

enum class Role(
    val title: String,
    val description: String,
    val faction: Faction,
    val maxBullets: Int = 0
) {
    MAFIA("مافیا ساده", "شب‌ها تصمیم می‌گیرید با هم‌تیمی‌ها چه کسی را حذف کنید و روزها باید در نقش شهروند تظاهر کنید.", Faction.MAFIA),
    GODFATHER("پدرخوانده (رئیس)", "تصمیم‌گیرنده نهایی شلیک شب. استعلام شما برای کارآگاه منفی (بی‌گناه) است.", Faction.MAFIA),
    LEKTOR("دکتر لکتور", "هم‌تیمی مافیا. شب‌ها می‌تواند یکی از اعضای مافیا را در برابر استعلام کارآگاه محافظت کند.", Faction.MAFIA),
    DOCTOR("دکتر شهر", "هر شب می‌توانید یک نفر (از جمله خودتان) را از شلیک شب نجات دهید.", Faction.CITIZENS),
    DETECTIVE("کارآگاه", "هر شب می‌توانید استعلام نقش یکی از بازیکنان را بگیرید تا بدانید مافیا است یا خیر.", Faction.CITIZENS),
    SNIPER("حرفه‌ای (اسنایپر)", "دارای ۲ تیر جنگی. شب‌ها می‌توانید شلیک کنید. مواظب باشید اگر به شهروند بزنید خودتان حذف می‌شوید.", Faction.CITIZENS, 2),
    CITIZEN("شهروند ساده", "نقش فعالی در شب ندارید. روزها باید با دقت در بحث‌ها شرکت کنید و مافیا را شناسایی کنید.", Faction.CITIZENS)
}

data class MafiaPlayer(
    val id: String,
    val name: String,
    val role: Role,
    val isAlive: Boolean = true,
    val isUser: Boolean = false,
    val inquiryChecked: Boolean = false,
    val avatarId: Int = 1
)

data class ChatMessage(
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val isUser: Boolean = false
)

data class GameRoom(
    val id: String,
    val name: String,
    val hostName: String,
    val currentPlayers: Int,
    val maxPlayers: Int = 12,
    val status: String = "در انتظار بازیکن" // "در انتظار بازیکن", "در حال بازی"
)

data class ReactionEvent(
    val id: String = UUID.randomUUID().toString(),
    val playerId: String,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class CourtPhase {
    SPEECHES,
    VOTING,
    RESULTS
}

data class MafiaUiState(
    val gamePhase: GamePhase = GamePhase.LOBBY,
    // Personal Profile
    val userNickname: String = "آریا",
    val profileAvatarId: Int = 1, // 1 to 6
    val profileBio: String = "عاشق بازی‌های معمایی و مافیا",
    
    // Room directory & selection
    val availableRooms: List<GameRoom> = emptyList(),
    val activeRoomId: String? = null,
    val activeRoomName: String = "",
    val activeRoomHostId: String = "",
    val activeRoomPlayers: List<MafiaPlayer> = emptyList(),
    val isHost: Boolean = false,
    val joinRequests: List<MafiaPlayer> = emptyList(), // Queue of bot join requests for Host approval
    val roomCode: String = "",
    
    // Core game state
    val players: List<MafiaPlayer> = emptyList(),
    val currentRole: Role = Role.CITIZEN,
    val dayNumber: Int = 1,
    val chatMessages: List<ChatMessage> = emptyList(),
    val narrationText: String = "",
    val isNarratorLoading: Boolean = false,
    val winningFaction: Faction? = null,
    val detectiveInquiryResult: String? = null,
    val currentVoteLeader: String? = null,
    val currentTrialPlayer: MafiaPlayer? = null,
    val userSelectedVoteId: String? = null,
    val votes: Map<String, Int> = emptyMap(), // playerId -> voteCount
    val lastNightKilled: String? = null,
    val lastNightSaved: Boolean = false,
    val sniperBulletsRemaining: Int = 2,
    val matchHistory: List<MatchRecord> = emptyList(),
    
    // Speaking / Turn-based round
    val currentSpeakingPlayerId: String? = null,
    val speakingTimerSeconds: Int = 15,
    val isUserMicEnabled: Boolean = false,
    val activeReactions: List<ReactionEvent> = emptyList(),
    val currentSpeakerText: String = "",

    // Voting phase and Court (Trial) phase additions
    val votingTimerSeconds: Int = 20,
    val voterDetails: Map<String, String> = emptyMap(), // voterId -> targetPlayerId
    val currentNomineePlayerId: String? = null, // The player currently nominated for a vote
    val nomineeVotes: Map<String, List<String>> = emptyMap(), // nomineePlayerId -> list of voterIds who voted for them
    val courtPlayers: List<MafiaPlayer> = emptyList(),
    val courtPhase: CourtPhase = CourtPhase.SPEECHES,
    val courtSpeakingPlayerId: String? = null,
    val courtSpeakingTimerSeconds: Int = 15,
    val courtSpeakerText: String = "",
    val courtVotes: Map<String, List<String>> = emptyMap(), // courtPlayerId -> list of voterIds who voted Yes/Eliminate
    val userTrialVotes: Map<String, Boolean> = emptyMap(), // courtPlayerId -> true/false (eliminate/spare)
    val isOnlineMode: Boolean = false,
    val isFirebaseConfigured: Boolean = false,
    val firebaseError: String? = null
)

class MafiaViewModel(private val repository: GameRepository) : ViewModel() {

    val userId: String by lazy { repository.getUserId() }

    private var roomsValueListener: com.google.firebase.database.ValueEventListener? = null
    private var activeRoomValueListener: com.google.firebase.database.ValueEventListener? = null

    private val _uiState = MutableStateFlow(MafiaUiState())
    val uiState: StateFlow<MafiaUiState> = _uiState.asStateFlow()

    // Persian names for bots
    private val botNames = listOf(
        "رضا", "مریم", "امیر", "سارا", "بردیا", "الناز", "نیما", "پرستو", "آبتین", "هستی",
        "پوریا", "مهسا", "آرش", "مینا", "سینا", "شیدا", "کوروش", "نگین", "کیان", "دینا"
    )

    private val botBios = listOf(
        "حرفه‌ای بازی مافیا", "شهروند کاربلد", "مافیا رو از چشمش می‌شناسم", "همیشه شهروندم!",
        "عاشق ساید مافیا", "سکوت بهترین تاکتیکه", "با فکت بازی می‌کنم"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var speakingJob: Job? = null
    private var lobbySimulationJob: Job? = null
    private var votingJob: Job? = null
    private var courtSpeakingJob: Job? = null

    init {
        // Load saved user profile
        val savedNickname = repository.getSavedNickname("آریا")
        val savedAvatarId = repository.getSavedAvatarId(1)
        val savedBio = repository.getSavedBio("عاشق بازی‌های معمایی و مافیا")

        // Generate initial random room code & sample available rooms
        val randomCode = (1000..9999).random().toString()
        _uiState.update { 
            it.copy(
                userNickname = savedNickname,
                profileAvatarId = savedAvatarId,
                profileBio = savedBio,
                roomCode = randomCode,
                availableRooms = generateSampleRooms()
            ) 
        }

        // Fetch match history
        viewModelScope.launch {
            repository.allMatches.collect { history ->
                _uiState.update { it.copy(matchHistory = history) }
            }
        }

        // Periodically clear expired reaction events (older than 2 seconds)
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                _uiState.update { state ->
                    state.copy(
                        activeReactions = state.activeReactions.filter { now - it.timestamp < 2000 }
                    )
                }
            }
        }
    }

    fun toggleOnlineMode(enabled: Boolean) {
        if (enabled) {
            val isAvailable = try {
                com.google.firebase.FirebaseApp.getInstance()
                true
            } catch (e: Exception) {
                false
            }
            if (isAvailable) {
                _uiState.update { it.copy(isOnlineMode = true, isFirebaseConfigured = true, firebaseError = null) }
                startFirebaseRoomsListener()
            } else {
                _uiState.update { 
                    it.copy(
                        isOnlineMode = false, 
                        isFirebaseConfigured = false, 
                        firebaseError = "سرویس فایربیس (Firebase) در برنامه شناسایی نشد.\nبرای فعال‌سازی کامل بخش تجاری و آنلاین واقعی، فایل تنظیمات فایربیس (google-services.json) پروژه خود را در پوشه app برنامه قرار داده و مجدد بیلد کنید."
                    ) 
                }
            }
        } else {
            _uiState.update { it.copy(isOnlineMode = false, availableRooms = generateSampleRooms(), firebaseError = null) }
            stopFirebaseRoomsListener()
        }
    }

    private fun startFirebaseRoomsListener() {
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val roomsRef = database.getReference("rooms")
            roomsValueListener?.let { roomsRef.removeEventListener(it) }

            roomsValueListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val rooms = mutableListOf<GameRoom>()
                    for (child in snapshot.children) {
                        try {
                            val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                            val name = child.child("name").getValue(String::class.java) ?: "اتاق مافیا"
                            val hostName = child.child("hostName").getValue(String::class.java) ?: "میزبان"
                            val status = child.child("status").getValue(String::class.java) ?: "در انتظار بازیکن"
                            val maxPlayers = child.child("maxPlayers").getValue(Int::class.java) ?: 12
                            
                            val playersSnap = child.child("players")
                            val currentPlayersCount = playersSnap.childrenCount.toInt()

                            rooms.add(GameRoom(id, name, hostName, currentPlayersCount, maxPlayers, status))
                        } catch (e: Exception) {
                            Log.e("MafiaViewModel", "Error parsing room: ${e.message}")
                        }
                    }
                    _uiState.update { it.copy(availableRooms = rooms, firebaseError = null) }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    _uiState.update { it.copy(firebaseError = "خطای همگام‌سازی فایربیس: ${error.message}") }
                }
            }

            roomsRef.addValueEventListener(roomsValueListener!!)
        } catch (e: Exception) {
            _uiState.update { it.copy(firebaseError = "خطای اتصال به فایربیس: ${e.message}") }
        }
    }

    private fun stopFirebaseRoomsListener() {
        try {
            if (roomsValueListener != null) {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                database.getReference("rooms").removeEventListener(roomsValueListener!!)
                roomsValueListener = null
            }
        } catch (e: Exception) {
            Log.e("MafiaViewModel", "Error stopping room listener: ${e.message}")
        }
    }

    private fun startActiveRoomFirebaseListener(roomId: String) {
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val roomRef = database.getReference("rooms").child(roomId)
            activeRoomValueListener?.let { roomRef.removeEventListener(it) }

            activeRoomValueListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (!snapshot.exists()) return
                    
                    val hostId = snapshot.child("hostId").getValue(String::class.java) ?: ""
                    val roomName = snapshot.child("name").getValue(String::class.java) ?: ""
                    val code = snapshot.child("code").getValue(String::class.java) ?: ""
                    
                    val playersList = mutableListOf<MafiaPlayer>()
                    val playersSnap = snapshot.child("players")
                    for (playerSnap in playersSnap.children) {
                        try {
                            val id = playerSnap.child("id").getValue(String::class.java) ?: playerSnap.key ?: ""
                            val name = playerSnap.child("name").getValue(String::class.java) ?: "بازیکن"
                            val avatarId = playerSnap.child("avatarId").getValue(Int::class.java) ?: 1
                            val roleStr = playerSnap.child("role").getValue(String::class.java) ?: "CITIZEN"
                            val isAlive = playerSnap.child("alive").getValue(Boolean::class.java) ?: true
                            
                            val role = try { Role.valueOf(roleStr) } catch(e: Exception) { Role.CITIZEN }
                            
                            val isUser = id == userId
                            
                            playersList.add(MafiaPlayer(
                                id = id,
                                name = name,
                                role = role,
                                isAlive = isAlive,
                                isUser = isUser,
                                avatarId = avatarId
                            ))
                        } catch (e: Exception) {
                            Log.e("MafiaViewModel", "Error parsing player: ${e.message}")
                        }
                    }

                    val chatList = mutableListOf<ChatMessage>()
                    val chatSnap = snapshot.child("chatMessages")
                    for (msgSnap in chatSnap.children) {
                        try {
                            val senderName = msgSnap.child("senderName").getValue(String::class.java) ?: ""
                            val text = msgSnap.child("text").getValue(String::class.java) ?: ""
                            val isSystem = msgSnap.child("system").getValue(Boolean::class.java) ?: false
                            val senderId = msgSnap.child("senderId").getValue(String::class.java) ?: ""
                            
                            chatList.add(ChatMessage(
                                senderName = senderName,
                                text = text,
                                isSystem = isSystem,
                                isUser = senderId == userId
                            ))
                        } catch (e: Exception) {
                            Log.e("MafiaViewModel", "Error parsing chat: ${e.message}")
                        }
                    }

                    val phaseStr = snapshot.child("gamePhase").getValue(String::class.java) ?: "LOBBY"
                    val phase = try { GamePhase.valueOf(phaseStr) } catch(e: Exception) { GamePhase.LOBBY }
                    val currentSpeakingPlayerId = snapshot.child("currentSpeakingPlayerId").getValue(String::class.java)
                    val narrationText = snapshot.child("narrationText").getValue(String::class.java) ?: ""
                    
                    _uiState.update { state ->
                        state.copy(
                            activeRoomId = roomId,
                            activeRoomName = roomName,
                            activeRoomHostId = hostId,
                            activeRoomPlayers = playersList,
                            roomCode = code,
                            chatMessages = chatList,
                            gamePhase = phase,
                            currentSpeakingPlayerId = currentSpeakingPlayerId,
                            narrationText = narrationText,
                            isHost = hostId == userId
                        )
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e("MafiaViewModel", "Active room sync cancelled: ${error.message}")
                }
            }

            roomRef.addValueEventListener(activeRoomValueListener!!)
        } catch (e: Exception) {
            Log.e("MafiaViewModel", "Error starting active room listener: ${e.message}")
        }
    }

    private fun stopActiveRoomFirebaseListener(roomId: String) {
        try {
            if (activeRoomValueListener != null) {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                database.getReference("rooms").child(roomId).removeEventListener(activeRoomValueListener!!)
                activeRoomValueListener = null
            }
        } catch (e: Exception) {
            Log.e("MafiaViewModel", "Error stopping active room listener: ${e.message}")
        }
    }

    private fun generateSampleRooms(): List<GameRoom> {
        return listOf(
            GameRoom("room_1", "عمارت سرخ 🩸", "بردیا", 9, 12, "در انتظار بازیکن"),
            GameRoom("room_2", "کافه سناریو ☕", "پرستو", 11, 12, "در انتظار بازیکن"),
            GameRoom("room_3", "شهر بیدار 👁️", "امیر", 5, 12, "در انتظار بازیکن"),
            GameRoom("room_4", "مافیا بازان حرفه‌ای 🕵️", "سارا", 12, 12, "در حال بازی")
        )
    }

    // Profile updates
    fun updateProfile(nickname: String, avatarId: Int, bio: String) {
        val finalNickname = nickname.ifBlank { _uiState.value.userNickname }
        val finalBio = bio.ifBlank { _uiState.value.profileBio }

        _uiState.update {
            it.copy(
                userNickname = finalNickname,
                profileAvatarId = avatarId,
                profileBio = finalBio
            )
        }
        
        repository.saveUserProfile(finalNickname, avatarId, finalBio)
    }

    // 1. Create a Room as Host
    fun createRoom() {
        val nickname = _uiState.value.userNickname
        val avatarId = _uiState.value.profileAvatarId
        val code = (1000..9999).random().toString()
        val roomTitle = "اتاق $nickname 🏢"

        val hostPlayer = MafiaPlayer(
            id = userId,
            name = nickname,
            role = Role.CITIZEN, // Assigned at game start
            isAlive = true,
            isUser = true,
            avatarId = avatarId
        )

        if (_uiState.value.isOnlineMode) {
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                val roomRef = database.getReference("rooms").child(code)
                val roomData = mapOf(
                    "id" to code,
                    "name" to roomTitle,
                    "code" to code,
                    "hostId" to userId,
                    "hostName" to nickname,
                    "status" to "در انتظار بازیکن",
                    "gamePhase" to "LOBBY"
                )
                roomRef.setValue(roomData)
                
                val playerRef = roomRef.child("players").child(userId)
                val playerData = mapOf(
                    "id" to userId,
                    "name" to nickname,
                    "avatarId" to avatarId,
                    "role" to "CITIZEN",
                    "alive" to true
                )
                playerRef.setValue(playerData)

                _uiState.update {
                    it.copy(
                        activeRoomId = code,
                        activeRoomName = roomTitle,
                        activeRoomHostId = userId,
                        activeRoomPlayers = listOf(hostPlayer),
                        isHost = true,
                        roomCode = code,
                        joinRequests = emptyList(),
                        chatMessages = emptyList()
                    )
                }
                startActiveRoomFirebaseListener(code)
                addSystemMessage("اتاق آنلاین با موفقیت ساخته شد. کد اتاق: $code")
            } catch (e: Exception) {
                _uiState.update { it.copy(firebaseError = "خطا در ساخت اتاق آنلاین: ${e.message}") }
            }
        } else {
            _uiState.update {
                it.copy(
                    activeRoomId = "room_user",
                    activeRoomName = roomTitle,
                    activeRoomHostId = userId,
                    activeRoomPlayers = listOf(hostPlayer),
                    isHost = true,
                    roomCode = code,
                    joinRequests = emptyList(),
                    chatMessages = emptyList()
                )
            }
            startHostLobbySimulation()
        }
    }

    // Host Lobby: simulates players requesting to join
    private fun startHostLobbySimulation() {
        lobbySimulationJob?.cancel()
        lobbySimulationJob = viewModelScope.launch {
            delay(2000)
            val availableNames = botNames.shuffled()
            var nameIndex = 0

            while (_uiState.value.activeRoomId == "room_user" && _uiState.value.gamePhase == GamePhase.LOBBY) {
                val currentPlayers = _uiState.value.activeRoomPlayers.size
                val currentRequests = _uiState.value.joinRequests.size
                
                if (currentPlayers + currentRequests < 12 && nameIndex < availableNames.size) {
                    delay(3000) // request every 3 seconds
                    val botName = availableNames[nameIndex++]
                    val randomAvatar = (2..6).random()
                    val botRequest = MafiaPlayer(
                        id = "bot_${UUID.randomUUID()}",
                        name = botName,
                        role = Role.CITIZEN,
                        isAlive = true,
                        isUser = false,
                        avatarId = randomAvatar
                    )
                    _uiState.update { state ->
                        state.copy(joinRequests = state.joinRequests + botRequest)
                    }
                } else {
                    delay(2000)
                }
            }
        }
    }

    // Host accepts join request
    fun acceptJoinRequest(player: MafiaPlayer) {
        _uiState.update { state ->
            val updatedPlayers = state.activeRoomPlayers + player
            val updatedRequests = state.joinRequests.filter { it.id != player.id }
            state.copy(
                activeRoomPlayers = updatedPlayers,
                joinRequests = updatedRequests
            )
        }
        addSystemMessage("بازیکن «${player.name}» توسط مدیر تایید شد و وارد لابی شد.")
    }

    // Host rejects join request
    fun rejectJoinRequest(playerId: String) {
        _uiState.update { state ->
            val updatedRequests = state.joinRequests.filter { it.id != playerId }
            state.copy(joinRequests = updatedRequests)
        }
    }

    // 2. Join a Room as Guest
    fun joinRoom(room: GameRoom) {
        val nickname = _uiState.value.userNickname
        val avatarId = _uiState.value.profileAvatarId
        val userPlayer = MafiaPlayer(
            id = userId,
            name = nickname,
            role = Role.CITIZEN,
            isAlive = true,
            isUser = true,
            avatarId = avatarId
        )

        if (_uiState.value.isOnlineMode) {
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                val roomRef = database.getReference("rooms").child(room.id)
                
                val playerRef = roomRef.child("players").child(userId)
                val playerData = mapOf(
                    "id" to userId,
                    "name" to nickname,
                    "avatarId" to avatarId,
                    "role" to "CITIZEN",
                    "alive" to true
                )
                playerRef.setValue(playerData)

                // Add join message to chat
                val msgRef = roomRef.child("chatMessages").push()
                msgRef.setValue(mapOf(
                    "senderName" to "سیستم",
                    "text" to "«$nickname» وارد اتاق شد.",
                    "system" to true,
                    "senderId" to "system"
                ))

                _uiState.update {
                    it.copy(
                        activeRoomId = room.id,
                        activeRoomName = room.name,
                        activeRoomHostId = room.hostName,
                        activeRoomPlayers = listOf(userPlayer),
                        isHost = false,
                        roomCode = room.id,
                        chatMessages = emptyList()
                    )
                }
                startActiveRoomFirebaseListener(room.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(firebaseError = "خطا در ورود به اتاق آنلاین: ${e.message}") }
            }
        } else {
            _uiState.update {
                it.copy(
                    activeRoomId = room.id,
                    activeRoomName = room.name,
                    activeRoomHostId = "host_bot",
                    activeRoomPlayers = listOf(userPlayer),
                    isHost = false,
                    roomCode = (1000..9999).random().toString(),
                    chatMessages = emptyList()
                )
            }
            startGuestLobbySimulation()
        }
    }

    private fun startGuestLobbySimulation() {
        lobbySimulationJob?.cancel()
        lobbySimulationJob = viewModelScope.launch {
            addSystemMessage("در حال ارسال درخواست عضویت به مدیر اتاق...")
            delay(2500)
            addSystemMessage("درخواست شما پذیرفته شد! وارد اتاق شدید.")

            // Add host and some bots immediately
            val hostBot = MafiaPlayer(
                id = "host_bot",
                name = "میزبان مقتدر",
                role = Role.CITIZEN,
                isAlive = true,
                isUser = false,
                avatarId = 2
            )

            _uiState.update { state ->
                state.copy(activeRoomPlayers = state.activeRoomPlayers + hostBot)
            }

            val availableNames = botNames.shuffled()
            var nameIndex = 0

            // Fill up to 12 players
            while (_uiState.value.activeRoomId != null && _uiState.value.activeRoomPlayers.size < 12 && _uiState.value.gamePhase == GamePhase.LOBBY) {
                delay(1200)
                if (nameIndex < availableNames.size) {
                    val botPlayer = MafiaPlayer(
                        id = "bot_${UUID.randomUUID()}",
                        name = availableNames[nameIndex++],
                        role = Role.CITIZEN,
                        isAlive = true,
                        isUser = false,
                        avatarId = (3..6).random()
                    )
                    _uiState.update { state ->
                        state.copy(activeRoomPlayers = state.activeRoomPlayers + botPlayer)
                    }
                    addSystemMessage("«${botPlayer.name}» وارد اتاق شد.")

                    // Let them say hello occasionally
                    if (Random.nextFloat() < 0.45f) {
                        delay(600)
                        val greetings = listOf(
                            "سلام دوستان! خوشحالم توی جمعتون هستم 👋",
                            "سلام همگی، بازی کی شروع میشه؟",
                            "سلام! عجب اتاق خفنی.",
                            "درود رفقا! امیدوارم بازی مهیجی بشه.",
                            "سلام، من آماده‌ام!"
                        )
                        val botGreeting = ChatMessage(
                            senderName = botPlayer.name,
                            text = greetings.random(),
                            isUser = false
                        )
                        _uiState.update { state ->
                            state.copy(chatMessages = state.chatMessages + botGreeting)
                        }
                    }
                }
            }

            // Room is full! Host announces readiness
            delay(1500)
            val hostMsg = ChatMessage(
                senderName = "میزبان مقتدر",
                text = "ظرفیت اتاق تکمیل شد! بازیکنان گرامی هر زمان آماده بودید روی دکمه «شروع بازی» ضربه بزنید تا سناریوی مافیای سرخ رو آغاز کنیم. 🚀",
                isUser = false
            )
            _uiState.update { state ->
                state.copy(
                    chatMessages = state.chatMessages + hostMsg
                )
            }
        }
    }

    // Host manually starts the game
    fun startNewGameFromLobby() {
        if (_uiState.value.activeRoomPlayers.size >= 4) { // Allow starting with at least 4 for testing, up to 12
            lobbySimulationJob?.cancel()
            startActualGame()
        }
    }

    // Distribute roles for 12 players and trigger ROLE_REVEAL
    private fun startActualGame() {
        val lobbyPlayers = _uiState.value.activeRoomPlayers
        val playerCount = lobbyPlayers.size

        // Dynamically assign roles based on player count (balanced up to 12 players)
        val roles = when {
            playerCount >= 12 -> mutableListOf(
                Role.GODFATHER, Role.LEKTOR, Role.MAFIA, // 3 Mafia
                Role.DOCTOR, Role.DETECTIVE, Role.SNIPER, // 3 Special Citizens
                Role.CITIZEN, Role.CITIZEN, Role.CITIZEN, Role.CITIZEN, Role.CITIZEN, Role.CITIZEN // 6 Simple Citizens
            )
            playerCount >= 8 -> mutableListOf(
                Role.GODFATHER, Role.MAFIA, // 2 Mafia
                Role.DOCTOR, Role.DETECTIVE, Role.SNIPER, // 3 Special Citizens
                Role.CITIZEN, Role.CITIZEN, Role.CITIZEN // 3 Simple Citizens
            )
            else -> mutableListOf(
                Role.GODFATHER, // 1 Mafia
                Role.DOCTOR, Role.DETECTIVE, // 2 Special Citizens
                Role.CITIZEN, Role.CITIZEN, Role.CITIZEN, Role.CITIZEN // Citizens
            )
        }

        // Adjust roles size to match players size
        while (roles.size < playerCount) {
            roles.add(Role.CITIZEN)
        }
        while (roles.size > playerCount) {
            roles.removeAt(roles.size - 1)
        }

        roles.shuffle()

        val assignedPlayers = lobbyPlayers.mapIndexed { index, player ->
            player.copy(role = roles[index])
        }

        val userPlayer = assignedPlayers.find { it.isUser }
        val userRole = userPlayer?.role ?: Role.CITIZEN

        if (_uiState.value.isOnlineMode) {
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                val roomRef = database.getReference("rooms").child(_uiState.value.activeRoomId ?: "")
                
                // Set assigned roles on Firebase
                val playersMap = assignedPlayers.associate { player ->
                    player.id to mapOf(
                        "id" to player.id,
                        "name" to player.name,
                        "avatarId" to player.avatarId,
                        "role" to player.role.name,
                        "alive" to player.isAlive
                    )
                }
                roomRef.child("players").setValue(playersMap)
                
                roomRef.child("gamePhase").setValue("ROLE_REVEAL")
                roomRef.child("status").setValue("در حال بازی")
                roomRef.child("narrationText").setValue("بازی آغاز شد! برای مشاهده کارت نقش خود ضربه بزنید.")
                
                val msgRef = roomRef.child("chatMessages").push()
                msgRef.setValue(mapOf(
                    "senderName" to "سیستم",
                    "text" to "بازی آنلاین آغاز شد! نقش‌ها توزیع شدند.",
                    "system" to true,
                    "senderId" to "system"
                ))
            } catch (e: Exception) {
                Log.e("MafiaViewModel", "Error starting online game: ${e.message}")
            }
        } else {
            _uiState.update {
                it.copy(
                    players = assignedPlayers,
                    currentRole = userRole,
                    gamePhase = GamePhase.ROLE_REVEAL,
                    dayNumber = 1,
                    chatMessages = listOf(
                        ChatMessage("سیستم", "بازی مافیای سرخ دوازده نفره آغاز شد! نقش‌ها توزیع شدند.", isSystem = true)
                    ),
                    winningFaction = null,
                    narrationText = "",
                    lastNightKilled = null,
                    lastNightSaved = false,
                    sniperBulletsRemaining = if (userRole == Role.SNIPER) 2 else 0
                )
            }
        }
    }

    fun leaveRoom() {
        val roomId = _uiState.value.activeRoomId
        lobbySimulationJob?.cancel()

        if (_uiState.value.isOnlineMode && roomId != null) {
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                database.getReference("rooms").child(roomId).child("players").child(userId).removeValue()
                
                val nickname = _uiState.value.userNickname
                val msgRef = database.getReference("rooms").child(roomId).child("chatMessages").push()
                msgRef.setValue(mapOf(
                    "senderName" to "سیستم",
                    "text" to "«$nickname» از اتاق خارج شد.",
                    "system" to true,
                    "senderId" to "system"
                ))
                stopActiveRoomFirebaseListener(roomId)
            } catch (e: Exception) {
                Log.e("MafiaViewModel", "Error leaving online room: ${e.message}")
            }
        }

        _uiState.update {
            it.copy(
                activeRoomId = null,
                activeRoomPlayers = emptyList(),
                joinRequests = emptyList(),
                gamePhase = GamePhase.LOBBY
            )
        }
    }

    fun proceedToNight() {
        val userRole = _uiState.value.currentRole
        // Determine starting phase of the night based on user role to handle custom user screen first
        when (userRole) {
            Role.GODFATHER, Role.MAFIA, Role.LEKTOR -> {
                _uiState.update { it.copy(gamePhase = GamePhase.NIGHT_MAFIA) }
            }
            Role.DETECTIVE -> {
                _uiState.update { it.copy(gamePhase = GamePhase.NIGHT_DETECTIVE) }
            }
            Role.DOCTOR -> {
                _uiState.update { it.copy(gamePhase = GamePhase.NIGHT_DOCTOR) }
            }
            else -> {
                // Citizen or Sniper: proceed with automated bot actions in order, then day summary
                simulateAutomatedNightPhases(null, null, null)
            }
        }
    }

    // User shoots a player
    fun performUserMafiaAction(targetPlayerId: String) {
        val userRole = _uiState.value.currentRole
        val target = _uiState.value.players.find { it.id == targetPlayerId } ?: return

        addSystemMessage("اسلحه مافیا شلیک شد به سمت: ${target.name}")

        if (userRole == Role.DETECTIVE) {
            goToDoctorNightPhase(targetPlayerId, null)
        } else {
            val hasDetective = _uiState.value.players.any { it.role == Role.DETECTIVE && it.isAlive }
            if (hasDetective) {
                _uiState.update { it.copy(gamePhase = GamePhase.NIGHT_DETECTIVE) }
            } else {
                goToDoctorNightPhase(targetPlayerId, null)
            }
        }
    }

    // User inquiry
    fun performUserDetectiveAction(targetPlayerId: String) {
        val target = _uiState.value.players.find { it.id == targetPlayerId } ?: return
        val resultText = if (target.role == Role.GODFATHER) {
            "شهروند بی‌گناه 🟢 (پدرخوانده استعلامش منفی است!)"
        } else if (target.role.faction == Faction.MAFIA) {
            "مافیای سرخ 🔴 (استعلام مثبت)"
        } else {
            "شهروند بی‌گناه 🟢"
        }

        _uiState.update {
            it.copy(detectiveInquiryResult = "استعلام شما برای ${target.name}: $resultText")
        }

        addSystemMessage("کارآگاه استعلام ${target.name} را گرفت.")

        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(detectiveInquiryResult = null) }
            goToDoctorNightPhase(null, targetPlayerId)
        }
    }

    private fun goToDoctorNightPhase(mafiaTarget: String?, detectiveTarget: String?) {
        val hasDoctor = _uiState.value.players.any { it.role == Role.DOCTOR && it.isAlive }
        if (hasDoctor) {
            _uiState.update { it.copy(gamePhase = GamePhase.NIGHT_DOCTOR) }
        } else {
            simulateAutomatedNightPhases(mafiaTarget, detectiveTarget, null)
        }
    }

    // User doctor saves
    fun performUserDoctorAction(targetPlayerId: String) {
        val target = _uiState.value.players.find { it.id == targetPlayerId } ?: return
        addSystemMessage("دکتر تصمیم گرفت نجات دهد: ${target.name}")
        simulateAutomatedNightPhases(null, null, targetPlayerId)
    }

    private fun simulateAutomatedNightPhases(
        userMafiaTarget: String?,
        userDetectiveTarget: String?,
        userDoctorTarget: String?
    ) {
        _uiState.update { it.copy(isNarratorLoading = true) }

        viewModelScope.launch {
            delay(1500)
            val currentState = _uiState.value
            val alivePlayers = currentState.players.filter { it.isAlive }

            // 1. Mafia Target
            val mafiaTargetId = userMafiaTarget ?: run {
                val citizens = alivePlayers.filter { it.role.faction == Faction.CITIZENS }
                if (citizens.isNotEmpty()) citizens.random().id else alivePlayers.random().id
            }

            // 2. Doctor Target
            val doctorTargetId = userDoctorTarget ?: run {
                val doctor = alivePlayers.find { it.role == Role.DOCTOR }
                if (doctor != null) {
                    if (Random.nextFloat() < 0.5f) doctor.id else alivePlayers.random().id
                } else null
            }

            val victim = currentState.players.find { it.id == mafiaTargetId }
            val wasSaved = mafiaTargetId == doctorTargetId

            var killedPlayerName: String? = null
            val updatedPlayers = currentState.players.map { player ->
                if (player.id == mafiaTargetId && !wasSaved) {
                    killedPlayerName = player.name
                    player.copy(isAlive = false)
                } else {
                    player
                }
            }

            val narration = getGeminiDayNarration(killedPlayerName, wasSaved, currentState.dayNumber)

            _uiState.update {
                it.copy(
                    players = updatedPlayers,
                    narrationText = narration,
                    lastNightKilled = killedPlayerName,
                    lastNightSaved = wasSaved,
                    isNarratorLoading = false,
                    gamePhase = GamePhase.DAY_SUMMARY
                )
            }

            addSystemMessage("صبح روز ${currentState.dayNumber} آغاز شد.")
        }
    }

    private suspend fun getGeminiDayNarration(
        victimName: String?,
        wasSaved: Boolean,
        dayNumber: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        val hasKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!hasKey) {
            return@withContext getPersianNarrationFallback(victimName, wasSaved, dayNumber)
        }

        try {
            val systemInstruction = "شما گرداننده بازی مافیا (راوی سناریو) هستید. لحن حماسی، ادبی، دراماتیک و با ابهت به زبان فارسی."
            val prompt = """
                یک روایت حماسی و ادبی بسیار کوتاه (حداکثر ۲ یا ۳ جمله کوتاه) به زبان فارسی درباره وقایع دیشب (شب $dayNumber) بنویسید.
                وقایع دیشب:
                ${if (victimName != null) "مافیا شلیک کرد و «$victimName» حذف شد." else "شلیکی انجام شد اما کسی کشته نشد."}
                ${if (wasSaved) "پزشک با بیداری و درایت قربانی را نجات داد." else "پزشک نتوانست قربانی را مداوا کند."}
                
                فقط متن فارسی به صورت مستقیم و بدون پیشوند خروجی دهید. از نام بازیکنان دقیقاً استفاده کنید.
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", systemInstruction)
                    }))
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.75)
                    put("maxOutputTokens", 150)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext getPersianNarrationFallback(victimName, wasSaved, dayNumber)
                val responseBodyStr = response.body?.string() ?: ""
                val text = JSONObject(responseBodyStr).getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                return@withContext text.trim()
            }
        } catch (e: Exception) {
            return@withContext getPersianNarrationFallback(victimName, wasSaved, dayNumber)
        }
    }

    private fun getPersianNarrationFallback(victimName: String?, wasSaved: Boolean, dayNumber: Int): String {
        return if (wasSaved) {
            "شهر دیشب در بیداری مطلق پزشک جان سالم به در برد. تفنگ‌های سربی مافیا شلیک شدند اما دستان پر مهر نجات‌دهنده دکتر التیامی بر سینه قربانی شد. صبح بی‌خونی را آغاز می‌کنیم..."
        } else {
            "سکوتی سنگین بر پیکر شهر حاکم بود تا اینکه تک‌گلوله سربی سکوت را شکافت. با کمال تاسف، صبح امروز با پیکر بی‌جان «${victimName ?: "یکی از شهروندان"}» روبرو شدیم. او قربانی فتنه‌انگیزی مافیا شد..."
        }
    }

    // 3. Speaking Turn-based Phase
    fun startSpeakingPhase() {
        _uiState.update { it.copy(gamePhase = GamePhase.DAY_SPEAKING) }
        speakingJob?.cancel()

        speakingJob = viewModelScope.launch {
            val alivePlayers = _uiState.value.players.filter { it.isAlive }
            
            for (player in alivePlayers) {
                // Set active speaker
                val botSpeechText = if (!player.isUser) getBotSpeakingText(player) else ""
                _uiState.update {
                    it.copy(
                        currentSpeakingPlayerId = player.id,
                        speakingTimerSeconds = 15,
                        isUserMicEnabled = false,
                        currentSpeakerText = botSpeechText
                    )
                }

                // Periodically make other bots send reaction emojis during this speech
                val reactionJob = launch {
                    while (true) {
                        delay(Random.nextLong(2000, 4500))
                        val otherAliveBots = _uiState.value.players.filter { !it.isUser && it.isAlive && it.id != player.id }
                        if (otherAliveBots.isNotEmpty()) {
                            val reactingBot = otherAliveBots.random()
                            val possibleEmojis = listOf("👍", "👎", "🤫", "😮", "😠", "💀", "👏")
                            val reactionEmoji = possibleEmojis.random()
                            triggerReaction(reactingBot.id, reactionEmoji)
                        }
                    }
                }

                // Speaking Countdown (15 seconds)
                while (_uiState.value.speakingTimerSeconds > 0 && _uiState.value.currentSpeakingPlayerId == player.id) {
                    delay(1000)
                    _uiState.update { it.copy(speakingTimerSeconds = it.speakingTimerSeconds - 1) }
                }

                reactionJob.cancel()
            }

            // All alive players finished speaking! Proceed to unified voting
            addSystemMessage("نوبت صحبت تمام اعضای زنده پایان یافت. وارد مرحله رأی‌گیری سراسری می‌شویم...")
            delay(1500)
            proceedToVoting()
        }
    }

    private fun getBotSpeakingText(bot: MafiaPlayer): String {
        val alivePlayers = _uiState.value.players.filter { it.isAlive && it.id != bot.id }
        val suspect = if (alivePlayers.isNotEmpty()) alivePlayers.random().name else "کسی"
        return when (bot.role) {
            Role.GODFATHER, Role.MAFIA, Role.LEKTOR -> {
                listOf(
                    "من صد در صد شهروندم و دارم کاملاً سفید بازی می‌کنم. تارگت‌های روی من کار مافیاست.",
                    "من نقشم بی‌تاثیر در شبه ولی روز بسیار مفیدم. به نظرم «$suspect» لحنش مشکوکه.",
                    "بچه‌ها کورکورانه رأی ندید. به نظر من «$suspect» داره فرافکنی می‌کنه تا خودش رو نجات بده.",
                    "من دیروز سکوت کردم تا روند بازی رو ببینم. اتهام من امروز به «$suspect» هست."
                ).random()
            }
            Role.DETECTIVE -> {
                listOf(
                    "بچه‌ها من کارآگاه شهرم. بیدار باشید که امروز استعلام یکی برام مهم بود. «$suspect» به نظرم مافیاست.",
                    "من هنوز تارگت صد در صدی ندارم ولی رفتارهای «$suspect» اصلاً شهروندی نیست.",
                    "من به دلسوزی شهر ایمان دارم. بیایید امروز روی «$suspect» تمرکز کنیم چون سابقه رأیش خرابه."
                ).random()
            }
            Role.DOCTOR -> {
                listOf(
                    "دیشب نجات موفقی داشتیم و این نشون میده که کار شهر درسته. به نظرم «$suspect» مشکوک بازی می‌کنه.",
                    "من حامی شهرم و به عنوان دکتر خیالتون راحت باشه. به نظر من امروز باید به «$suspect» اتهام جدی بزنیم."
                ).random()
            }
            Role.SNIPER -> {
                listOf(
                    "من تفنگ شهروندم و ترسی ندارم. اگر امروز «$suspect» بره بیرون خیلی از مجهولات حل میشه.",
                    "من با قدرت تو بازی می‌مونم. شلیک دیشب نشون داد مافیا کجاست. به نظر من «$suspect» نقش بازی می‌کنه."
                ).random()
            }
            else -> {
                listOf(
                    "من یک شهروند کاملاً ساده و مخلصم. تارگت من روی «$suspect» هست چون فرار رو به جلو می‌کنه.",
                    "بیایید روی فکت بازی کنیم. «$suspect» دیروز اصرار عجیبی روی رأی دادن داشت.",
                    "من حسم دروغ نمیگه. «$suspect» لحن صحبتش با روزهای قبل فرق کرده."
                ).random()
            }
        }
    }

    // Pass turn prematurely
    fun passSpeakingTurn() {
        _uiState.update { it.copy(speakingTimerSeconds = 0) }
    }

    // User toggles mic
    fun setUserMicEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isUserMicEnabled = enabled) }
    }

    // Reaction triggering (both for user and bots)
    fun sendReactionFromUser(emoji: String) {
        triggerReaction(userId, emoji)
    }

    private fun triggerReaction(senderId: String, emoji: String) {
        val newReaction = ReactionEvent(playerId = senderId, emoji = emoji)
        _uiState.update { state ->
            state.copy(activeReactions = state.activeReactions + newReaction)
        }
    }

    // Unified voting (sequential candidate-by-candidate)
    fun proceedToVoting() {
        speakingJob?.cancel()
        votingJob?.cancel()
        courtSpeakingJob?.cancel()
        
        val alivePlayers = _uiState.value.players.filter { it.isAlive }
        val initialVotes = alivePlayers.associate { it.id to 0 }

        _uiState.update {
            it.copy(
                gamePhase = GamePhase.DAY_VOTING,
                votes = initialVotes,
                nomineeVotes = emptyMap(),
                currentNomineePlayerId = null,
                userSelectedVoteId = null,
                currentSpeakingPlayerId = null
            )
        }

        votingJob = viewModelScope.launch {
            addSystemMessage("🗳️ نوبت‌دهی و رأی‌گیری انفرادی آغاز شد. بازیکنان زنده تک به تک نامزد رأی‌گیری می‌شوند:")
            delay(1200)

            for (nominee in alivePlayers) {
                val currentNomineeId = nominee.id
                _uiState.update {
                    it.copy(
                        currentNomineePlayerId = currentNomineeId,
                        userSelectedVoteId = null,
                        votingTimerSeconds = 8
                    )
                }

                addSystemMessage("⚖️ رأی‌گیری برای بازیکن «${nominee.name}». آیا او متهم است؟")

                // Staggered bot decisions for this candidate
                val aliveBots = _uiState.value.players.filter { !it.isUser && it.isAlive && it.id != currentNomineeId }
                launch {
                    aliveBots.forEach { bot ->
                        delay(Random.nextLong(300, 4800))
                        val stateNow = _uiState.value
                        if (stateNow.currentNomineePlayerId == currentNomineeId && stateNow.gamePhase == GamePhase.DAY_VOTING) {
                            val shouldVoteYes = decideBotNomineeVote(bot, nominee)
                            if (shouldVoteYes) {
                                registerNomineeVote(bot.id, currentNomineeId)
                            }
                        }
                    }
                }

                // Countdown loop
                while (_uiState.value.votingTimerSeconds > 0 && _uiState.value.currentNomineePlayerId == currentNomineeId) {
                    delay(1000)
                    _uiState.update { it.copy(votingTimerSeconds = it.votingTimerSeconds - 1) }
                }

                delay(600)
            }

            proceedToTrial()
        }
    }

    private fun decideBotNomineeVote(bot: MafiaPlayer, nominee: MafiaPlayer): Boolean {
        val botFaction = bot.role.faction
        val nomineeFaction = nominee.role.faction
        val probability = when {
            botFaction == Faction.MAFIA && nomineeFaction == Faction.CITIZENS -> 0.65f
            botFaction == Faction.CITIZENS && nomineeFaction == Faction.MAFIA -> 0.50f
            botFaction == Faction.MAFIA && nomineeFaction == Faction.MAFIA -> 0.12f
            else -> 0.25f
        }
        return Random.nextFloat() < probability
    }

    private fun registerNomineeVote(voterId: String, nomineeId: String) {
        _uiState.update { state ->
            val currentList = state.nomineeVotes[nomineeId] ?: emptyList()
            if (voterId !in currentList) {
                val updatedList = currentList + voterId
                val updatedNomineeVotes = state.nomineeVotes + (nomineeId to updatedList)
                
                val updatedVotes = state.votes.toMutableMap()
                updatedVotes[nomineeId] = updatedList.size

                val voterName = state.players.find { it.id == voterId }?.name ?: ""
                val nomineeName = state.players.find { it.id == nomineeId }?.name ?: ""
                if (voterName.isNotEmpty() && nomineeName.isNotEmpty() && voterId != userId) {
                    // Log in system messaging briefly
                }

                state.copy(
                    nomineeVotes = updatedNomineeVotes,
                    votes = updatedVotes
                )
            } else {
                state
            }
        }
        recalculateVoteLeader()
    }

    fun castUserVote(nomineeId: String) {
        val currentState = _uiState.value
        if (currentState.currentNomineePlayerId != nomineeId) return
        if (currentState.userSelectedVoteId != null) return

        _uiState.update {
            it.copy(userSelectedVoteId = nomineeId)
        }
        registerNomineeVote(userId, nomineeId)
    }

    fun skipVotingTimer() {
        // Skip current nominee's timer or proceed to next nominee
        _uiState.update { it.copy(votingTimerSeconds = 0) }
    }

    private fun recalculateVoteLeader() {
        val currentState = _uiState.value
        val highestVote = currentState.votes.maxByOrNull { it.value }

        if (highestVote != null && highestVote.value > 0) {
            val leadingPlayer = currentState.players.find { it.id == highestVote.key }
            _uiState.update {
                it.copy(currentVoteLeader = leadingPlayer?.name)
            }
        }
    }

    fun proceedToTrial() {
        votingJob?.cancel()
        val currentState = _uiState.value
        val alivePlayers = currentState.players.filter { it.isAlive }
        val aliveCount = alivePlayers.size
        val halfVotes = aliveCount / 2.0

        // Find players who received > 50% of votes (above 50% of alive players)
        val accusedPlayers = alivePlayers.filter { player ->
            val voteCount = currentState.votes[player.id] ?: 0
            voteCount > halfVotes
        }

        if (accusedPlayers.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    gamePhase = GamePhase.DAY_TRIAL,
                    courtPlayers = accusedPlayers,
                    courtPhase = CourtPhase.SPEECHES,
                    courtSpeakingPlayerId = null,
                    courtVotes = emptyMap(),
                    userTrialVotes = emptyMap()
                )
            }
            addSystemMessage("⚖️ دادگاه تشکیل شد! متهمانی که بالای ۵۰٪ رأی آوردند: ${accusedPlayers.joinToString("، ") { it.name }}")
            startCourtSpeakingPhase()
        } else {
            val highestVote = currentState.votes.maxByOrNull { it.value }
            val leadingPlayer = highestVote?.key?.let { id -> currentState.players.find { it.id == id } }
            
            if (leadingPlayer != null && highestVote.value > 0) {
                // Fallback: If no one is above 50% but we have a single clear leader, let's put them in court
                // so the game is still highly engaging and doesn't get stuck!
                _uiState.update {
                    it.copy(
                        gamePhase = GamePhase.DAY_TRIAL,
                        courtPlayers = listOf(leadingPlayer),
                        courtPhase = CourtPhase.SPEECHES,
                        courtSpeakingPlayerId = null,
                        courtVotes = emptyMap(),
                        userTrialVotes = emptyMap()
                    )
                }
                addSystemMessage("هیچ‌کس بالای ۵۰٪ رأی نیاورد، اما بازیکن «${leadingPlayer.name}» با بیشترین رأی تک‌نفره (${highestVote.value.toPersianDigits()} رأی) به دادگاه فراخوانده شد.")
                startCourtSpeakingPhase()
            } else {
                addSystemMessage("آراء برابر بود یا کسی رأی نیاورد. دادگاهی تشکیل نشد. شهر دوباره به خواب می‌رود.")
                viewModelScope.launch {
                    delay(2000)
                    goToNextNight()
                }
            }
        }
    }

    // Court Speech Round
    fun startCourtSpeakingPhase() {
        courtSpeakingJob?.cancel()
        courtSpeakingJob = viewModelScope.launch {
            _uiState.update { it.copy(courtPhase = CourtPhase.SPEECHES) }
            val accused = _uiState.value.courtPlayers

            for (player in accused) {
                val botSpeechText = if (!player.isUser) getBotCourtSpeakingText(player) else ""
                _uiState.update {
                    it.copy(
                        courtSpeakingPlayerId = player.id,
                        courtSpeakingTimerSeconds = 15,
                        courtSpeakerText = botSpeechText
                    )
                }

                // Speaking Countdown (15 seconds)
                while (_uiState.value.courtSpeakingTimerSeconds > 0 && _uiState.value.courtSpeakingPlayerId == player.id) {
                    delay(1000)
                    _uiState.update { it.copy(courtSpeakingTimerSeconds = it.courtSpeakingTimerSeconds - 1) }
                }
            }

            addSystemMessage("نوبت دفاع متهمان دادگاه پایان یافت. وارد مرحله رأی‌گیری نهایی دادگاه برای اخراج می‌شویم...")
            delay(1500)
            startCourtVotingPhase()
        }
    }

    private fun getBotCourtSpeakingText(player: MafiaPlayer): String {
        return when (player.role.faction) {
            Faction.MAFIA -> listOf(
                "من بیگناهم! باور کنید من شهروندم و مافیا دارن من رو قربانی می‌کنن تا فرار کنند.",
                "رأی به خروج من بزرگترین اشتباه شهره. اگر من برم بیرون شهروند سوخته.",
                "من حاضرم استعلام نقش من گرفته بشه. خواهش می‌کنم تبرئه‌ام کنید."
            ).random()
            Faction.CITIZENS -> listOf(
                "بچه‌ها من شهروندم، فکت‌های من علیه مافیا درسته. به من رأی خروج ندید.",
                "اگر من حذف بشم توانایی‌های شهر کم میشه. اشتباه رأی‌گیری رو تکرار نکنید.",
                "من کارآمدترین عضو شهرم و دلسوزانه بازی می‌کنم. تبرئه کنید تا مافیا رو پیدا کنیم."
            ).random()
        }
    }

    fun passCourtSpeakingTurn() {
        _uiState.update { it.copy(courtSpeakingTimerSeconds = 0) }
    }

    // Court Voting Round
    fun startCourtVotingPhase() {
        courtSpeakingJob?.cancel()
        _uiState.update {
            it.copy(
                courtPhase = CourtPhase.VOTING,
                courtVotes = emptyMap(),
                userTrialVotes = emptyMap()
            )
        }

        viewModelScope.launch {
            delay(1000)
            val currentState = _uiState.value
            val aliveBots = currentState.players.filter { !it.isUser && it.isAlive }
            val accusedIds = currentState.courtPlayers.map { it.id }

            val updatedVotes = currentState.courtVotes.toMutableMap()

            // Bots cast their voting decisions to Eliminate (Yes) or Spare (No) for each accused player
            accusedIds.forEach { accusedId ->
                val votersWhoVotedYes = mutableListOf<String>()
                
                aliveBots.forEach { bot ->
                    if (bot.id != accusedId) {
                        val botPlayer = currentState.players.find { it.id == bot.id }
                        val accusedPlayer = currentState.players.find { it.id == accusedId }
                        
                        val probability = if (botPlayer?.role?.faction == Faction.MAFIA && accusedPlayer?.role?.faction == Faction.CITIZENS) {
                            0.75f
                        } else if (botPlayer?.role?.faction == Faction.CITIZENS && accusedPlayer?.role?.faction == Faction.MAFIA) {
                            0.65f
                        } else {
                            0.45f
                        }

                        if (Random.nextFloat() < probability) {
                            votersWhoVotedYes.add(bot.id)
                        }
                    }
                }
                updatedVotes[accusedId] = votersWhoVotedYes
            }

            _uiState.update {
                it.copy(courtVotes = updatedVotes)
            }
        }
    }

    fun castUserCourtVote(courtPlayerId: String, shouldEliminate: Boolean) {
        val currentState = _uiState.value
        val currentVotesList = currentState.courtVotes[courtPlayerId]?.toMutableList() ?: mutableListOf()
        
        if (shouldEliminate) {
            if (userId !in currentVotesList) {
                currentVotesList.add(userId)
            }
        } else {
            currentVotesList.remove(userId)
        }
        
        val updatedCourtVotes = currentState.courtVotes.toMutableMap()
        updatedCourtVotes[courtPlayerId] = currentVotesList
        
        val updatedUserTrialVotes = currentState.userTrialVotes.toMutableMap()
        updatedUserTrialVotes[courtPlayerId] = shouldEliminate
        
        _uiState.update {
            it.copy(
                courtVotes = updatedCourtVotes,
                userTrialVotes = updatedUserTrialVotes
            )
        }
    }

    fun resolveCourtVoting() {
        val currentState = _uiState.value
        val alivePlayers = currentState.players.filter { it.isAlive }
        val aliveCount = alivePlayers.size
        val threshold = aliveCount / 2.0

        val eliminatedNames = mutableListOf<String>()
        val sparedNames = mutableListOf<String>()

        val updatedPlayers = currentState.players.map { player ->
            if (player.id in currentState.courtPlayers.map { it.id }) {
                val yesVotesList = currentState.courtVotes[player.id] ?: emptyList()
                val totalYesVotes = yesVotesList.size

                if (totalYesVotes > threshold) {
                    eliminatedNames.add("«${player.name}» (نقش: ${player.role.title})")
                    player.copy(isAlive = false)
                } else {
                    sparedNames.add(player.name)
                    player
                }
            } else {
                player
            }
        }

        _uiState.update {
            it.copy(
                players = updatedPlayers,
                courtPhase = CourtPhase.RESULTS
            )
        }

        if (eliminatedNames.isNotEmpty()) {
            addSystemMessage("⚖️ دادگاه پایان یافت. بازیکنان اخراج شده (بالای ۵۰٪ رأی اعدام): ${eliminatedNames.joinToString("، ")}")
        }
        if (sparedNames.isNotEmpty()) {
            addSystemMessage("بازیکنان تبرئه شده (زیر ۵۰٪ رأی اعدام): ${sparedNames.joinToString("، ")}")
        }
        if (eliminatedNames.isEmpty()) {
            addSystemMessage("هیچ‌کدام از متهمان بیش از ۵۰ درصد رأی اعدام نیاوردند و همگی در بازی باقی ماندند.")
        }

        viewModelScope.launch {
            delay(4000)
            checkGameEndConditions()
        }
    }

    // Dummy backward-compatible method for old button triggers
    fun resolveTrial(shouldEliminate: Boolean) {
        resolveCourtVoting()
    }

    private fun checkGameEndConditions() {
        val currentState = _uiState.value
        val alivePlayers = currentState.players.filter { it.isAlive }
        val aliveMafia = alivePlayers.count { it.role.faction == Faction.MAFIA }
        val aliveCitizens = alivePlayers.count { it.role.faction == Faction.CITIZENS }

        if (aliveMafia == 0) {
            endGame(Faction.CITIZENS)
        } else if (aliveMafia >= aliveCitizens) {
            endGame(Faction.MAFIA)
        } else {
            goToNextNight()
        }
    }

    private fun goToNextNight() {
        _uiState.update {
            it.copy(
                dayNumber = it.dayNumber + 1,
                userSelectedVoteId = null,
                currentVoteLeader = null,
                currentTrialPlayer = null,
                votes = emptyMap()
            )
        }
        proceedToNight()
    }

    private fun endGame(winningFaction: Faction) {
        val currentState = _uiState.value
        val userStatus = if (currentState.currentRole.faction == winningFaction) "برنده 🏆" else "بازنده 💀"
        val winnerPersian = winningFaction.persianTitle

        _uiState.update {
            it.copy(
                gamePhase = GamePhase.GAME_OVER,
                winningFaction = winningFaction
            )
        }

        viewModelScope.launch {
            repository.insertMatch(
                MatchRecord(
                    playerX = currentState.userNickname,
                    playerO = winnerPersian,
                    winner = if (currentState.currentRole.faction == winningFaction) "X" else "O",
                    mode = "مافیای سرخ ۱۲ نفره"
                )
            )
        }
    }

    fun restartGame() {
        speakingJob?.cancel()
        lobbySimulationJob?.cancel()
        votingJob?.cancel()
        courtSpeakingJob?.cancel()
        val randomCode = (1000..9999).random().toString()
        _uiState.update {
            it.copy(
                gamePhase = GamePhase.LOBBY,
                activeRoomId = null,
                players = emptyList(),
                activeRoomPlayers = emptyList(),
                joinRequests = emptyList(),
                chatMessages = emptyList(),
                narrationText = "",
                winningFaction = null,
                currentTrialPlayer = null,
                roomCode = randomCode,
                availableRooms = generateSampleRooms()
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun proceedFromRoleRevealToDay() {
        addSystemMessage("معارفه پایان یافت. صبح روز اول آغاز شد! نوبت صحبت‌های بازیکنان است.")
        _uiState.update {
            it.copy(
                gamePhase = GamePhase.DAY_SPEAKING,
                dayNumber = 1,
                currentSpeakingPlayerId = null
            )
        }
        startSpeakingPhase()
    }

    fun sendUserChatMessage(text: String) {
        if (text.isBlank()) return
        val userNickname = _uiState.value.userNickname
        val roomId = _uiState.value.activeRoomId

        if (_uiState.value.isOnlineMode && roomId != null) {
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                val msgRef = database.getReference("rooms").child(roomId).child("chatMessages").push()
                val msgData = mapOf(
                    "senderName" to userNickname,
                    "text" to text,
                    "system" to false,
                    "senderId" to userId
                )
                msgRef.setValue(msgData)
            } catch (e: Exception) {
                Log.e("MafiaViewModel", "Error sending online chat: ${e.message}")
            }
        } else {
            val userMsg = ChatMessage(senderName = userNickname, text = text, isUser = true)
            _uiState.update { it.copy(chatMessages = it.chatMessages + userMsg) }

            // Simulate random bot responding
            viewModelScope.launch {
                delay(Random.nextLong(1000, 2500))
                val currentPlayers = _uiState.value.activeRoomPlayers.filter { !it.isUser }
                if (currentPlayers.isNotEmpty() && _uiState.value.gamePhase == GamePhase.LOBBY) {
                    val responder = currentPlayers.random()
                    val responses = listOf(
                        "سلام به همگی، بازی کی شروع میشه؟ 👋",
                        "امیدوارم این دست شهروند کاربلد بشم و مافیا رو ببرم!",
                        "به به! همگی خوش اومدین.",
                        "بچه‌ها حتماً آماده باشین که بازی مهیجی در پیش داریم.",
                        "راوی جان کارت‌ها رو کی پخش می‌کنی؟ 😁",
                        "من که این دست مافیا باشم همه رو شات می‌کنم! 😂",
                        "امیدوارم بازی جذابی بشه.",
                        "بیاین شروع کنیم دیگر، همگی آماده‌اید؟",
                        "سلام ${userNickname}! خوش اومدی به جمع ما."
                    )
                    val botMsg = ChatMessage(senderName = responder.name, text = responses.random(), isUser = false)
                    _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg) }
                }
            }
        }
    }

    private fun addSystemMessage(text: String) {
        val sysMsg = ChatMessage("سیستم", text, isSystem = true)
        _uiState.update { it.copy(chatMessages = it.chatMessages + sysMsg) }
    }

    override fun onCleared() {
        super.onCleared()
        speakingJob?.cancel()
        lobbySimulationJob?.cancel()
        votingJob?.cancel()
        courtSpeakingJob?.cancel()
    }
}

class MafiaViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MafiaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MafiaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
