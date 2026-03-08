package com.example.idleprogrammergame

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idleprogrammergame.game_logic.AdEngine
import com.example.idleprogrammergame.game_logic.GameEngine
import com.example.idleprogrammergame.game_logic.NetworkManager
import com.example.idleprogrammergame.ui.theme.IdleProgrammerGameTheme
import com.example.idleprogrammergame.ui_components.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val gameEngine = GameEngine()
    private lateinit var adEngine: AdEngine
    private val networkManager = NetworkManager()
    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adEngine = AdEngine(gameEngine)
        adEngine.initialize(this)
        adEngine.setActivity(this)
        
        enableEdgeToEdge()
        
        // Retrieve username from preferences if available
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        currentUsername = prefs.getString("username", "") ?: ""
        
        setContent {
            IdleProgrammerGameTheme {
                MainContent(gameEngine, adEngine, networkManager) { updatedUsername ->
                    currentUsername = updatedUsername
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Save game state when app goes to background or is about to be destroyed
        if (currentUsername.isNotEmpty()) {
            val data = gameEngine.toPlayerData(currentUsername)
            // Use a background scope to ensure saving happens even if activity is finishing
            CoroutineScope(Dispatchers.IO).launch {
                networkManager.saveGame(data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameEngine.stopGameLoop()
    }
}

@Composable
fun MainContent(
    gameEngine: GameEngine, 
    adEngine: AdEngine, 
    networkManager: NetworkManager,
    onUsernameUpdate: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var showLogin by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Load saved username if exists
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("username", "") ?: ""
        if (savedUser.isNotEmpty()) {
            username = savedUser
            onUsernameUpdate(savedUser)
            showLogin = false
            // Auto-load data
            val loadedData = networkManager.loadGame(savedUser)
            if (loadedData != null) {
                gameEngine.loadFromPlayerData(loadedData)
            }
        }
    }

    if (showLogin) {
        LoginScreen(
            onLogin = { enteredName ->
                username = enteredName
                onUsernameUpdate(enteredName)
                context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
                    .edit().putString("username", enteredName).apply()
                
                scope.launch {
                    val loadedData = networkManager.loadGame(enteredName)

                    if (loadedData != null) {
                        gameEngine.loadFromPlayerData(loadedData)
                        Toast.makeText(context, "Welcome back, $enteredName!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "New career started for $enteredName", Toast.LENGTH_SHORT).show()

                        // Create player on server immediately
                        networkManager.saveGame(gameEngine.toPlayerData(enteredName))
                    }
                    showLogin = false
                }
            }
        )
    } else {
        GameScreen(gameEngine, adEngine, networkManager, username)
    }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A10)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "IDLE PROGRAMMER",
                color = Color(0xFF00FFC6),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Enter your dev name to start",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Dev Username") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00FFC6),
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { if (text.isNotBlank()) onLogin(text) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC6)),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RUN APPLICATION", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GameScreen(
    gameEngine: GameEngine, 
    adEngine: AdEngine, 
    networkManager: NetworkManager,
    username: String
) {
    val hazeState = remember { HazeState() }
    var selectedTab by remember { mutableStateOf(GameTab.VENTURES) }
    var showProfileModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Auto-save every 30 seconds
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(30000)
            networkManager.saveGame(gameEngine.toPlayerData(username))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A10))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                JetpackComposeMoneyComponent(
                    balance = gameEngine.formatCurrencyWithSymbol(gameEngine.balance.value),
                    incomePerSecond = "+${gameEngine.formatCurrencyWithSymbol(gameEngine.incomePerSecond.value)} / s"
                )

                Spacer(Modifier.height(16.dp))

                FunnyHeaderButtons(
                    onProfileClick = { showProfileModal = true },
                    onModesClick = { 
                        // Manual Save Button Logic
                        scope.launch {
                            val success = networkManager.saveGame(gameEngine.toPlayerData(username))
                            if (success) {
                                Toast.makeText(context, "Game Uploaded to Cloud", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Cloud Sync Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
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
