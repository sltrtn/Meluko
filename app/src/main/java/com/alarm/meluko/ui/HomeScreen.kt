package com.alarm.meluko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "MELUKO",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { /* Add Group */ },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(36.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Hero Bento Card: The Alarm
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(40.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Next Wake Up",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "07:30",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "AM",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Streak Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("12", style = MaterialTheme.typography.displayMedium)
                            Text("DAY STREAK", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Freeze Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("3", style = MaterialTheme.typography.displayMedium)
                            Text("FREEZES", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "The Tribe",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Tribe Masonry-style Grid
            val friends = listOf("You", "Sachin", "Aisha", "Joe")
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(friends) { friend ->
                    FriendCard(name = friend)
                }
            }
        }
    }
}

@Composable
fun FriendCard(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (name == "You") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomStart) {
            Text(name, style = MaterialTheme.typography.titleLarge)
        }
    }
}
