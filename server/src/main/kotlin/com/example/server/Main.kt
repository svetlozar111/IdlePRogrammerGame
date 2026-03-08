package com.example.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.event.Level
import java.sql.Connection

@Serializable
data class VentureSaveData(
    val id: String,
    val count: Int,
    val isAutomated: Boolean
)

@Serializable
data class HireSaveData(
    val id: String,
    val isHired: Boolean
)

@Serializable
data class UpgradeSaveData(
    val id: String,
    val isPurchased: Boolean
)

@Serializable
data class SkillSaveData(
    val id: String,
    val level: Int,
    val cost: Long
)

@Serializable
data class PlayerData(
    val username: String,
    val balance: Double,
    val lifetimeEarnings: Double,
    val experiencePoints: Long,
    val ventures: List<VentureSaveData>,
    val hires: List<HireSaveData>,
    val upgrades: List<UpgradeSaveData>,
    val skills: List<SkillSaveData>,
    val lastSaveTime: Long = System.currentTimeMillis()
)

fun main() {
    // 2. Standard Application Connection
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://eu.difuser.online:5432/idleprogrammer"
        username = "gameuser"
        password = "0108021060sS"
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }

    val dataSource = HikariDataSource(hikariConfig)

    // Run database initialization (Tables)
    initDatabase(dataSource)

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallLogging) {
            level = Level.INFO
        }
        install(ContentNegotiation) {
            json(json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        routing {
            get("/") {
                call.respondText("Idle Programmer Server is running!")
            }

            post("/save") {
                println("📥 SAVE request received")

                try {
                    val data = call.receive<PlayerData>()
                    println("💾 Saving player: ${data.username}")
                    println("Balance: ${data.balance}")
                    println("Ventures: ${data.ventures.size}")

                    dataSource.connection.use { conn ->
                        try {
                            savePlayerData(conn, data)
                            conn.commit()

                            println("✅ Save successful for ${data.username}")

                            call.respond(mapOf("status" to "saved"))
                        } catch (e: Exception) {
                            conn.rollback()
                            e.printStackTrace()
                            println("❌ Save failed: ${e.message}")

                            call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to e.localizedMessage)
                            )
                        }
                    }

                } catch (e: Exception) {
                    println("❌ Invalid save request: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            get("/load") {
                val username = call.request.queryParameters["username"]

                println("📤 LOAD request for: $username")

                if (username.isNullOrBlank()) {
                    println("❌ Missing username")
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                dataSource.connection.use { conn ->
                    val data = loadPlayerData(conn, username)

                    if (data != null) {
                        println("✅ Player loaded: $username")
                        call.respond(data)
                    } else {
                        println("⚠️ Player not found: $username")
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }.start(wait = true)
}

fun initDatabase(dataSource: HikariDataSource) {
    dataSource.connection.use { conn ->
        val queries = listOf(
            """
            CREATE TABLE IF NOT EXISTS players (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                balance DOUBLE PRECISION DEFAULT 0.0,
                lifetime_earnings DOUBLE PRECISION DEFAULT 0.0,
                experience_points BIGINT DEFAULT 0,
                last_save_time BIGINT
            );
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS player_ventures (
                player_id INTEGER REFERENCES players(id) ON DELETE CASCADE,
                venture_id TEXT NOT NULL,
                count INTEGER DEFAULT 0,
                is_automated BOOLEAN DEFAULT FALSE,
                PRIMARY KEY (player_id, venture_id)
            );
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS player_hires (
                player_id INTEGER REFERENCES players(id) ON DELETE CASCADE,
                hire_id TEXT NOT NULL,
                is_hired BOOLEAN DEFAULT FALSE,
                PRIMARY KEY (player_id, hire_id)
            );
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS player_upgrades (
                player_id INTEGER REFERENCES players(id) ON DELETE CASCADE,
                upgrade_id TEXT NOT NULL,
                is_purchased BOOLEAN DEFAULT FALSE,
                PRIMARY KEY (player_id, upgrade_id)
            );
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS player_skills (
                player_id INTEGER REFERENCES players(id) ON DELETE CASCADE,
                skill_id TEXT NOT NULL,
                level INTEGER DEFAULT 0,
                cost BIGINT,
                PRIMARY KEY (player_id, skill_id)
            );
            """.trimIndent()
        )
        conn.autoCommit = true
        queries.forEach { query ->
            conn.createStatement().execute(query)
        }
    }
}

fun savePlayerData(conn: Connection, data: PlayerData) {
    val playerId = conn.prepareStatement(
        """
        INSERT INTO players (username, balance, lifetime_earnings, experience_points, last_save_time)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (username)
        DO UPDATE SET balance = EXCLUDED.balance,
                      lifetime_earnings = EXCLUDED.lifetime_earnings,
                      experience_points = EXCLUDED.experience_points,
                      last_save_time = EXCLUDED.last_save_time
        RETURNING id;
        """.trimIndent()
    ).apply {
        setString(1, data.username)
        setDouble(2, data.balance)
        setDouble(3, data.lifetimeEarnings)
        setLong(4, data.experiencePoints)
        setLong(5, data.lastSaveTime)
    }.executeQuery().use { rs ->
        if (rs.next()) rs.getInt(1) else throw Exception("Failed to save player")
    }

    // Save Ventures
    conn.prepareStatement("DELETE FROM player_ventures WHERE player_id = ?").apply {
        setInt(1, playerId)
        executeUpdate()
    }
    data.ventures.forEach { venture ->
        conn.prepareStatement(
            "INSERT INTO player_ventures (player_id, venture_id, count, is_automated) VALUES (?, ?, ?, ?)"
        ).apply {
            setInt(1, playerId)
            setString(2, venture.id)
            setInt(3, venture.count)
            setBoolean(4, venture.isAutomated)
            executeUpdate()
        }
    }

    // Save Hires
    conn.prepareStatement("DELETE FROM player_hires WHERE player_id = ?").apply {
        setInt(1, playerId)
        executeUpdate()
    }
    data.hires.forEach { hire ->
        conn.prepareStatement(
            "INSERT INTO player_hires (player_id, hire_id, is_hired) VALUES (?, ?, ?)"
        ).apply {
            setInt(1, playerId)
            setString(2, hire.id)
            setBoolean(3, hire.isHired)
            executeUpdate()
        }
    }

    // Save Upgrades
    conn.prepareStatement("DELETE FROM player_upgrades WHERE player_id = ?").apply {
        setInt(1, playerId)
        executeUpdate()
    }
    data.upgrades.forEach { upgrade ->
        conn.prepareStatement(
            "INSERT INTO player_upgrades (player_id, upgrade_id, is_purchased) VALUES (?, ?, ?)"
        ).apply {
            setInt(1, playerId)
            setString(2, upgrade.id)
            setBoolean(3, upgrade.isPurchased)
            executeUpdate()
        }
    }

    // Save Skills
    conn.prepareStatement("DELETE FROM player_skills WHERE player_id = ?").apply {
        setInt(1, playerId)
        executeUpdate()
    }
    data.skills.forEach { skill ->
        conn.prepareStatement(
            "INSERT INTO player_skills (player_id, skill_id, level, cost) VALUES (?, ?, ?, ?)"
        ).apply {
            setInt(1, playerId)
            setString(2, skill.id)
            setInt(3, skill.level)
            setLong(4, skill.cost)
            executeUpdate()
        }
    }
}

fun loadPlayerData(conn: Connection, username: String): PlayerData? {
    val playerStmt = conn.prepareStatement(
        "SELECT id, balance, lifetime_earnings, experience_points, last_save_time FROM players WHERE username = ?"
    ).apply { setString(1, username) }

    val rs = playerStmt.executeQuery()
    if (!rs.next()) return null

    val playerId = rs.getInt("id")
    val balance = rs.getDouble("balance")
    val lifetimeEarnings = rs.getDouble("lifetime_earnings")
    val xp = rs.getLong("experience_points")
    val lastSave = rs.getLong("last_save_time")

    val ventures = mutableListOf<VentureSaveData>()
    conn.prepareStatement("SELECT venture_id, count, is_automated FROM player_ventures WHERE player_id = ?").apply {
        setInt(1, playerId)
        executeQuery().use { vrs ->
            while (vrs.next()) {
                ventures.add(VentureSaveData(vrs.getString("venture_id"), vrs.getInt("count"), vrs.getBoolean("is_automated")))
            }
        }
    }

    val hires = mutableListOf<HireSaveData>()
    conn.prepareStatement("SELECT hire_id, is_hired FROM player_hires WHERE player_id = ?").apply {
        setInt(1, playerId)
        executeQuery().use { hrs ->
            while (hrs.next()) {
                hires.add(HireSaveData(hrs.getString("hire_id"), hrs.getBoolean("is_hired")))
            }
        }
    }

    val upgrades = mutableListOf<UpgradeSaveData>()
    conn.prepareStatement("SELECT upgrade_id, is_purchased FROM player_upgrades WHERE player_id = ?").apply {
        setInt(1, playerId)
        executeQuery().use { urs ->
            while (urs.next()) {
                upgrades.add(UpgradeSaveData(urs.getString("upgrade_id"), urs.getBoolean("is_purchased")))
            }
        }
    }

    val skills = mutableListOf<SkillSaveData>()
    conn.prepareStatement("SELECT skill_id, level, cost FROM player_skills WHERE player_id = ?").apply {
        setInt(1, playerId)
        executeQuery().use { srs ->
            while (srs.next()) {
                skills.add(SkillSaveData(srs.getString("skill_id"), srs.getInt("level"), srs.getLong("cost")))
            }
        }
    }

    return PlayerData(username, balance, lifetimeEarnings, xp, ventures, hires, upgrades, skills, lastSave)
}
