package com.example.idleprogrammergame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idleprogrammergame.game_logic.*
import com.example.idleprogrammergame.ui.theme.IdleProgrammerGameTheme
import com.example.idleprogrammergame.ui_components.*

enum class GameTab {
    VENTURES,
    TEAM,
    UPGRADES,
    CAREER
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
    var showProfileModal by remember { mutableStateOf(false) }

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

                Spacer(Modifier.height(16.dp))

                // NEW FUNNY HEADER BUTTONS
                FunnyHeaderButtons(gameEngine) { showProfileModal = true }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    when (selectedTab) {
                        GameTab.VENTURES -> VenturesScreen(gameEngine)
                        GameTab.TEAM -> TeamScreen(gameEngine)
                        GameTab.UPGRADES -> UpgradesScreen(gameEngine)
                        GameTab.CAREER -> CareerScreen(gameEngine)
                    }
                }
            }

            FloatingAdButton(
                adEngine = adEngine,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = padding.calculateTopPadding() + 8.dp, end = 16.dp)
            )
            
            if (showProfileModal) {
                ProfileOverlay(gameEngine) { showProfileModal = false }
            }
        }
    }
}

@Composable
fun FunnyHeaderButtons(gameEngine: GameEngine, onProfileClick: () -> Unit) {
    val bg = Color(0xFF0E141B)
    val accent = Color(0xFF00FFC6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // LEFT BUTTON: PROFILE
        HeaderButton(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 16.dp, bottomEnd = 16.dp)),
            icon = Icons.Default.Person,
            label = "PROFILE",
            onClick = onProfileClick
        )

        // MIDDLE BUTTON: MODES
        HeaderButton(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp)),
            icon = Icons.Default.PlayArrow,
            label = "MODES",
            onClick = { /* Reserved */ }
        )

        // RIGHT BUTTON: LEADERBOARD
        HeaderButton(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 32.dp, bottomEnd = 32.dp)),
            icon = Icons.Default.Star,
            label = "RANKS",
            onClick = { /* Mocked data */ }
        )
    }
}

@Composable
fun HeaderButton(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(Color(0xFF0E141B))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00FFC6), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileOverlay(gameEngine: GameEngine, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0E141B))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DEVELOPER PROFILE", color = Color(0xFF00FFC6), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            ProfileStatRow("Lifetime Earnings", gameEngine.formatCurrencyWithSymbol(gameEngine.getTotalEarnings()))
            ProfileStatRow("Total Ventures", gameEngine.getTotalVentures().toString())
            ProfileStatRow("Team Size", gameEngine.getTotalHires().toString())
            
            Spacer(Modifier.height(24.dp))
            Text("SHOWCASED ACHIEVEMENTS", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AchievementBadge("💻", "Bug Crusher")
                AchievementBadge("🚀", "Scale King")
                AchievementBadge("💰", "Billionaire")
            }
            
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00FFC6))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AchievementBadge(emoji: String, title: String) {
    Column(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 24.sp)
        Text(title, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomNavBar(
    selectedTab: GameTab,
    onTabSelected: (GameTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E141B))
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        NavItem("Ventures", selectedTab == GameTab.VENTURES) { onTabSelected(GameTab.VENTURES) }
        NavItem("Team", selectedTab == GameTab.TEAM) { onTabSelected(GameTab.TEAM) }
        NavItem("Upgrades", selectedTab == GameTab.UPGRADES) { onTabSelected(GameTab.UPGRADES) }
        NavItem("Career", selectedTab == GameTab.CAREER) { onTabSelected(GameTab.CAREER) }
    }
}

@Composable
fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
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
                    onButtonClick = { gameEngine.purchaseVenture(venture.id) },
                    onManualClick = { gameEngine.startManualProduction(venture.id) }
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
                    requiredVenture = getVentureTitle(gameEngine, hire.ventureId),
                    onClick = { gameEngine.purchaseHire(hire.id) }
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
                    onClick = { gameEngine.purchaseUpgrade(upgrade.id) }
                )
            }
        }
    }
}

@Composable
fun CareerScreen(gameEngine: GameEngine) {
    val xpGain = gameEngine.calculateExperienceGain()
    val canAscend = gameEngine.canAscend()
    val xpColor = Color(0xFFBB86FC)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0E141B))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Prestige Points", color = Color.Gray, fontSize = 14.sp)
                Text("${gameEngine.experiencePoints.value} XP", color = xpColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (canAscend) Color.White else Color.Gray.copy(alpha = 0.2f))
                        .clickable(enabled = canAscend) { gameEngine.ascend() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (canAscend) "ASCEND CAREER" else "NOT READY",
                            color = if (canAscend) Color.Black else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        if (canAscend) {
                            Text("Gain +$xpGain XP", color = Color.Black.copy(alpha = 0.6f), fontSize = 12.sp)
                        } else {
                            Text("Need $MIN_XP_TO_ASCEND XP to reset", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("Skill Tree", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }

        gameEngine.getAllSkills().forEach { skill ->
            item {
                SkillUpgradeCard(
                    title = skill.title,
                    description = skill.description,
                    level = skill.level,
                    maxLevel = skill.maxLevel,
                    cost = skill.cost,
                    canAfford = gameEngine.experiencePoints.value >= skill.cost,
                    onClick = { gameEngine.purchaseSkill(skill.id) }
                )
            }
        }
    }
}

@Composable
fun getVentureState(venture: Venture): UpgradeState {
    return when {
        venture.count > 0 -> UpgradeState.OWNED
        else -> UpgradeState.AVAILABLE
    }
}

@Composable
fun getHireState(hire: Hire, gameEngine: GameEngine): HireState {
    return when {
        hire.isHired -> HireState.HIRED
        hasRequiredVentureForHire(hire, gameEngine) -> HireState.AVAILABLE
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
        hasRequiredVenture(upgrade, gameEngine) -> UpgradeState.AVAILABLE
        else -> UpgradeState.LOCKED
    }
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