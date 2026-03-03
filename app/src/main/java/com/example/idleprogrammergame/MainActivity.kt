package com.example.idleprogrammergame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idleprogrammergame.game_logic.GameEngine
import com.example.idleprogrammergame.game_logic.Hire
import com.example.idleprogrammergame.game_logic.Upgrade
import com.example.idleprogrammergame.game_logic.Venture
import com.example.idleprogrammergame.ui.theme.IdleProgrammerGameTheme
import com.example.idleprogrammergame.ui_components.EarningsUpgradeCard
import com.example.idleprogrammergame.ui_components.FloatingAdButton
import com.example.idleprogrammergame.ui_components.HireDevCard
import com.example.idleprogrammergame.ui_components.HireState
import com.example.idleprogrammergame.ui_components.JetpackComposeEarningsFieldComponent
import com.example.idleprogrammergame.ui_components.JetpackComposeMoneyComponent
import com.example.idleprogrammergame.ui_components.UpgradeState
import com.example.idleprogrammergame.game_logic.AdEngine

enum class GameTab {
    VENTURES,
    TEAM,
    UPGRADES
}

class MainActivity : ComponentActivity() {
    private val gameEngine = GameEngine()
    private lateinit var adEngine: AdEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adEngine = AdEngine(gameEngine)
        adEngine.initialize(this)
        adEngine.setActivity(this)
        enableEdgeToEdge()
        setContent {
            IdleProgrammerGameTheme {
                GameScreen(gameEngine, adEngine)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameEngine.stopGameLoop()
    }
}

@Composable
fun GameScreen(gameEngine: GameEngine, adEngine: AdEngine) {
    var selectedTab by remember { mutableStateOf(GameTab.VENTURES) }

    Scaffold(
        containerColor = Color(0xFF050A10),
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {

                JetpackComposeMoneyComponent(
                    balance = gameEngine.formatCurrencyWithSymbol(gameEngine.balance.value),
                    incomePerSecond = "+${gameEngine.formatCurrencyWithSymbol(gameEngine.incomePerSecond.value)} / s"
                )

                Spacer(Modifier.height(12.dp))

                StatsRow(gameEngine)

                Spacer(Modifier.height(12.dp))

                // 👇 THIS is the fix
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    when (selectedTab) {
                        GameTab.VENTURES -> VenturesScreen(gameEngine)
                        GameTab.TEAM -> TeamScreen(gameEngine)
                        GameTab.UPGRADES -> UpgradesScreen(gameEngine)
                    }
                }
            }

            // Floating Ad Button in top-right corner
            FloatingAdButton(
                adEngine = adEngine,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = padding.calculateTopPadding() + 8.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: GameTab,
    onTabSelected: (GameTab) -> Unit
) {
    val accent = Color(0xFF00FFC6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E141B))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        NavItem("Ventures", selectedTab == GameTab.VENTURES) {
            onTabSelected(GameTab.VENTURES)
        }
        NavItem("Team", selectedTab == GameTab.TEAM) {
            onTabSelected(GameTab.TEAM)
        }
        NavItem("Upgrades", selectedTab == GameTab.UPGRADES) {
            onTabSelected(GameTab.UPGRADES)
        }
    }
}

@Composable
fun NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (selected) Color(0xFF00FFC6) else Color.Gray,
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun VenturesScreen(gameEngine: GameEngine) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        gameEngine.getAllVentures().forEach { venture ->
            item {
                JetpackComposeEarningsFieldComponent(
                    title = venture.title,
                    income = "${gameEngine.formatCurrencyWithSymbol(gameEngine.calculateVentureIncome(venture))} / cycle",
                    totalUpgrade = venture.count,
                    price = gameEngine.formatCurrencyWithSymbol(gameEngine.calculateVenturePrice(venture)),
                    state = getVentureState(venture),
                    isAutomated = venture.isAutomated,
                    progress = gameEngine.getProductionProgress(venture.id),
                    onButtonClick = {
                        gameEngine.purchaseVenture(venture.id)
                    },
                    onManualClick = {
                        gameEngine.startManualProduction(venture.id)
                    }
                )
            }
        }
    }
}

@Composable
fun TeamScreen(gameEngine: GameEngine) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        gameEngine.getAllHires().forEach { hire ->
            item {
                HireDevCard(
                    title = hire.title,
                    subtitle = "Automating ${getVentureTitle(gameEngine, hire.ventureId)}",
                    price = gameEngine.formatCurrencyWithSymbol(hire.price),
                    state = getHireState(hire, gameEngine),
                    onClick = {
                        gameEngine.purchaseHire(hire.id)
                    }
                )
            }
        }
    }
}

@Composable
fun UpgradesScreen(gameEngine: GameEngine) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        gameEngine.getAllUpgrades().forEach { upgrade ->
            item {
                EarningsUpgradeCard(
                    title = upgrade.title,
                    description = upgrade.description,
                    multiplier = "x${upgrade.multiplier.toInt()}",
                    price = gameEngine.formatCurrencyWithSymbol(upgrade.price),
                    state = getUpgradeState(upgrade, gameEngine),
                    onClick = {
                        gameEngine.purchaseUpgrade(upgrade.id)
                    }
                )
            }
        }
    }
}

@Composable
fun StatsRow(gameEngine: GameEngine) {
    val bg = Color(0xFF0E141B)
    val accent = Color(0xFF00FFC6)
    val muted = Color.Gray.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        StatItem(
            value = gameEngine.formatCurrency(gameEngine.getTotalEarnings()),
            label = "TOTAL",
            color = accent
        )

        VerticalDivider()

        StatItem(
            value = gameEngine.getTotalVentures().toString(),
            label = "VENTURES",
            color = accent
        )

        VerticalDivider()

        StatItem(
            value = gameEngine.getTotalHires().toString(),
            label = "DEVS",
            color = accent
        )
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}

// Helper functions to determine states
@Composable
fun getVentureState(venture: Venture): UpgradeState {
    return when {
        venture.count > 0 -> UpgradeState.OWNED
        else -> UpgradeState.AVAILABLE
    }
}

@Composable
fun isVentureAutomated(venture: Venture, gameEngine: GameEngine): Boolean {
    return venture.isAutomated
}

@Composable
fun getHireState(hire: Hire, gameEngine: GameEngine): HireState {
    return when {
        hire.isHired -> HireState.HIRED
        canAffordHire(hire, gameEngine) && hasRequiredVentureForHire(hire, gameEngine) -> HireState.AVAILABLE
        else -> HireState.LOCKED
    }
}

@Composable
fun hasRequiredVentureForHire(hire: Hire, gameEngine: GameEngine): Boolean {
    val venture = gameEngine.getVenture(hire.ventureId)
    return venture != null && venture.count > 0
}

@Composable
fun getUpgradeState(upgrade: Upgrade, gameEngine: GameEngine): UpgradeState {
    return when {
        upgrade.isPurchased -> UpgradeState.OWNED
        canAffordUpgrade(upgrade, gameEngine) && hasRequiredVenture(upgrade, gameEngine) -> UpgradeState.AVAILABLE
        else -> UpgradeState.LOCKED
    }
}

@Composable
fun canAffordHire(hire: Hire, gameEngine: GameEngine): Boolean {
    return gameEngine.balance.value >= hire.price
}

@Composable
fun canAffordUpgrade(upgrade: Upgrade, gameEngine: GameEngine): Boolean {
    return gameEngine.balance.value >= upgrade.price
}

@Composable
fun hasRequiredVenture(upgrade: Upgrade, gameEngine: GameEngine): Boolean {
    val venture = gameEngine.getVenture(upgrade.ventureId)
    return venture != null && venture.count > 0
}

@Composable
fun getVentureTitle(gameEngine: GameEngine, ventureId: String): String {
    return gameEngine.getVenture(ventureId)?.title ?: "Unknown"
}