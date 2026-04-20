package com.alarm.meluko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.meluko.ui.theme.MelukoTheme

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Expressive Logo/Icon Placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Brand Name with Expressive Typography
            Text(
                text = "Meluko",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Wake up together.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Login Bento Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Join the Tribe",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onLoginSuccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Get Started",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By continuing, you agree to our Social Sleep Terms.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(150.dp))
            Text(
                text = "Kriaa Inc. All Rights Reserved",
                style = MaterialTheme.typography.labelSmall

            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MelukoTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
