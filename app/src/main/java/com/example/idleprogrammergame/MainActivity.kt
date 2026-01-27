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
import com.example.idleprogrammergame.ui.theme.IdleProgrammerGameTheme
import com.example.idleprogrammergame.ui_components.EarningsUpgradeCard
import com.example.idleprogrammergame.ui_components.HireDevCard
import com.example.idleprogrammergame.ui_components.HireState
import com.example.idleprogrammergame.ui_components.JetpackComposeEarningsFieldComponent
import com.example.idleprogrammergame.ui_components.JetpackComposeMoneyComponent
import com.example.idleprogrammergame.ui_components.UpgradeState

enum class GameTab {
    VENTURES,
    TEAM,
    UPGRADES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IdleProgrammerGameTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
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

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            JetpackComposeMoneyComponent(
                balance = "$499.51K",
                incomePerSecond = "+$313 / s"
            )

            Spacer(Modifier.height(12.dp))

            StatsRow()

            Spacer(Modifier.height(12.dp))

            // 👇 THIS is the fix
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (selectedTab) {
                    GameTab.VENTURES -> VenturesScreen()
                    GameTab.TEAM -> TeamScreen()
                    GameTab.UPGRADES -> UpgradesScreen()
                }
            }
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
fun VenturesScreen() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            JetpackComposeEarningsFieldComponent(
                title = "Bug Fix",
                income = "$156 / cycle",
                totalUpgrade = 27,
                price = "$174",
                state = UpgradeState.OWNED
            )
        }

        item {
            JetpackComposeEarningsFieldComponent(
                title = "Freelance Gig",
                income = "$5 / cycle",
                totalUpgrade = 1,
                price = "$68",
                state = UpgradeState.AVAILABLE
            )
        }

        item {
            JetpackComposeEarningsFieldComponent(
                title = "Open Source",
                income = "Community contributions",
                totalUpgrade = 0,
                price = "$720",
                state = UpgradeState.LOCKED
            )
        }
    }
}

@Composable
fun TeamScreen() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HireDevCard(
                title = "Junior Dev",
                subtitle = "Automating Bug Fix",
                price = "$1.00K",
                state = HireState.HIRED
            )
        }

        item {
            HireDevCard(
                title = "Mid-level Dev",
                subtitle = "Automates Freelance Gig",
                price = "$1.00K",
                state = HireState.AVAILABLE
            )
        }

        item {
            HireDevCard(
                title = "Senior Dev",
                subtitle = "Automates Open Source",
                price = "$10.00K",
                state = HireState.LOCKED
            )
        }
    }
}

@Composable
fun UpgradesScreen() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EarningsUpgradeCard(
                title = "Better Tools",
                description = "Doubles Bug Fix income",
                multiplier = "x2",
                price = "$174",
                state = UpgradeState.AVAILABLE
            )
        }

        item {
            EarningsUpgradeCard(
                title = "Open Source Fame",
                description = "Triples Open Source income",
                multiplier = "x3",
                price = "$720",
                state = UpgradeState.LOCKED
            )
        }

        item {
            EarningsUpgradeCard(
                title = "Enterprise Clients",
                description = "Quintuples Freelance income",
                multiplier = "x5",
                price = "$312",
                state = UpgradeState.OWNED
            )
        }
    }
}

@Composable
fun StatsRow() {
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
            value = "500.8K",
            label = "TOTAL",
            color = accent
        )

        VerticalDivider()

        StatItem(
            value = "28",
            label = "VENTURES",
            color = accent
        )

        VerticalDivider()

        StatItem(
            value = "1",
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IdleProgrammerGameTheme {
        GameScreen()
    }
}