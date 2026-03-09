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
data class VentureSaveData(val id: String, val count: Int, val isAutomated: Boolean)
@Serializable
data class HireSaveData(val id: String, val isHired: Boolean)
@Serializable
data class UpgradeSaveData(val id: String, val isPurchased: Boolean)
@Serializable
data class SkillSaveData(val id: String, val level: Int, val cost: Long)

@Serializable
data class PlayerData(
    val username: String,
    val balance: Double,
    val lifetimeEarnings: Double,
    val experiencePoints: Long,
    val rebirths: Int = 0,
    val ventures: List<VentureSaveData>,
    val hires: List<HireSaveData>,
    val upgrades: List<UpgradeSaveData>,
    val skills: List<SkillSaveData>,
    val lastSaveTime: Long = System.currentTimeMillis()
)

@Serializable
data class LeaderboardEntry(val username: String, val score: Double)

@Serializable
data class LeaderboardResponse(
    val topEarnings: List<LeaderboardEntry>,
    val topRebirths: List<LeaderboardEntry>,
    val topVentures: List<LeaderboardEntry>
)

fun main() {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://eu.difuser.online:5432/idleprogrammer"
        username = "gameuser"
        password = "0108021060sS"
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }

    val dataSource = HikariDataSource(hikariConfig)
    initDatabase(dataSource)

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallLogging) { level = Level.INFO }
        install(ContentNegotiation) {
            json(json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        routing {
            get("/") { call.respondText("Idle Programmer Server is running!") }

            post("/save") {
                try {
                    val data = call.receive<PlayerData>()
                    dataSource.connection.use { conn ->
                        try {
                            savePlayerData(conn, data)
                            conn.commit()
                            call.respond(mapOf("status" to "saved"))
                        } catch (e: Exception) {
                            conn.rollback()
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.localizedMessage))
                        }
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            get("/load") {
                val username = call.request.queryParameters["username"]
                if (username.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                dataSource.connection.use { conn ->
                    val data = loadPlayerData(conn, username)
                    if (data != null) call.respond(data) else call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/leaderboard") {
                dataSource.connection.use { conn ->
                    val topEarnings = getTopByColumn(conn, "lifetime_earnings")
                    val topRebirths = getTopByColumn(conn, "rebirths")
                    
                    // Complex query for total ventures
                    val topVentures = conn.prepareStatement("""
                        SELECT p.username, COALESCE(SUM(v.count), 0) as total_v
                        FROM players p
                        LEFT JOIN player_ventures v ON p.id = v.player_id
                        GROUP BY p.id
                        ORDER BY total_v DESC
                        LIMIT 10
                    """).executeQuery().use { rs ->
                        val list = mutableListOf<LeaderboardEntry>()
                        while(rs.next()) list.add(LeaderboardEntry(rs.getString("username"), rs.getDouble("total_v")))
                        list
                    }

                    call.respond(LeaderboardResponse(topEarnings, topRebirths, topVentures))
                }
            }
        }
    }.start(wait = true)
}

fun getTopByColumn(conn: Connection, column: String): List<LeaderboardEntry> {
    return conn.prepareStatement("SELECT username, $column FROM players ORDER BY $column DESC LIMIT 10")
        .executeQuery().use { rs ->
            val list = mutableListOf<LeaderboardEntry>()
            while(rs.next()) list.add(LeaderboardEntry(rs.getString("username"), rs.getDouble(column)))
            list
        }
}

fun initDatabase(dataSource: HikariDataSource) {
    dataSource.connection.use { conn ->
        val queries = listOf(
            "CREATE TABLE IF NOT EXISTS players (id SERIAL PRIMARY KEY, username TEXT UNIQUE NOT NULL, balance DOUBLE PRECISION DEFAULT 0.0, lifetime_earnings DOUBLE PRECISION DEFAULT 0.0, experience_points BIGINT DEFAULT 0, rebirths INTEGER DEFAULT 0, last_save_time BIGINT);",
            "CREATE TABLE IF NOT EXISTS player_ventures (player_id INTEGER REFERENCES players(id) ON DELETE CASCADE, venture_id TEXT NOT NULL, count INTEGER DEFAULT 0, is_automated BOOLEAN DEFAULT FALSE, PRIMARY KEY (player_id, venture_id));",
            "CREATE TABLE IF NOT EXISTS player_hires (player_id INTEGER REFERENCES players(id) ON DELETE CASCADE, hire_id TEXT NOT NULL, is_hired BOOLEAN DEFAULT FALSE, PRIMARY KEY (player_id, hire_id));",
            "CREATE TABLE IF NOT EXISTS player_upgrades (player_id INTEGER REFERENCES players(id) ON DELETE CASCADE, upgrade_id TEXT NOT NULL, is_purchased BOOLEAN DEFAULT FALSE, PRIMARY KEY (player_id, upgrade_id));",
            "CREATE TABLE IF NOT EXISTS player_skills (player_id INTEGER REFERENCES players(id) ON DELETE CASCADE, skill_id TEXT NOT NULL, level INTEGER DEFAULT 0, cost BIGINT, PRIMARY KEY (player_id, skill_id));"
        )
        conn.autoCommit = true
        queries.forEach { conn.createStatement().execute(it) }
        
        // Ensure rebirths column exists (for existing tables)
        try { conn.createStatement().execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS rebirths INTEGER DEFAULT 0;") } catch (e: Exception) {}
    }
}

fun savePlayerData(conn: Connection, data: PlayerData) {
    val playerId = conn.prepareStatement(
        "INSERT INTO players (username, balance, lifetime_earnings, experience_points, rebirths, last_save_time) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (username) DO UPDATE SET balance = EXCLUDED.balance, lifetime_earnings = EXCLUDED.lifetime_earnings, experience_points = EXCLUDED.experience_points, rebirths = EXCLUDED.rebirths, last_save_time = EXCLUDED.last_save_time RETURNING id;"
    ).apply {
        setString(1, data.username); setDouble(2, data.balance); setDouble(3, data.lifetimeEarnings); setLong(4, data.experiencePoints); setInt(5, data.rebirths); setLong(6, data.lastSaveTime)
    }.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else throw Exception("Fail") }

    fun clearAndSave(table: String, action: (java.sql.PreparedStatement) -> Unit) {
        conn.prepareStatement("DELETE FROM $table WHERE player_id = ?").apply { setInt(1, playerId); executeUpdate() }
        action(conn.prepareStatement("")) // Placeholder
    }

    // Simplified save for brevity in this tool call
    conn.prepareStatement("DELETE FROM player_ventures WHERE player_id = ?").apply { setInt(1, playerId); executeUpdate() }
    data.ventures.forEach { v ->
        conn.prepareStatement("INSERT INTO player_ventures (player_id, venture_id, count, is_automated) VALUES (?, ?, ?, ?)").apply {
            setInt(1, playerId); setString(2, v.id); setInt(3, v.count); setBoolean(4, v.isAutomated); executeUpdate()
        }
    }
    conn.prepareStatement("DELETE FROM player_hires WHERE player_id = ?").apply { setInt(1, playerId); executeUpdate() }
    data.hires.forEach { h ->
        conn.prepareStatement("INSERT INTO player_hires (player_id, hire_id, is_hired) VALUES (?, ?, ?)").apply {
            setInt(1, playerId); setString(2, h.id); setBoolean(3, h.isHired); executeUpdate()
        }
    }
    conn.prepareStatement("DELETE FROM player_upgrades WHERE player_id = ?").apply { setInt(1, playerId); executeUpdate() }
    data.upgrades.forEach { u ->
        conn.prepareStatement("INSERT INTO player_upgrades (player_id, upgrade_id, is_purchased) VALUES (?, ?, ?)").apply {
            setInt(1, playerId); setString(2, u.id); setBoolean(3, u.isPurchased); executeUpdate()
        }
    }
    conn.prepareStatement("DELETE FROM player_skills WHERE player_id = ?").apply { setInt(1, playerId); executeUpdate() }
    data.skills.forEach { s ->
        conn.prepareStatement("INSERT INTO player_skills (player_id, skill_id, level, cost) VALUES (?, ?, ?, ?)").apply {
            setInt(1, playerId); setString(2, s.id); setInt(3, s.level); setLong(4, s.cost); executeUpdate()
        }
    }
}

fun loadPlayerData(conn: Connection, username: String): PlayerData? {
    val playerStmt = conn.prepareStatement("SELECT id, balance, lifetime_earnings, experience_points, rebirths, last_save_time FROM players WHERE username = ?").apply { setString(1, username) }
    val rs = playerStmt.executeQuery()
    if (!rs.next()) return null
    val playerId = rs.getInt("id")
    val balance = rs.getDouble("balance"); val earnings = rs.getDouble("lifetime_earnings"); val xp = rs.getLong("experience_points"); val rebirths = rs.getInt("rebirths"); val lastSave = rs.getLong("last_save_time")

    val ventures = mutableListOf<VentureSaveData>()
    conn.prepareStatement("SELECT venture_id, count, is_automated FROM player_ventures WHERE player_id = ?").apply { setInt(1, playerId); executeQuery().use { vrs -> while(vrs.next()) ventures.add(VentureSaveData(vrs.getString("venture_id"), vrs.getInt("count"), vrs.getBoolean("is_automated"))) } }
    val hires = mutableListOf<HireSaveData>()
    conn.prepareStatement("SELECT hire_id, is_hired FROM player_hires WHERE player_id = ?").apply { setInt(1, playerId); executeQuery().use { hrs -> while(hrs.next()) hires.add(HireSaveData(hrs.getString("hire_id"), hrs.getBoolean("is_hired"))) } }
    val upgrades = mutableListOf<UpgradeSaveData>()
    conn.prepareStatement("SELECT upgrade_id, is_purchased FROM player_upgrades WHERE player_id = ?").apply { setInt(1, playerId); executeQuery().use { urs -> while(urs.next()) upgrades.add(UpgradeSaveData(urs.getString("upgrade_id"), urs.getBoolean("is_purchased"))) } }
    val skills = mutableListOf<SkillSaveData>()
    conn.prepareStatement("SELECT skill_id, level, cost FROM player_skills WHERE player_id = ?").apply { setInt(1, playerId); executeQuery().use { srs -> while(srs.next()) skills.add(SkillSaveData(srs.getString("skill_id"), srs.getInt("level"), srs.getLong("cost"))) } }

    return PlayerData(username, balance, earnings, xp, rebirths, ventures, hires, upgrades, skills, lastSave)
}
