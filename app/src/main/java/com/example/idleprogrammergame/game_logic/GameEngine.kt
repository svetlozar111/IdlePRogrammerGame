package com.example.idleprogrammergame.game_logic

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import com.example.idleprogrammergame.game_logic.AdRewardType
import kotlinx.serialization.Serializable

// Game constants
const val INITIAL_BALANCE = 0.0
const val UPDATE_INTERVAL_MS = 1000L
const val MIN_XP_TO_ASCEND = 10L // Player needs at least 10 XP to prestige
val MILESTONES = listOf(10, 25, 50, 100, 250, 500, 999)

// Venture types
data class Venture(
    val id: String,
    val title: String,
    val baseIncome: Double,
    val count: Int,
    val basePrice: Double,
    val isAutomated: Boolean,
    val productionTime: Long
)

// Hire types
data class Hire(
    val id: String,
    val title: String,
    val ventureId: String,
    val price: Double,
    val isHired: Boolean
)

data class Upgrade(
    val id: String,
    val title: String,
    val description: String,
    val ventureId: String,
    val multiplier: Double,
    val price: Double,
    val isPurchased: Boolean
)

// Skill Tree / Prestige Upgrade
data class SkillUpgrade(
    val id: String,
    val title: String,
    val description: String,
    val cost: Long,
    val level: Int = 0,
    val maxLevel: Int = 10,
    val effectMultiplier: Double = 0.1
)

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
    val ventures: List<VentureSaveData>,
    val hires: List<HireSaveData>,
    val upgrades: List<UpgradeSaveData>,
    val skills: List<SkillSaveData>,
    val lastSaveTime: Long = System.currentTimeMillis()
)

class GameEngine {
    // Game state
    var balance = mutableStateOf(INITIAL_BALANCE)
    var incomePerSecond = mutableStateOf(0.0)
    var incomeMultiplier = mutableStateOf(1.0)
    var bonusTimeRemainingSeconds = mutableStateOf(0.0)
    
    // Prestige State
    var experiencePoints = mutableStateOf(0L)
    var lifetimeEarnings = mutableStateOf(0.0)
    
    private val timeSinceLastProduction =
        androidx.compose.runtime.mutableStateMapOf<String, Double>()

    // Game data
    private val ventures = mutableStateListOf<Venture>()
    private val hires = mutableStateListOf<Hire>()
    private val upgrades = mutableStateListOf<Upgrade>()
    
    // Skill Tree data
    private val skillUpgrades = mutableStateListOf(
        SkillUpgrade(
            id = "legacy_knowledge",
            title = "Legacy Knowledge",
            description = "+10% Income per level",
            cost = 10,
            effectMultiplier = 0.1
        ),
        SkillUpgrade(
            id = "efficient_workflow",
            title = "Efficient Workflow",
            description = "-5% Venture costs per level",
            cost = 25,
            effectMultiplier = 0.05
        ),
        SkillUpgrade(
            id = "deep_focus",
            title = "Deep Focus",
            description = "Manual production is 20% faster per level",
            cost = 50,
            effectMultiplier = 0.2
        )
    )

    // Game loop
    private var isRunning = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        resetGameData()
        updateIncomePerSecond()
        startGameLoop()
    }

    private fun resetGameData() {
        balance.value = INITIAL_BALANCE
        ventures.clear()
        ventures.addAll(listOf(
            Venture("bug_fix", "Bug Fix", 10.0, 1, 100.0, false, 5),
            Venture("freelance_gig", "Freelance Gig", 50.0, 0, 500.0, false, 60),
            Venture("open_source", "Open Source", 200.0, 0, 2000.0, false, 300),
            Venture("mobile_app", "Mobile App", 500.0, 0, 5000.0, false, 1800),
            Venture("web_development", "Web Development", 1000.0, 0, 10000.0, false, 3600),
            Venture("ai_startup", "AI Startup", 5000.0, 0, 50000.0, false, 18000),
            Venture("blockchain", "Blockchain Project", 10000.0, 0, 100000.0, false, 86400)
        ))

        hires.clear()
        hires.addAll(listOf(
            Hire("junior_dev", "Junior Dev", "bug_fix", 1000.0, false),
            Hire("mid_level_dev", "Mid-level Dev", "freelance_gig", 5000.0, false),
            Hire("senior_dev", "Senior Dev", "open_source", 10000.0, false),
            Hire("mobile_dev", "Mobile Developer", "mobile_app", 25000.0, false),
            Hire("web_dev", "Web Developer", "web_development", 50000.0, false),
            Hire("ai_engineer", "AI Engineer", "ai_startup", 100000.0, false),
            Hire("blockchain_expert", "Blockchain Expert", "blockchain", 200000.0, false)
        ))

        upgrades.clear()
        upgrades.addAll(listOf(
            Upgrade("better_tools", "Better Tools", "Doubles Bug Fix income", "bug_fix", 2.0, 1000.0, false),
            Upgrade("open_source_fame", "Open Source Fame", "Triples Open Source income", "open_source", 3.0, 5000.0, false),
            Upgrade("enterprise_clients", "Enterprise Clients", "Quintuples Freelance income", "freelance_gig", 5.0, 3000.0, false),
            Upgrade("cross_platform", "Cross-Platform", "Doubles Mobile App income", "mobile_app", 2.0, 10000.0, false),
            Upgrade("cloud_hosting", "Cloud Hosting", "Triples Web Development income", "web_development", 3.0, 20000.0, false),
            Upgrade("ai_algorithm", "Advanced AI Algorithm", "Quadruples AI Startup income", "ai_startup", 4.0, 50000.0, false),
            Upgrade("smart_contracts", "Smart Contracts", "Quintuples Blockchain Project income", "blockchain", 5.0, 100000.0, false)
        ))

        ventures.forEach { timeSinceLastProduction[it.id] = 0.0 }
    }

    fun toPlayerData(username: String): PlayerData {
        return PlayerData(
            username = username,
            balance = balance.value,
            lifetimeEarnings = lifetimeEarnings.value,
            experiencePoints = experiencePoints.value,
            ventures = ventures.map { VentureSaveData(it.id, it.count, it.isAutomated) },
            hires = hires.map { HireSaveData(it.id, it.isHired) },
            upgrades = upgrades.map { UpgradeSaveData(it.id, it.isPurchased) },
            skills = skillUpgrades.map { SkillSaveData(it.id, it.level, it.cost) }
        )
    }

    fun loadFromPlayerData(data: PlayerData) {
        balance.value = data.balance
        lifetimeEarnings.value = data.lifetimeEarnings
        experiencePoints.value = data.experiencePoints
        
        data.ventures.forEach { save ->
            val index = ventures.indexOfFirst { it.id == save.id }
            if (index != -1) {
                ventures[index] = ventures[index].copy(count = save.count, isAutomated = save.isAutomated)
            }
        }
        
        data.hires.forEach { save ->
            val index = hires.indexOfFirst { it.id == save.id }
            if (index != -1) {
                hires[index] = hires[index].copy(isHired = save.isHired)
            }
        }
        
        data.upgrades.forEach { save ->
            val index = upgrades.indexOfFirst { it.id == save.id }
            if (index != -1) {
                upgrades[index] = upgrades[index].copy(isPurchased = save.isPurchased)
            }
        }
        
        data.skills.forEach { save ->
            val index = skillUpgrades.indexOfFirst { it.id == save.id }
            if (index != -1) {
                skillUpgrades[index] = skillUpgrades[index].copy(level = save.level, cost = save.cost)
            }
        }
        
        updateIncomePerSecond()
    }

    fun calculateExperienceGain(): Long {
        val gain = floor(sqrt(lifetimeEarnings.value) / 10).toLong()
        return gain.coerceAtLeast(0)
    }

    fun canAscend(): Boolean = calculateExperienceGain() >= MIN_XP_TO_ASCEND

    fun ascend() {
        if (!canAscend()) return
        
        val gain = calculateExperienceGain()
        experiencePoints.value += gain
        
        // Reset everything except XP and Skill Tree
        resetGameData()
        updateIncomePerSecond()
    }

    fun purchaseSkill(skillId: String): Boolean {
        val index = skillUpgrades.indexOfFirst { it.id == skillId }
        if (index == -1) return false
        
        val skill = skillUpgrades[index]
        if (experiencePoints.value >= skill.cost && skill.level < skill.maxLevel) {
            experiencePoints.value -= skill.cost
            skillUpgrades[index] = skill.copy(
                level = skill.level + 1,
                cost = (skill.cost * 1.5).toLong()
            )
            updateIncomePerSecond()
            return true
        }
        return false
    }

    private fun getMilestoneSpeedMultiplier(count: Int): Double {
        var multiplier = 1.0
        for (m in MILESTONES) {
            if (count >= m) {
                multiplier *= 2.0 // Each milestone doubles the speed
            } else {
                break
            }
        }
        return multiplier
    }

    private fun startGameLoop() {
        isRunning = true
        coroutineScope.launch {
            val tickInterval = 100L
            while (isRunning) {
                delay(tickInterval)
                val deltaTime = tickInterval / 1000.0
                updateBonusTime(deltaTime)
                
                ventures.forEach { venture ->
                    if (venture.count > 0) {
                        val currentTime = timeSinceLastProduction[venture.id] ?: 0.0
                        val manualSpeedBoost = if (!venture.isAutomated) {
                            1.0 + (getSkillLevel("deep_focus") * getSkillEffect("deep_focus"))
                        } else 1.0
                        
                        val milestoneBoost = getMilestoneSpeedMultiplier(venture.count)
                        val totalSpeedBoost = manualSpeedBoost * milestoneBoost
                        
                        val adjustedDelta = deltaTime * totalSpeedBoost
                        
                        if (venture.isAutomated) {
                            val newTime = currentTime + adjustedDelta
                            timeSinceLastProduction[venture.id] = newTime
                            if (newTime >= venture.productionTime) {
                                val income = calculateVentureIncome(venture)
                                addBalance(income)
                                timeSinceLastProduction[venture.id] = newTime % venture.productionTime
                            }
                        } else if (currentTime < venture.productionTime) {
                            val newTime = currentTime + adjustedDelta
                            timeSinceLastProduction[venture.id] = newTime
                            if (newTime >= venture.productionTime) {
                                val income = calculateVentureIncome(venture)
                                addBalance(income)
                                timeSinceLastProduction[venture.id] = venture.productionTime.toDouble()
                            }
                        }
                    }
                }
            }
        }
    }

    fun stopGameLoop() {
        isRunning = false
    }

    fun startManualProduction(ventureId: String): Boolean {
        val venture = getVenture(ventureId)
        if (venture != null && venture.count > 0 && !venture.isAutomated) {
            val currentTime = timeSinceLastProduction[ventureId] ?: 0.0
            
            // If cycle is complete, start a new cycle
            if (currentTime >= venture.productionTime) {
                timeSinceLastProduction[ventureId] = 0.0
                return true
            }
        }
        return false
    }

    private fun addBalance(amount: Double) {
        balance.value += amount
        lifetimeEarnings.value += amount
    }

    fun getProductionProgress(ventureId: String): Float {
        val venture = ventures.find { it.id == ventureId } ?: return 0f
        val time = timeSinceLastProduction[ventureId] ?: 0.0
        return (time / venture.productionTime).toFloat().coerceIn(0f, 1f)
    }

    private fun updateIncomePerSecond() {
        var total = 0.0
        ventures.forEach { venture ->
            if (venture.count > 0) {
                val perCycle = calculateVentureIncome(venture)
                val milestoneBoost = getMilestoneSpeedMultiplier(venture.count)
                val perSecond = (perCycle * milestoneBoost) / venture.productionTime
                total += perSecond
            }
        }
        incomePerSecond.value = total
    }

    fun calculateVentureIncome(venture: Venture): Double {
        val baseIncome = venture.baseIncome
        val count = venture.count
        val upgradesMultiplier = getVentureUpgradesMultiplier(venture.id)
        val xpBonus = 1.0 + (getSkillLevel("legacy_knowledge") * getSkillEffect("legacy_knowledge"))
        
        return baseIncome * count * upgradesMultiplier * incomeMultiplier.value * xpBonus
    }

    private fun getVentureUpgradesMultiplier(ventureId: String): Double {
        var multiplier = 1.0
        upgrades.forEach {
            if (it.ventureId == ventureId && it.isPurchased) {
                multiplier *= it.multiplier
            }
        }
        return multiplier
    }

    fun calculateVenturePrice(venture: Venture): Double {
        val basePrice = venture.basePrice
        val count = venture.count
        val priceMultiplier = 1.15.pow(count)
        val costReduction = 1.0 - (getSkillLevel("efficient_workflow") * getSkillEffect("efficient_workflow"))
        
        return basePrice * priceMultiplier * costReduction
    }

    fun purchaseVenture(ventureId: String): Boolean {
        val index = ventures.indexOfFirst { it.id == ventureId }
        if (index == -1) return false
        val venture = ventures[index]
        if (venture.count >= 999) return false

        val price = calculateVenturePrice(venture)
        if (balance.value >= price) {
            balance.value -= price
            ventures[index] = venture.copy(count = venture.count + 1)
            updateIncomePerSecond()
            return true
        }
        return false
    }

    fun purchaseHire(hireId: String): Boolean {
        val hireIndex = hires.indexOfFirst { it.id == hireId }
        if (hireIndex == -1) return false
        val hire = hires[hireIndex]
        if (balance.value >= hire.price && !hire.isHired) {
            balance.value -= hire.price
            hires[hireIndex] = hire.copy(isHired = true)
            val ventureIndex = ventures.indexOfFirst { it.id == hire.ventureId }
            if (ventureIndex != -1) {
                ventures[ventureIndex] = ventures[ventureIndex].copy(isAutomated = true)
            }
            updateIncomePerSecond()
            return true
        }
        return false
    }

    fun purchaseUpgrade(upgradeId: String): Boolean {
        val index = upgrades.indexOfFirst { it.id == upgradeId }
        if (index == -1) return false
        val upgrade = upgrades[index]
        if (balance.value >= upgrade.price && !upgrade.isPurchased) {
            balance.value -= upgrade.price
            upgrades[index] = upgrade.copy(isPurchased = true)
            updateIncomePerSecond()
            return true
        }
        return false
    }

    private fun getSkillLevel(skillId: String): Int = skillUpgrades.find { it.id == skillId }?.level ?: 0
    private fun getSkillEffect(skillId: String): Double = skillUpgrades.find { it.id == skillId }?.effectMultiplier ?: 0.0
    
    fun getVenture(ventureId: String): Venture? = ventures.find { it.id == ventureId }
    fun getAllVentures(): List<Venture> = ventures.toList()
    fun getAllHires(): List<Hire> = hires.toList()
    fun getAllUpgrades(): List<Upgrade> = upgrades.toList()
    fun getAllSkills(): List<SkillUpgrade> = skillUpgrades.toList()

    fun formatCurrency(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${String.format("%.2f", amount / 1_000_000_000)}B"
            amount >= 1_000_000 -> "${String.format("%.2f", amount / 1_000_000)}M"
            amount >= 1_000 -> "${String.format("%.2f", amount / 1_000)}K"
            else -> String.format("%.0f", amount)
        }
    }

    fun formatCurrencyWithSymbol(amount: Double): String = "$${formatCurrency(amount)}"

    fun getTotalVentures(): Int {
        return ventures.sumOf { it.count }
    }

    fun getTotalHires(): Int {
        return hires.count { it.isHired }
    }

    fun getTotalEarnings(): Double {
        return lifetimeEarnings.value
    }

    fun activateBonus(rewardType: AdRewardType) {
        incomeMultiplier.value = rewardType.multiplier
        bonusTimeRemainingSeconds.value = rewardType.durationMinutes * 60.0
        updateIncomePerSecond()
    }

    private fun updateBonusTime(deltaTime: Double) {
        if (bonusTimeRemainingSeconds.value > 0) {
            val newTime = (bonusTimeRemainingSeconds.value - deltaTime).coerceAtLeast(0.0)
            bonusTimeRemainingSeconds.value = newTime
            if (bonusTimeRemainingSeconds.value <= 0) {
                bonusTimeRemainingSeconds.value = 0.0
                incomeMultiplier.value = 1.0
                updateIncomePerSecond()
            }
        }
    }

    fun hasActiveBonus(): Boolean = bonusTimeRemainingSeconds.value > 0

    fun getBonusTimeFormatted(): String {
        val totalSeconds = bonusTimeRemainingSeconds.value.toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}