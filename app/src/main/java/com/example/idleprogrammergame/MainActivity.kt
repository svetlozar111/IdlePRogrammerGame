package com.example.idleprogrammergame

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.example.idleprogrammergame.game_logic.*
import com.example.idleprogrammergame.ui.theme.IdleProgrammerGameTheme
import com.example.idleprogrammergame.ui_components.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val gameEngine = GameEngine()
    private lateinit var adEngine: AdEngine
    private val networkManager = NetworkManager()
    private var currentUsername: String = ""
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adEngine = AdEngine(gameEngine)
        adEngine.initialize(this)
        adEngine.setActivity(this)
        credentialManager = CredentialManager.create(this)
        
        enableEdgeToEdge()
        
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        currentUsername = prefs.getString("username", "") ?: ""
        
        setContent {
            IdleProgrammerGameTheme {
                MainContent(
                    gameEngine = gameEngine,
                    adEngine = adEngine,
                    networkManager = networkManager,
                    onGoogleSignIn = { handleGoogleSignIn() },
                    onUsernameUpdate = { updatedUsername ->
                        currentUsername = updatedUsername
                    }
                )
            }
        }
    }

    private fun handleGoogleSignIn() {
        val webClientId = "YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com" 
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .setNonce(UUID.randomUUID().toString())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity
                )
                
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: email.split("@")[0]
                    loginWithUser(displayName)
                }
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Google Sign-In failed", e)
                Toast.makeText(this@MainActivity, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginWithUser(name: String) {
        currentUsername = name
        getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
            .edit().putString("username", name).apply()
        
        setContent {
            IdleProgrammerGameTheme {
                MainContent(gameEngine, adEngine, networkManager, { handleGoogleSignIn() }) { currentUsername = it }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (currentUsername.isNotEmpty()) {
            val data = gameEngine.toPlayerData(currentUsername)
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
    onGoogleSignIn: () -> Unit,
    onUsernameUpdate: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var showLogin by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("username", "") ?: ""
        if (savedUser.isNotEmpty()) {
            username = savedUser
            onUsernameUpdate(savedUser)
            showLogin = false
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
                    } else {
                        networkManager.saveGame(gameEngine.toPlayerData(enteredName))
                    }
                    showLogin = false
                }
            },
            onGoogleSignIn = onGoogleSignIn
        )
    } else {
        GameScreen(gameEngine, adEngine, networkManager, username)
    }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit, onGoogleSignIn: () -> Unit) {
    var text by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF050A10)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("IDLE PROGRAMMER", color = Color(0xFF00FFC6), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Enter your dev name to start", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Dev Username") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFC6), unfocusedBorderColor = Color.Gray),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { if (text.isNotBlank()) onLogin(text) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC6)),
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("RUN APPLICATION", color = Color.Black, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(16.dp))
            Text("OR", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) { Text("Sign in with Google", fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
fun GameScreen(gameEngine: GameEngine, adEngine: AdEngine, networkManager: NetworkManager, username: String) {
    val hazeState = remember { HazeState() }
    var selectedTab by remember { mutableStateOf(GameTab.VENTURES) }
    var showProfileModal by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(30000)
            networkManager.saveGame(gameEngine.toPlayerData(username))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050A10))) {
        Box(modifier = Modifier.fillMaxSize().haze(state = hazeState)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                JetpackComposeMoneyComponent(
                    balance = gameEngine.formatCurrencyWithSymbol(gameEngine.balance.value),
                    incomePerSecond = "+${gameEngine.formatCurrencyWithSymbol(gameEngine.incomePerSecond.value)} / s"
                )
                Spacer(Modifier.height(16.dp))
                FunnyHeaderButtons(
                    onProfileClick = { showProfileModal = true },
                    onModesClick = { 
                        scope.launch {
                            if (networkManager.saveGame(gameEngine.toPlayerData(username))) {
                                Toast.makeText(context, "Game Uploaded", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onRanksClick = { showLeaderboard = true }
                )
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.weight(1f)) {
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
        BottomNavBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, hazeState = hazeState, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        FloatingAdButton(adEngine = adEngine, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 16.dp))
        
        if (showProfileModal) {
            ProfileOverlay(
                lifetimeEarnings = gameEngine.formatCurrencyWithSymbol(gameEngine.getTotalEarnings()),
                totalVentures = gameEngine.getTotalVentures().toString(),
                teamSize = gameEngine.getTotalHires().toString(),
                onDismiss = { showProfileModal = false }
            )
        }

        if (showLeaderboard) {
            LeaderboardDialog(networkManager, onDismiss = { showLeaderboard = false })
        }
    }
}

@Composable
fun LeaderboardDialog(networkManager: NetworkManager, onDismiss: () -> Unit) {
    var leaderboardData by remember { mutableStateOf<LeaderboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf(0) } // 0: Wealth, 1: Prestige, 2: Empire

    LaunchedEffect(Unit) {
        leaderboardData = networkManager.getLeaderboard()
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF050A10).copy(alpha = 0.95f)
        ) {
            Column(modifier = Modifier.padding(24.dp).statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("GLOBAL RANKS", color = Color(0xFF00FFC6), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("Wealth", selectedCategory == 0) { selectedCategory = 0 }
                    CategoryChip("Prestige", selectedCategory == 1) { selectedCategory = 1 }
                    CategoryChip("Empire", selectedCategory == 2) { selectedCategory = 2 }
                }

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00FFC6))
                    }
                } else if (leaderboardData == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Failed to load ranks", color = Color.Gray)
                    }
                } else {
                    val currentList = when(selectedCategory) {
                        0 -> leaderboardData?.topEarnings ?: emptyList()
                        1 -> leaderboardData?.topRebirths ?: emptyList()
                        else -> leaderboardData?.topVentures ?: emptyList()
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(currentList) { index, entry ->
                            LeaderboardItem(index + 1, entry, selectedCategory)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF00FFC6) else Color.DarkGray,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LeaderboardItem(rank: Int, entry: LeaderboardEntry, category: Int) {
    val rankColor = when(rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color.White
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#$rank", color = rankColor, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
        Text(entry.username, color = Color.White, modifier = Modifier.weight(1f))
        
        val valueStr = when(category) {
            0 -> "$${formatCurrency(entry.score)}"
            1 -> "${entry.score.toInt()} Asc"
            else -> "${entry.score.toInt()} Vent"
        }
        Text(valueStr, color = Color(0xFF00FFC6), fontWeight = FontWeight.Bold)
    }
}

fun formatCurrency(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> "${String.format("%.1f", amount / 1_000_000_000)}B"
        amount >= 1_000_000 -> "${String.format("%.1f", amount / 1_000_000)}M"
        amount >= 1_000 -> "${String.format("%.1f", amount / 1_000)}K"
        else -> String.format("%.0f", amount)
    }
}
