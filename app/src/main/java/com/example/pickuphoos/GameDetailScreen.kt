package com.example.pickuphoos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.pickuphoos.ui.theme.*
import com.example.pickuphoos.viewmodel.GameDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val game = state.game

    Scaffold(
        containerColor = DarkNavy,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = TextMuted
                        )
                    }
                },
                title = {
                    Text(
                        text = "Game details",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF131325)
                )
            )
        },
        bottomBar = {
            if (game != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131325))
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    when {
                        state.isHost -> Button(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text("You are the host", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        state.isJoined -> OutlinedButton(
                            onClick = { viewModel.toggleJoin() },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0392B)),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x66C0392B)),
                            enabled = !state.isSubmitting
                        ) {
                            Text("Leave game", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        game.playersJoined >= game.maxPlayers -> Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = DarkSurface,
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Text("Game is full", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        else -> Button(
                            onClick = { viewModel.toggleJoin() },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            enabled = !state.isSubmitting
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("Join game", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading || game == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangeAccent)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Hero ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(game.sport.pinColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (game.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = game.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(game.sport.pinColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(game.sport.emoji, fontSize = 28.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x227B6EF6))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Hosted by ${game.hostName}",
                        color = Color(0xFF9B8FF8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Detail block ──────────────────────────────────────────────────
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${game.sport.displayName} at ${game.locationName}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    if (game.format.isNotBlank()) {
                        Text(game.format, color = Color(0xFF666666), fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF444444))
                        )
                    }
                    Text(game.timeLabel, color = Color(0xFF666666), fontSize = 11.sp)
                }

                // Info grid
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoCard(
                        label = "LOCATION",
                        value = game.locationName,
                        icon = Icons.Outlined.Place,
                        modifier = Modifier.weight(1f)
                    )
                    InfoCard(
                        label = "TIME",
                        value = game.timeLabel,
                        icon = Icons.Outlined.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Players section ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAYERS JOINED",
                    color = TextMuted,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${game.playersJoined} / ${game.maxPlayers}",
                    color = OrangeAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                state.playerNames.forEachIndexed { index, name ->
                    val playerUid = state.playerUids.getOrNull(index) ?: ""
                    val isCurrentUser = playerUid == state.currentUid
                    val showContactInfo = state.playerContactInfoVisibility[playerUid] ?: false
                    val avatarUrl = state.playerAvatarUrls[playerUid] ?: ""

                    PlayerRow(
                        name = name,
                        avatarUrl = avatarUrl,
                        showAvatar = showContactInfo || isCurrentUser, // Always show own avatar
                        isHost = index == 0 && name == game.hostName,
                        isYou = isCurrentUser
                    )
                }

                val openSpots = (game.maxPlayers - game.playersJoined).coerceAtLeast(0)
                repeat(openSpots) {
                    PlayerRow(name = "Open spot", isEmpty = true)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun InfoCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF131325))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF555555),
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 3.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = value,
                color = Color(0xFFE4E4E4),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PlayerRow(
    name: String,
    avatarUrl: String = "",
    showAvatar: Boolean = false,
    isHost: Boolean = false,
    isYou: Boolean = false,
    isEmpty: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (isEmpty) DarkSurface
                    else avatarColorFor(name)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isEmpty && showAvatar && avatarUrl.isNotBlank()) {
                // Show real avatar image
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar for $name",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Show first letter or question mark
                Text(
                    text = if (isEmpty) "?" else name.firstOrNull()?.uppercase() ?: "?",
                    color = if (isEmpty) Color(0xFF444444) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = name,
            color = if (isEmpty) Color(0xFF444444) else Color(0xFFCCCCCC),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        if (isHost) {
            Tag("Host", Color(0xFF9B8FF8))
        } else if (isYou) {
            Tag("You", OrangeAccent)
        }
    }

    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF181828))
}

@Composable
private fun Tag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

private fun avatarColorFor(name: String): Color {
    val colors = listOf(
        Color(0xFFFF6B2B), Color(0xFF4AC26B), Color(0xFF7B6EF6),
        Color(0xFFE8A04A), Color(0xFFD94A6B), Color(0xFF4A90D9)
    )
    val index = (name.hashCode().rem(colors.size) + colors.size).rem(colors.size)
    return colors[index]
}