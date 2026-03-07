package com.example.idleprogrammergame.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idleprogrammergame.GameTab
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

@Composable
fun BottomNavBar(
    selectedTab: GameTab,
    onTabSelected: (GameTab) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 16.dp) // Reduced horizontal padding to give more width
            .fillMaxWidth()
            .height(64.dp) // Slightly reduced height
            .clip(RoundedCornerShape(20.dp))
            .hazeChild(
                state = hazeState,
                style = HazeDefaults.style(
                    backgroundColor = Color(0xFF0E141B).copy(alpha = 0.5f),
                    blurRadius = 24.dp,
                    noiseFactor = 0.1f
                )
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Ventures",
                selected = selectedTab == GameTab.VENTURES,
                modifier = Modifier.weight(1f)
            ) { onTabSelected(GameTab.VENTURES) }

            NavItem(
                label = "Team",
                selected = selectedTab == GameTab.TEAM,
                modifier = Modifier.weight(1f)
            ) { onTabSelected(GameTab.TEAM) }

            NavItem(
                label = "Upgrades",
                selected = selectedTab == GameTab.UPGRADES,
                modifier = Modifier.weight(1f)
            ) { onTabSelected(GameTab.UPGRADES) }

            NavItem(
                label = "Career",
                selected = selectedTab == GameTab.CAREER,
                modifier = Modifier.weight(1f)
            ) { onTabSelected(GameTab.CAREER) }

            NavItem(
                label = "Career",
                selected = selectedTab == GameTab.TEST,
                modifier = Modifier.weight(1f)
            ) { onTabSelected(GameTab.CAREER) }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = Color(0xFF00FFC6)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp, // Slightly smaller font
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(width = 12.dp, height = 2.dp)
                    .background(accent, RoundedCornerShape(1.dp))
            )
        }
    }
}