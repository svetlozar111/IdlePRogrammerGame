package com.example.idleprogrammergame.game_logic

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

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

    // Change this to your server's IP/domain
    private val baseUrl = "http://eu.difuser.online:8080"

    suspend fun saveGame(data: PlayerData): Boolean {
        println("📤 Sending save to server for ${data.username}")

        return try {
            val response = client.post("$baseUrl/save") {
                contentType(ContentType.Application.Json)
                setBody(data)
            }

            println("📡 Save response: ${response.status}")

            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            println("❌ Save failed")
            e.printStackTrace()
            false
        }
    }

    suspend fun loadGame(username: String): PlayerData? {
        println("📤 Requesting load for $username")

        return try {
            val response = client.get("$baseUrl/load") {
                parameter("username", username)
            }

            println("📡 Load response: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val data = response.body<PlayerData>()
                println("✅ Loaded player ${data.username}")
                data
            } else {
                println("⚠️ Player not found")
                null
            }

        } catch (e: Exception) {
            println("❌ Load failed")
            e.printStackTrace()
            null
        }
    }
}
