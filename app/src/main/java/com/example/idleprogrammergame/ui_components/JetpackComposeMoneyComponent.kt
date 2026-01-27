package com.example.idleprogrammergame.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JetpackComposeMoneyComponent(
    balance: String = "$297.55K",
    incomePerSecond: String = "+$313 / s"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(16.dp)
            .shadow(
                elevation = 25.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF00FFC6).copy(alpha = 0.80f),
                spotColor = Color(0xFF00FFEA).copy(alpha = 0.70f)
            )

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0E141B))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center// 👈 center children
        ) {

            Text(
                text = balance,
                color = Color(0xFF00FFD9),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF101820))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = incomePerSecond,
                    color = Color(0xFF00FFD9),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JetpackComposeMoneyComponentPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF050A10)) // dark app background
            .padding(40.dp)                // space for shadow to breathe
    ) {
        JetpackComposeMoneyComponent()
    }
}