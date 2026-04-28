package com.example.pickuphoos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickuphoos.model.Game
import com.example.pickuphoos.ui.theme.*
import com.example.pickuphoos.viewmodel.MapViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.*
import com.example.pickuphoos.viewmodel.ProfileViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: MapViewModel,
    profileViewModel: ProfileViewModel, // Add this parameter
    onGameClick: (Game) -> Unit,
    onCreateGameClick: () -> Unit,
    onMapClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val games by viewModel.filteredGames.collectAsStateWithLifecycle()
    val selectedSport by viewModel.selectedSport.collectAsStateWithLifecycle()

    // Get showMyGamesOnly from ProfileViewModel instead of local state
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val showMyGamesOnly = profileState.showMyGamesOnly

    val currentUid = Firebase.auth.currentUser?.uid ?: ""

    val visibleGames = remember(games, showMyGamesOnly) {
        val filtered = if (showMyGamesOnly) {
            games.filter { it.isJoined || it.hostUid == currentUid }
        } else games

        filtered.sortedBy { it.startTime?.toDate()?.time ?: Long.MAX_VALUE }
    }

    val grouped = remember(visibleGames) { groupGamesByDay(visibleGames) }

    Scaffold(
        containerColor = DarkNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Games",
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
            PickupHoosBottomNav(
                currentRoute = "list",
                onMapClick = onMapClick,
                onListClick = {},
                onProfileClick = onProfileClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateGameClick,
                containerColor = OrangeAccent,
                shape = RoundedCornerShape(14.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Game",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Show my games toggle ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = showMyGamesOnly,
                    onCheckedChange = { profileViewModel.setShowMyGamesOnly(it) }, // Update via ProfileViewModel
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OrangeAccent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = DarkSurface,
                        uncheckedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.scale(0.85f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Show my games only",
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp
                )
            }

            // ── Sport filter chips ────────────────────────────────────────────
            SportFilterRow(
                selectedSport = selectedSport,
                onSportSelected = { viewModel.setFilter(it) },
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // ── Game list ─────────────────────────────────────────────────────
            if (visibleGames.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showMyGamesOnly) "You haven't joined any games yet"
                        else "No games found",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    grouped.forEach { (sectionLabel, sectionGames) ->
                        item(key = "section-$sectionLabel") {
                            Text(
                                text = sectionLabel,
                                color = TextMuted,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp)
                            )
                        }
                        items(sectionGames, key = { it.id }) { game ->
                            ListGameCard(
                                game = game,
                                isHost = game.hostUid == currentUid,
                                onJoinClick = { viewModel.joinGame(game) },
                                onCardClick = { onGameClick(game) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ── Game Card ─────────────────────────────────────────────────────────────────

@Composable
private fun ListGameCard(
    game: Game,
    isHost: Boolean,
    onJoinClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131325))
            .clickable { onCardClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sport thumbnail
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(game.sport.pinColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = game.sport.emoji,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${game.sport.displayName} · ${game.locationName}",
                    color = Color(0xFFE4E4E4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (isHost) {
                    StatusBadge("Host", Color(0xFF7B6EF6))
                } else if (game.isJoined) {
                    StatusBadge("Joined", OrangeAccent)
                }
            }
            Text(
                text = game.timeLabel,
                color = Color(0xFF666666),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (game.format.isNotBlank()) {
                    Text(game.format, color = TextMuted, fontSize = 10.sp)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF444444))
                    )
                }
                Text(
                    text = "${game.playersJoined}/${game.maxPlayers} players",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (game.isJoined || isHost) {
            OutlinedButton(
                onClick = {},
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAccent),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, OrangeAccent)
            ) {
                Text("Joined", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Button(
                onClick = onJoinClick,
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Join", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Day grouping logic ────────────────────────────────────────────────────────

private fun groupGamesByDay(games: List<Game>): List<Pair<String, List<Game>>> {
    val now = Calendar.getInstance()
    val today = now.clone() as Calendar
    val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val weekEnd = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }

    val sectionToday = mutableListOf<Game>()
    val sectionTomorrow = mutableListOf<Game>()
    val sectionThisWeek = mutableListOf<Game>()
    val sectionLater = mutableListOf<Game>()

    games.forEach { game ->
        val gameTime = game.startTime?.toDate() ?: return@forEach
        val gameCal = Calendar.getInstance().apply { time = gameTime }

        when {
            isSameDay(gameCal, today) -> sectionToday.add(game)
            isSameDay(gameCal, tomorrow) -> sectionTomorrow.add(game)
            gameCal.before(weekEnd) -> sectionThisWeek.add(game)
            else -> sectionLater.add(game)
        }
    }

    return buildList {
        if (sectionToday.isNotEmpty())     add("TODAY"     to sectionToday)
        if (sectionTomorrow.isNotEmpty())  add("TOMORROW"  to sectionTomorrow)
        if (sectionThisWeek.isNotEmpty())  add("THIS WEEK" to sectionThisWeek)
        if (sectionLater.isNotEmpty())     add("LATER"     to sectionLater)
    }
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)