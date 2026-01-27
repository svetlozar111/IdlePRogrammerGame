package com.example.idleprogrammergame.game_logic

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow

// Game constants
const val INITIAL_BALANCE = 50000.0 // $50.00K
const val UPDATE_INTERVAL_MS = 1000L // Update every second

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

class GameEngine {
    // Game state
    var balance = mutableStateOf(INITIAL_BALANCE)
    var incomePerSecond = mutableStateOf(0.0)
    private val timeSinceLastProduction =
        androidx.compose.runtime.mutableStateMapOf<String, Double>()

    // Game data (using mutableState for UI updates)
    private val ventures = mutableStateListOf(
        Venture(
            id = "bug_fix",
            title = "Bug Fix",
            baseIncome = 10.0,
            count = 0,
            basePrice = 100.0,
            isAutomated = false,
            productionTime = 5 // 5 seconds per cycle
        ),
        Venture(
            id = "freelance_gig",
            title = "Freelance Gig",
            baseIncome = 50.0,
            count = 0,
            basePrice = 500.0,
            isAutomated = false,
            productionTime = 60 // 1 minute per cycle
        ),
        Venture(
            id = "open_source",
            title = "Open Source",
            baseIncome = 200.0,
            count = 0,
            basePrice = 2000.0,
            isAutomated = false,
            productionTime = 300 // 5 minutes per cycle
        ),
        Venture(
            id = "mobile_app",
            title = "Mobile App",
            baseIncome = 500.0,
            count = 0,
            basePrice = 5000.0,
            isAutomated = false,
            productionTime = 1800 // 30 minutes per cycle
        ),
        Venture(
            id = "web_development",
            title = "Web Development",
            baseIncome = 1000.0,
            count = 0,
            basePrice = 10000.0,
            isAutomated = false,
            productionTime = 3600 // 1 hour per cycle
        ),
        Venture(
            id = "ai_startup",
            title = "AI Startup",
            baseIncome = 5000.0,
            count = 0,
            basePrice = 50000.0,
            isAutomated = false,
            productionTime = 18000 // 5 hours per cycle
        ),
        Venture(
            id = "blockchain",
            title = "Blockchain Project",
            baseIncome = 10000.0,
            count = 0,
            basePrice = 100000.0,
            isAutomated = false,
            productionTime = 86400 // 24 hours per cycle
        )
    )

    private val hires = mutableStateListOf(
        Hire(
            id = "junior_dev",
            title = "Junior Dev",
            ventureId = "bug_fix",
            price = 1000.0,
            isHired = false
        ),
        Hire(
            id = "mid_level_dev",
            title = "Mid-level Dev",
            ventureId = "freelance_gig",
            price = 5000.0,
            isHired = false
        ),
        Hire(
            id = "senior_dev",
            title = "Senior Dev",
            ventureId = "open_source",
            price = 10000.0,
            isHired = false
        ),
        Hire(
            id = "mobile_dev",
            title = "Mobile Developer",
            ventureId = "mobile_app",
            price = 25000.0,
            isHired = false
        ),
        Hire(
            id = "web_dev",
            title = "Web Developer",
            ventureId = "web_development",
            price = 50000.0,
            isHired = false
        ),
        Hire(
            id = "ai_engineer",
            title = "AI Engineer",
            ventureId = "ai_startup",
            price = 100000.0,
            isHired = false
        ),
        Hire(
            id = "blockchain_expert",
            title = "Blockchain Expert",
            ventureId = "blockchain",
            price = 200000.0,
            isHired = false
        )
    )

    private val upgrades = mutableStateListOf(
        Upgrade(
            id = "better_tools",
            title = "Better Tools",
            description = "Doubles Bug Fix income",
            ventureId = "bug_fix",
            multiplier = 2.0,
            price = 1000.0,
            isPurchased = false
        ),
        Upgrade(
            id = "open_source_fame",
            title = "Open Source Fame",
            description = "Triples Open Source income",
            ventureId = "open_source",
            multiplier = 3.0,
            price = 5000.0,
            isPurchased = false
        ),
        Upgrade(
            id = "enterprise_clients",
            title = "Enterprise Clients",
            description = "Quintuples Freelance income",
            ventureId = "freelance_gig",
            multiplier = 5.0,
            price = 3000.0,
            isPurchased = false
        ),
        Upgrade(
            id = "cross_platform",
            title = "Cross-Platform",
            description = "Doubles Mobile App income",
            ventureId = "mobile_app",
            multiplier = 2.0,
            price = 10000.0,
            isPurchased = false
        ),
        Upgrade(
            id = "cloud_hosting",
            title = "Cloud Hosting",
            description = "Triples Web Development income",
            ventureId = "web_development",
            multiplier = 3.0,
            price = 20000.0,
            isPurchased = false
        ),
        Upgrade(
            id = "ai_algorithm",
            title = "Advanced AI Algorithm",
            description = "Quadruples AI Startup income",
            ventureId = "ai_startup",
            multiplier = 4.0,
            price = 50000.0,
            isPurchased = false
        ),
        Upgrade(
            id = "smart_contracts",
            title = "Smart Contracts",
            description = "Quintuples Blockchain Project income",
            ventureId = "blockchain",
            multiplier = 5.0,
            price = 100000.0,
            isPurchased = false
        )
    )

    // Game loop
    private var isRunning = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        // Initialize production timers for each venture
        ventures.forEach {
            timeSinceLastProduction[it.id] = 0.0
        }
        updateIncomePerSecond()
        startGameLoop()
    }

    private fun startGameLoop() {
        isRunning = true
        coroutineScope.launch {
            val tickInterval = 100L // Update every 0.1 seconds
            while (isRunning) {
                delay(tickInterval)
                val deltaTime = tickInterval / 1000.0
                
                // Update production timers for automated ventures
                ventures.forEach { venture ->
                    if (venture.isAutomated && venture.count > 0) {
                        val currentTime = timeSinceLastProduction[venture.id] ?: 0.0
                        val newTime = currentTime + deltaTime
                        timeSinceLastProduction[venture.id] = newTime
                        
                        // Check if production cycle is complete
                        if (newTime >= venture.productionTime) {
                            val income = calculateVentureIncome(venture)
                            balance.value += income
                            timeSinceLastProduction[venture.id] = newTime % venture.productionTime
                            updateIncomePerSecond()
                        }
                    }
                }
            }
        }
    }

    fun stopGameLoop() {
        isRunning = false
    }

    // Get progress towards next production for a specific venture (0.0 to 1.0)
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
                val perSecond = perCycle / venture.productionTime
                total += perSecond
            }
        }
        incomePerSecond.value = total
    }

    fun calculateVentureIncome(venture: Venture): Double {
        val baseIncome = venture.baseIncome
        val count = venture.count
        val upgradesMultiplier = getVentureUpgradesMultiplier(venture.id)
        return baseIncome * count * upgradesMultiplier
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
        return basePrice * priceMultiplier
    }

    fun purchaseVenture(ventureId: String): Boolean {
        val index = ventures.indexOfFirst { it.id == ventureId }
        if (index == -1) return false

        val venture = ventures[index]
        val price = calculateVenturePrice(venture)

        if (balance.value >= price) {
            balance.value -= price

            ventures[index] = venture.copy(
                count = venture.count + 1
            )

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
                val venture = ventures[ventureIndex]
                ventures[ventureIndex] = venture.copy(isAutomated = true)
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
    // Helper methods to get game data
    fun getVenture(ventureId: String): Venture? {
        return ventures.find { it.id == ventureId }
    }

    fun getHire(hireId: String): Hire? {
        return hires.find { it.id == hireId }
    }

    fun getUpgrade(upgradeId: String): Upgrade? {
        return upgrades.find { it.id == upgradeId }
    }

    fun getAllVentures(): List<Venture> = ventures.toList()
    fun getAllHires(): List<Hire> = hires.toList()
    fun getAllUpgrades(): List<Upgrade> = upgrades.toList()

    // Formatters for display
    fun formatCurrency(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${String.format("%.2f", amount / 1_000_000_000)}B"
            amount >= 1_000_000 -> "${String.format("%.2f", amount / 1_000_000)}M"
            amount >= 1_000 -> "${String.format("%.2f", amount / 1_000)}K"
            else -> String.format("%.0f", amount)
        }
    }

    fun formatCurrencyWithSymbol(amount: Double): String {
        return "$${formatCurrency(amount)}"
    }

    fun getTotalVentures(): Int {
        return ventures.sumOf { it.count }
    }

    fun getTotalHires(): Int {
        return hires.count { it.isHired }
    }

    fun getTotalEarnings(): Double {
        return INITIAL_BALANCE + (balance.value - INITIAL_BALANCE) // Calculate total earnings from initial state
    }
}