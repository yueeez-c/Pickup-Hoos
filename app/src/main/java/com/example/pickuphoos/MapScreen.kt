package com.example.pickuphoos.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private val UVA_CENTER = LatLng(38.0356, -78.5036)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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

    Scaffold(
        containerColor = DarkNavy,
        bottomBar = {
            PickupHoosBottomNav(
                currentRoute = "map",
                onMapClick = {},
                onListClick = onListClick,
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
    ) {
            Box(

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


            }

//        scaffoldPadding ->BottomSheetScaffold(
//            scaffoldState = sheetState,
//            sheetPeekHeight = 180.dp,
//            sheetContainerColor = DarkNavy,
//            sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
//            containerColor = Color.Transparent,
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(scaffoldPadding)
//            ,
//            sheetContent = {
//                GameBottomSheet(
//                    games = games,
//                    selectedSport = selectedSport,
//                    onJoinClick = { game -> viewModel.joinGame(game) },
//                    onGameClick = onGameClick
//                )
//            }
//        ) { innerPadding ->
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(innerPadding)
//            ) {
//                GoogleMap(
//                    modifier = Modifier.fillMaxSize(),
//                    cameraPositionState = cameraPositionState,
//                    properties = MapProperties(
//                        isMyLocationEnabled = hasLocationPermission,
//                        mapType = MapType.NORMAL
//                    ),
//                    uiSettings = MapUiSettings(
//                        myLocationButtonEnabled = false,
//                        zoomControlsEnabled = false,
//                        compassEnabled = false
//                    )
//                ) {
//                    games.forEach { game ->
//                        GameMarker(
//                            game = game,
//                            isSelected = game.id == selectedGame?.id,
//                            onClick = {
//                                viewModel.selectGame(game)
//                                cameraPositionState.move(
//                                    CameraUpdateFactory.newLatLngZoom(game.location, 16f)
//                                )
//                            }
//                        )
//                    }
//                }
//
//                SportFilterRow(
//                    selectedSport = selectedSport,
//                    onSportSelected = { viewModel.setFilter(it) },
//                    modifier = Modifier
//                        .align(Alignment.TopCenter)
//                        .padding(top = 8.dp)
//                )
//
//                FloatingActionButton(
//                    onClick = onCreateGameClick,
//                    modifier = Modifier
//                        .align(Alignment.BottomEnd)
//                        .padding(end = 16.dp, bottom = 16.dp),
//                    containerColor = OrangeAccent,
//                    shape = RoundedCornerShape(14.dp),
//                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Add,
//                        contentDescription = "Create Game",
//                        tint = Color.White,
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//            }
//        }
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

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkNavy.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sports) { sport ->
            val selected = sport == selectedSport
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) OrangeAccent else DarkSurface)
                    .clickable { onSportSelected(sport) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (sport == null) "All" else sport.displayName,
                    color = if (selected) Color.White else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ── Game Bottom Sheet ─────────────────────────────────────────────────────────

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
            .heightIn(min = 180.dp, max = 500.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF333344))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby games",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${games.size} ${if (selectedSport == null) "total" else selectedSport.displayName}",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games) { game ->
                GameCard(
                    game = game,
                    onJoinClick = { onJoinClick(game) },
                    onClick = { onGameClick(game) }
                )
            }
        }
    }
}

// ── Game Card ─────────────────────────────────────────────────────────────────

@Composable
fun GameCard(
    game: Game,
    onJoinClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sport icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(game.sport.pinColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(game.sport.emoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.sport.displayName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = game.locationName,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "${game.playersJoined}/${game.maxPlayers} • ${game.timeLabel}",
                    color = Color(0xFF999999),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (game.isJoined) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OrangeAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Joined", color = OrangeAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            } else if (game.playersJoined < game.maxPlayers) {
                OutlinedButton(
                    onClick = onJoinClick,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Game Marker ───────────────────────────────────────────────────────────────

@Composable
fun GameMarker(
    game: Game,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    MarkerComposable(
        state = MarkerState(position = game.location),
        onClick = {
            onClick()
            true
        }
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 48.dp else 40.dp)
                .clip(CircleShape)
                .background(if (isSelected) game.sport.pinColor else game.sport.pinColor.copy(alpha = 0.9f))
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = game.sport.emoji,
                fontSize = if (isSelected) 22.sp else 18.sp
            )
        }
    }
}

// ── Bottom Navigation ─────────────────────────────────────────────────────────

@Composable
fun PickupHoosBottomNav(
    currentRoute: String,
    onMapClick: () -> Unit,
    onListClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF131325),
        contentColor = TextMuted,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "map",
            onClick = onMapClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = "Map",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Map", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangeAccent,
                selectedTextColor = OrangeAccent,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )

        NavigationBarItem(
            selected = currentRoute == "list",
            onClick = onListClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.List,
                    contentDescription = "List",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("List", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangeAccent,
                selectedTextColor = OrangeAccent,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )

        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = onProfileClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Profile", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangeAccent,
                selectedTextColor = OrangeAccent,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
    }
}