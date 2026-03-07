package com.example.idleprogrammergame.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FunnyHeaderButtons(
    onProfileClick: () -> Unit = {},
    onModesClick: () -> Unit = {},
    onRanksClick: () -> Unit = {}
) {
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
            onClick = onModesClick
        )

        // RIGHT BUTTON: LEADERBOARD
        HeaderButton(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 32.dp, bottomEnd = 32.dp)),
            icon = Icons.Default.Star,
            label = "RANKS",
            onClick = onRanksClick
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