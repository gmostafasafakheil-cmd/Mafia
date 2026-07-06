package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val matchDao: MatchDao,
    private val context: Context
) {
    val allMatches: Flow<List<MatchRecord>> = matchDao.getAllMatches()

    private val prefs = context.getSharedPreferences("mafia_user_prefs", Context.MODE_PRIVATE)

    suspend fun insertMatch(match: MatchRecord) {
        matchDao.insertMatch(match)
    }

    suspend fun clearHistory() {
        matchDao.clearAllMatches()
    }

    fun saveUserProfile(nickname: String, avatarId: Int, bio: String) {
        prefs.edit().apply {
            putString("user_nickname", nickname)
            putInt("profile_avatar_id", avatarId)
            putString("profile_bio", bio)
            apply()
        }
    }

    fun getSavedNickname(default: String): String {
        return prefs.getString("user_nickname", default) ?: default
    }

    fun getSavedAvatarId(default: Int): Int {
        return prefs.getInt("profile_avatar_id", default)
    }

    fun getSavedBio(default: String): String {
        return prefs.getString("profile_bio", default) ?: default
    }

    fun getUserId(): String {
        var id = prefs.getString("user_id", null)
        if (id == null) {
            id = "user_${java.util.UUID.randomUUID().toString().take(6)}"
            prefs.edit().putString("user_id", id).apply()
        }
        return id
    }
}
