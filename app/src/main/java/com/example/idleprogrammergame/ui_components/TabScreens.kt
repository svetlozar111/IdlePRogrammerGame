package com.example.idleprogrammergame.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idleprogrammergame.game_logic.GameEngine
import com.example.idleprogrammergame.game_logic.MIN_XP_TO_ASCEND

@Composable
fun VenturesScreen(gameEngine: GameEngine) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gameEngine.getAllVentures().forEach { venture ->
            item {
                JetpackComposeEarningsFieldComponent(
                    title = venture.title,
                    income = "${gameEngine.formatCurrencyWithSymbol(gameEngine.calculateVentureIncome(venture))} / cycle",
                    totalUpgrade = venture.count,
                    price = gameEngine.formatCurrencyWithSymbol(gameEngine.calculateVenturePrice(venture)),
                    state = if (venture.count > 0) UpgradeState.OWNED else UpgradeState.AVAILABLE,
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gameEngine.getAllHires().forEach { hire ->
            item {
                val requiredVenture = gameEngine.getVenture(hire.ventureId)
                val isUnlocked = requiredVenture != null && requiredVenture.count > 0
                
                HireDevCard(
                    title = hire.title,
                    subtitle = "Automating ${requiredVenture?.title ?: "Unknown"}",
                    price = gameEngine.formatCurrencyWithSymbol(hire.price),
                    state = when {
                        hire.isHired -> HireState.HIRED
                        isUnlocked -> HireState.AVAILABLE
                        else -> HireState.LOCKED
                    },
                    requiredVenture = requiredVenture?.title ?: "this business",
                    onClick = { gameEngine.purchaseHire(hire.id) }
                )
            }
        }
    }
}

@Composable
fun UpgradesScreen(gameEngine: GameEngine) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gameEngine.getAllUpgrades().forEach { upgrade ->
            item {
                val requiredVenture = gameEngine.getVenture(upgrade.ventureId)
                val isUnlocked = requiredVenture != null && requiredVenture.count > 0

                EarningsUpgradeCard(
                    title = upgrade.title,
                    description = upgrade.description,
                    multiplier = "x${upgrade.multiplier.toInt()}",
                    price = gameEngine.formatCurrencyWithSymbol(upgrade.price),
                    state = when {
                        upgrade.isPurchased -> UpgradeState.OWNED
                        isUnlocked -> UpgradeState.AVAILABLE
                        else -> UpgradeState.LOCKED
                    },
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
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