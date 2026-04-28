package com.example.pickuphoos.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.pickuphoos.model.SportType
import com.example.pickuphoos.ui.theme.*
import com.example.pickuphoos.viewmodel.CreateGameViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(
    viewModel: CreateGameViewModel,
    onBackClick: () -> Unit,
    onGameCreated: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Image picker (gallery)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setThumbnail(it) }
    }

    // Camera launcher
    val cameraUri = remember { viewModel.prepareCameraUri(context) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.setThumbnail(cameraUri)
    }

    var showImageDialog by remember { mutableStateOf(false) }

    // Navigate when game is successfully created
    LaunchedEffect(state.createdGameId) {
        if (state.createdGameId != null) onGameCreated()
    }

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
                        text = "Create a game",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131325))
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                ) {
                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = { viewModel.createGame() },
                    modifier = Modifier.weight(1.5f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    enabled = state.isFormValid && !state.isSubmitting,
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            "Create game",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Thumbnail picker ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10101E))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .clickable { showImageDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (state.thumbnailUri != null) {
                    AsyncImage(
                        model = state.thumbnailUri,
                        contentDescription = "Game thumbnail",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Add a photo of the location", color = TextMuted, fontSize = 11.sp)
                        Text(
                            "Camera or gallery · optional",
                            color = Color(0xFF444444),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Sport ─────────────────────────────────────────────────────────
            SectionLabel("SPORT")
            SportChooser(
                selectedSport = state.sport,
                onSportSelected = { viewModel.setSport(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Location ──────────────────────────────────────────────────────
            SectionLabel("LOCATION")
            FieldRow(
                icon = Icons.Outlined.Place,
                placeholder = "Pick a location",
                value = state.locationName,
                onClick = { /* TODO: location picker */ },
                showChevron = true
            )

            // ── Date + Time row ───────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("DATE")
                    FieldRow(
                        icon = Icons.Outlined.DateRange,
                        placeholder = "Date",
                        value = state.dateLabel,
                        onClick = {
                            showDatePicker(context, state.calendar) { y, m, d ->
                                viewModel.setDate(y, m, d)
                            }
                        }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("TIME")
                    FieldRow(
                        icon = Icons.Outlined.Schedule,
                        placeholder = "Time",
                        value = state.timeLabel,
                        onClick = {
                            showTimePicker(context, state.calendar) { h, mn ->
                                viewModel.setTime(h, mn)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Max Players ───────────────────────────────────────────────────
            SectionLabel("MAX PLAYERS")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(0.5.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Players needed", color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Stepper(
                    value = state.maxPlayers,
                    onDecrement = { viewModel.adjustMaxPlayers(-1) },
                    onIncrement = { viewModel.adjustMaxPlayers(1) }
                )
            }

            // Error message
            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Color(0xFFE24B4A), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Image source dialog
    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            containerColor = DarkSurface,
            title = { Text("Add photo", color = Color.White, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showImageDialog = false
                            cameraLauncher.launch(cameraUri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Take photo", color = OrangeAccent, fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = {
                            showImageDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose from gallery", color = OrangeAccent, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageDialog = false }) {
                    Text("Cancel", color = TextMuted, fontSize = 13.sp)
                }
            }
        )
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF666666),
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 5.dp)
    )
}

@Composable
private fun SportChooser(
    selectedSport: SportType?,
    onSportSelected: (SportType) -> Unit
) {
    val rows = SportType.entries.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { sport ->
                    val selected = sport == selectedSport
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) OrangeAccent else DarkSurface)
                            .clickable { onSportSelected(sport) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sport.displayName,
                            color = if (selected) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String,
    value: String,
    onClick: () -> Unit,
    showChevron: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(0.5.dp, DarkBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value.ifBlank { placeholder },
            color = if (value.isBlank()) Color(0xFF444444) else Color(0xFFCCCCCC),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF444444),
                modifier = Modifier.size(16.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
private fun Stepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkNavy)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onDecrement() },
            contentAlignment = Alignment.Center
        ) {
            Text("−", color = OrangeAccent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            text = "$value",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onIncrement() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = OrangeAccent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Date/Time pickers ─────────────────────────────────────────────────────────

private fun showDatePicker(
    context: android.content.Context,
    initial: Calendar,
    onPicked: (year: Int, month: Int, day: Int) -> Unit
) {
    DatePickerDialog(
        context,
        { _, y, m, d -> onPicked(y, m, d) },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = System.currentTimeMillis() - 1000
    }.show()
}

private fun showTimePicker(
    context: android.content.Context,
    initial: Calendar,
    onPicked: (hour: Int, minute: Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _, h, mn -> onPicked(h, mn) },
        initial.get(Calendar.HOUR_OF_DAY),
        initial.get(Calendar.MINUTE),
        false
    ).show()
}
