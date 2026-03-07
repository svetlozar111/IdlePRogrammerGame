package com.example.idleprogrammergame.ui_components

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileOverlay(
    lifetimeEarnings: String,
    totalVentures: String,
    teamSize: String,
    onDismiss: () -> Unit
) {
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
            
            ProfileStatRow("Lifetime Earnings", lifetimeEarnings)
            ProfileStatRow("Total Ventures", totalVentures)
            ProfileStatRow("Team Size", teamSize)
            
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
private fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AchievementBadge(emoji: String, title: String) {
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