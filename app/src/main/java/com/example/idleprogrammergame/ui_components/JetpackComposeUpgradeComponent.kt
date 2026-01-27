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
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EarningsUpgradeCard(
    title: String,
    description: String,
    multiplier: String, // e.g. "x2", "x3"
    price: String,
    state: UpgradeState,
    onClick: () -> Unit = {}
) {
    val accent = Color(0xFF00FFC6)
    val bg = Color(0xFF0E141B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                when (state) {
                    UpgradeState.OWNED -> accent
                    UpgradeState.LOCKED -> Color.Gray.copy(alpha = 0.4f)
                    UpgradeState.AVAILABLE -> Color.Transparent
                },
                RoundedCornerShape(20.dp)
            )
            .background(bg)
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {

        // ───── HEADER ─────
        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (state == UpgradeState.OWNED) accent else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = multiplier,
                    color = if (state == UpgradeState.LOCKED) Color.Gray else accent
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = if (state == UpgradeState.LOCKED) Color.Gray else Color.White
                )

                Text(
                    text = when (state) {
                        UpgradeState.LOCKED ->
                            "Requires owning this business"

                        UpgradeState.AVAILABLE ->
                            description

                        UpgradeState.OWNED ->
                            "$multiplier earnings active"
                    },
                    color = if (state == UpgradeState.LOCKED)
                        Color.Gray.copy(alpha = 0.6f)
                    else
                        accent
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ───── ACTION ─────
        when (state) {
            UpgradeState.LOCKED -> {
                DisabledButton("Locked")
            }

            UpgradeState.AVAILABLE -> {
                PrimaryButton("Buy • $price", accent, onClick)
            }

            UpgradeState.OWNED -> {
                SecondaryLabel("Purchased")
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, color: Color, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Black)
    }
}

@Composable
fun DisabledButton(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A2028)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Gray)
    }
}

@Composable
fun SecondaryLabel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Gray)
    }
}

@Preview(showBackground = true)
@Composable
fun EarningsUpgradePreview() {
    Column(
        modifier = Modifier
            .background(Color(0xFF050A10))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EarningsUpgradeCard(
            title = "Better Tools",
            description = "Doubles Bug Fix income",
            multiplier = "x2",
            price = "$174",
            state = UpgradeState.AVAILABLE
        )

        EarningsUpgradeCard(
            title = "Open Source Fame",
            description = "Triples Open Source income",
            multiplier = "x3",
            price = "$720",
            state = UpgradeState.LOCKED
        )

        EarningsUpgradeCard(
            title = "Enterprise Clients",
            description = "Quintuples Freelance income",
            multiplier = "x5",
            price = "$312",
            state = UpgradeState.OWNED
        )
    }
}