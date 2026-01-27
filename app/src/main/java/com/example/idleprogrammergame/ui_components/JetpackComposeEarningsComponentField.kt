package com.example.idleprogrammergame.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

enum class UpgradeState {
    LOCKED,
    AVAILABLE,
    OWNED
}

@Composable
fun JetpackComposeEarningsFieldComponent(
    title: String,
    income: String,
    totalUpgrade: Int,
    price: String,
    state: UpgradeState,
    isAutomated: Boolean = false,
    progress: Float = 0f,
    onButtonClick: () -> Unit = {},
    onManualClick: () -> Unit = {}
) {
    val accent = Color(0xFF00FFD9)
    val bg = Color(0xFF0E141B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(16.dp)
            .clickable(onClick = onManualClick)
    ) {

        // ───── HEADER ROW ─────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Icon container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, accent, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = accent
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White
                )
                Text(
                    text = income,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.weight(1f))

            // Count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))   // 👈 smaller radius = squarer
                    .border(
                        width = 1.dp,
                        color = accent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = totalUpgrade.toString(),
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ───── OWNED STATE EXTRA UI ─────
        if (state == UpgradeState.OWNED) {
            Spacer(Modifier.height(12.dp))

            val animatedProgress = remember { Animatable(0f) }
            LaunchedEffect(progress) {
                animatedProgress.animateTo(
                    targetValue = progress,
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.EaseOutQuad
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.value)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (isAutomated) {
                Text(
                    text = "⚡ Automated",
                    color = accent,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Text(
                    text = "⏱️ Manual",
                    color = Color.Yellow,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ───── ACTION BUTTON ─────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when (state) {
                        UpgradeState.LOCKED -> Color(0xFF1A2028)
                        else -> accent
                    }
                )
                .clickable(onClick = { 
                    // Stop propagation to prevent both button and column click from firing
                    onButtonClick() 
                }),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (state) {
                    UpgradeState.LOCKED -> "Unlock • $price"
                    UpgradeState.AVAILABLE -> "Buy • $price"
                    UpgradeState.OWNED -> "Buy • $price"
                },
                color = if (state == UpgradeState.LOCKED) Color.Gray else Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JetpackComposeEarningsFieldPreview() {
    Column(
        modifier = Modifier
            .background(Color(0xFF050A10))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        JetpackComposeEarningsFieldComponent(
            title = "Open Source",
            income = "Community contributions",
            totalUpgrade = 0,
            price = "$720",
            state = UpgradeState.LOCKED
        )

        JetpackComposeEarningsFieldComponent(
            title = "Bug Fix",
            income = "$156 / cycle",
            totalUpgrade = 27,
            price = "$174",
            state = UpgradeState.OWNED
        )

        JetpackComposeEarningsFieldComponent(
            title = "Bug String",
            income = "$10 / cycle",
            totalUpgrade = 100,
            price = "$16",
            state = UpgradeState.AVAILABLE
        )
    }
}