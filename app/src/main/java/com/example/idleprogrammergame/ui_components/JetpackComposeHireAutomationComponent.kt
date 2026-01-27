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
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class HireState {
    HIRED,
    AVAILABLE,
    LOCKED
}

@Composable
fun HireDevCard(
    title: String,
    subtitle: String,
    price: String,
    state: HireState,
    onClick: () -> Unit = {}
) {
    val accent = Color(0xFF00FFC6)
    val bg = Color(0xFF0E141B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = when (state) {
                    HireState.HIRED -> accent
                    HireState.LOCKED -> Color.Gray.copy(alpha = 0.4f)
                    HireState.AVAILABLE -> Color.Transparent
                },
                shape = RoundedCornerShape(20.dp)
            )
            .background(bg)
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {

        // ───── HEADER ─────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        if (state == HireState.HIRED) accent else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (state == HireState.HIRED) accent else Color.Gray
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = if (state == HireState.LOCKED) Color.Gray else Color.White
                )

                Text(
                    text = when (state) {
                        HireState.HIRED -> "⚡ $subtitle"
                        HireState.AVAILABLE -> subtitle
                        HireState.LOCKED -> "Own at least 1 Open Source first"
                    },
                    color = if (state == HireState.LOCKED)
                        Color.Gray.copy(alpha = 0.6f)
                    else
                        accent
                )
            }

            Spacer(Modifier.weight(1f))

            // Status badge
            when (state) {
                HireState.HIRED -> StatusBadge("HIRED", Color(0xFFFFC107))
                HireState.LOCKED -> StatusBadge("Locked", Color.Gray)
                else -> {}
            }
        }

        // ───── ACTION ─────
        if (state == HireState.AVAILABLE) {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hire • $price",
                    color = Color.Black
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HireDevCardPreview() {
    Column(
        modifier = Modifier
            .background(Color(0xFF050A10))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HireDevCard(
            title = "Junior Dev",
            subtitle = "Automating Bug Fix",
            price = "$1.00K",
            state = HireState.HIRED
        )

        HireDevCard(
            title = "Mid-level Dev",
            subtitle = "Automates Freelance Gig",
            price = "$1.00K",
            state = HireState.AVAILABLE
        )

        HireDevCard(
            title = "Senior Dev",
            subtitle = "Automates Open Source",
            price = "$10.00K",
            state = HireState.LOCKED
        )
    }
}