package com.example.idleprogrammergame.game_logic

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LeaderboardEntry(val username: String, val score: Double)

@Serializable
data class LeaderboardResponse(
    val topEarnings: List<LeaderboardEntry>,
    val topRebirths: List<LeaderboardEntry>,
    val topVentures: List<LeaderboardEntry>
)

class NetworkManager {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    private val baseUrl = "http://eu.difuser.online:8080"

    suspend fun saveGame(data: PlayerData): Boolean {
        return try {
            val response = client.post("$baseUrl/save") {
                contentType(ContentType.Application.Json)
                setBody(data)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loadGame(username: String): PlayerData? {
        return try {
            val response = client.get("$baseUrl/load") {
                parameter("username", username)
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<PlayerData>()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getLeaderboard(): LeaderboardResponse? {
        return try {
            val response = client.get("$baseUrl/leaderboard")
            if (response.status == HttpStatusCode.OK) {
                response.body<LeaderboardResponse>()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
