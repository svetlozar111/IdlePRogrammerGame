package com.example.idleprogrammergame.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SkillUpgradeCard(
    title: String,
    description: String,
    level: Int,
    maxLevel: Int,
    cost: Long,
    canAfford: Boolean,
    onClick: () -> Unit = {}
) {
    val xpColor = Color(0xFFBB86FC) // Purple for XP
    val bg = Color(0xFF0E141B)
    val isMaxed = level >= maxLevel

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (isMaxed) xpColor else Color.Gray.copy(alpha = 0.2f),
                RoundedCornerShape(20.dp)
            )
            .background(bg)
            .padding(16.dp)
            .clickable(enabled = !isMaxed && canAfford, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Lvl $level/$maxLevel",
                    color = xpColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Level Progress Bar
        LinearProgressIndicator(
            progress = level.toFloat() / maxLevel.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = xpColor,
            trackColor = Color.DarkGray
        )

        Spacer(Modifier.height(16.dp))

        // Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        isMaxed -> Color.DarkGray
                        canAfford -> xpColor
                        else -> Color.Gray.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isMaxed) "MAXED" else "Upgrade • ${cost} XP",
                color = if (canAfford && !isMaxed) Color.Black else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
fun SkillUpgradePreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkillUpgradeCard(
            title = "Legacy Knowledge",
            description = "+10% Income per level",
            level = 2,
            maxLevel = 10,
            cost = 15,
            canAfford = true
        )
        SkillUpgradeCard(
            title = "Efficient Workflow",
            description = "-5% Venture costs",
            level = 10,
            maxLevel = 10,
            cost = 100,
            canAfford = false
        )
    }
}