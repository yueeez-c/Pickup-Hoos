package com.example.pickuphoos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickuphoos.model.Game
import com.example.pickuphoos.model.SportType
import com.example.pickuphoos.ui.theme.*
import com.example.pickuphoos.viewmodel.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

// UVA Grounds center coordinate
private val UVA_CENTER = LatLng(38.0336, -78.5080)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onCreateGameClick: () -> Unit,
    onGameClick: (Game) -> Unit,
    onListClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val games by viewModel.filteredGames.collectAsStateWithLifecycle()
    val selectedSport by viewModel.selectedSport.collectAsStateWithLifecycle()
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(UVA_CENTER, 15f)
    }

    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        BottomSheetScaffold(
            scaffoldState = sheetState,
            sheetPeekHeight = 200.dp,
            sheetContainerColor = DarkNavy,
            sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = Color.Transparent,
            modifier = Modifier.weight(1f),
            sheetContent = {
                GameBottomSheet(
                    games = games,
                    selectedSport = selectedSport,
                    onJoinClick = { game -> viewModel.joinGame(game) },
                    onGameClick = onGameClick
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = false,
                        compassEnabled = false
                    )
                ) {
                    games.forEach { game ->
                        GameMarker(
                            game = game,
                            isSelected = game.id == selectedGame?.id,
                            onClick = {
                                viewModel.selectGame(game)
                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(game.location, 16f)
                                )
                            }
                        )
                    }
                }

                SportFilterRow(
                    selectedSport = selectedSport,
                    onSportSelected = { viewModel.setFilter(it) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )

                FloatingActionButton(
                    onClick = onCreateGameClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
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
        }

        PickupHoosBottomNav(
            currentRoute = "map",
            onMapClick = {},
            onListClick = onListClick,
            onProfileClick = onProfileClick
        )
    }
}

// ── Sport Filter Chip Row ─────────────────────────────────────────────────────

@Composable
fun SportFilterRow(
    selectedSport: SportType?,
    onSportSelected: (SportType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sports = listOf(null) + SportType.entries  // null = "All"

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sports.forEach { sport ->
            val isActive = sport == selectedSport
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) OrangeAccent else DarkSurface)
                    .clickable { onSportSelected(sport) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = sport?.displayName ?: "All",
                    color = if (isActive) Color.White else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ── Custom Map Marker ─────────────────────────────────────────────────────────

@Composable
fun GameMarker(
    game: Game,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    MarkerComposable(
        state = MarkerState(position = game.location),
        onClick = { onClick(); true }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (isSelected) 44.dp else 36.dp)
                    .clip(CircleShape)
                    .background(game.sport.pinColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = game.sport.emoji,
                    fontSize = if (isSelected) 18.sp else 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(DarkNavy, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${game.sport.displayName} · ${game.format}",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Bottom Sheet Content ──────────────────────────────────────────────────────

@Composable
fun GameBottomSheet(
    games: List<Game>,
    selectedSport: SportType?,
    onJoinClick: (Game) -> Unit,
    onGameClick: (Game) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 16.dp)
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF333344))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (selectedSport != null) "${selectedSport.displayName} games nearby"
            else "Nearby games",
            color = TextMuted,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (games.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No games nearby right now",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            games.forEach { game ->
                GameCard(
                    game = game,
                    onJoinClick = { onJoinClick(game) },
                    onCardClick = { onGameClick(game) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun GameCard(
    game: Game,
    onJoinClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F0F1A))
            .clickable { onCardClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sport color dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(game.sport.pinColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${game.sport.displayName} · ${game.locationName}",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${game.timeLabel} · ${game.playersJoined}/${game.maxPlayers} players",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (game.isJoined) {
            OutlinedButton(
                onClick = {},
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
// ── Bottom Navigation Bar ─────────────────────────────────────────────────────

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun PickupHoosBottomNav(
    currentRoute: String,
    onMapClick: () -> Unit,
    onListClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val items = listOf(
        BottomNavItem("Map", "map", Icons.Outlined.Map),
        BottomNavItem("List", "list", Icons.Outlined.List),
        BottomNavItem("Profile", "profile", Icons.Outlined.Person)
    )

    NavigationBar(
        containerColor = DarkNavy,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = {
                    when (item.route) {
                        "map"     -> onMapClick()
                        "list"    -> onListClick()
                        "profile" -> onProfileClick()
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangeAccent,
                    selectedTextColor = OrangeAccent,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}