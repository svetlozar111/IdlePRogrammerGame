package com.example.idleprogrammergame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.idleprogrammergame.game_logic.AdEngine
import com.example.idleprogrammergame.game_logic.GameEngine
import com.example.idleprogrammergame.ui.theme.IdleProgrammerGameTheme
import com.example.idleprogrammergame.ui_components.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

class MainActivity : ComponentActivity() {
    private val gameEngine = GameEngine()
    private lateinit var adEngine: AdEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adEngine = AdEngine(gameEngine)
        adEngine.initialize(this)
        adEngine.setActivity(this)
        
        // Ensure edge-to-edge is enabled for true transparency
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
    val hazeState = remember { HazeState() }
    var selectedTab by remember { mutableStateOf(GameTab.VENTURES) }
    var showProfileModal by remember { mutableStateOf(false) }

    // Using a root Box instead of Scaffold to ensure content spans the entire screen
    // without any hidden system bar paddings interfering.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A10))
    ) {
        // Main content area with Haze source
        // This Box will draw behind both the status bar and navigation bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // Only pad the top for the status bar
            ) {
                JetpackComposeMoneyComponent(
                    balance = gameEngine.formatCurrencyWithSymbol(gameEngine.balance.value),
                    incomePerSecond = "+${gameEngine.formatCurrencyWithSymbol(gameEngine.incomePerSecond.value)} / s"
                )

                Spacer(Modifier.height(16.dp))

                FunnyHeaderButtons(
                    onProfileClick = { showProfileModal = true },
                    onModesClick = { /* Handle Modes */ },
                    onRanksClick = { /* Handle Ranks */ }
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    when (selectedTab) {
                        GameTab.VENTURES -> VenturesScreen(gameEngine)
                        GameTab.TEAM -> TeamScreen(gameEngine)
                        GameTab.UPGRADES -> UpgradesScreen(gameEngine)
                        GameTab.CAREER -> CareerScreen(gameEngine)
                        GameTab.TEST -> CareerScreen(gameEngine)
                    }
                }
            }
        }

        // Floating Bottom Navigation Bar
        // We use navigationBarsPadding here so the "pill" stays above the gesture/button area,
        // but the area underneath it will show the game content (blurred).
        BottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        FloatingAdButton(
            adEngine = adEngine,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 16.dp)
        )
        
        if (showProfileModal) {
            ProfileOverlay(
                lifetimeEarnings = gameEngine.formatCurrencyWithSymbol(gameEngine.getTotalEarnings()),
                totalVentures = gameEngine.getTotalVentures().toString(),
                teamSize = gameEngine.getTotalHires().toString(),
                onDismiss = { showProfileModal = false }
            )
        }
    }
}
