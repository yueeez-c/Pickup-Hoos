package com.example.pickuphoos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.pickuphoos.model.SportType
import com.example.pickuphoos.ui.theme.*
import com.example.pickuphoos.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSignOutClick: () -> Unit,
    onMapClick: () -> Unit = {},
    onListClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Time picker state ──────────────────────────────────────────────────
    var showTimePicker by remember { mutableStateOf(false) }

    // ── Avatar picker (gallery) ───────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    // ── Camera permission launcher ────────────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && uiState.cameraUri != null) {
            viewModel.uploadAvatar(uiState.cameraUri!!)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, get URI and launch camera
            val cameraUri = viewModel.getCameraUri()
            cameraLauncher.launch(cameraUri)
        }
        // If denied, silently do nothing (user will tap again if they want to retry)
    }

    var showAvatarDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
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
                currentRoute = "profile",
                onMapClick = onMapClick,
                onListClick = onListClick,
                onProfileClick = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Avatar ────────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(80.dp)
                    .clickable { showAvatarDialog = true }
            ) {
                if (uiState.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = uiState.avatarUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(OrangeAccent),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(OrangeAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.name.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Camera badge
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Change photo",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(uiState.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(uiState.email, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // ── Sport Interests ───────────────────────────────────────────────
            SectionLabel("SPORT INTERESTS")
            SportChipGrid(
                selectedSports = uiState.sportPreferences,
                onToggle = { sport -> viewModel.toggleSport(sport) }
            )

            // ── Preferences ───────────────────────────────────────────────────
            SectionLabel("PREFERENCES")

            PreferenceRow(
                icon = Icons.Outlined.Schedule,
                label = "Preferred time",
                value = uiState.preferredTime.ifBlank { "Not set" },
                onClick = { showTimePicker = true }
            )
//            PreferenceRow(
//                icon = Icons.Outlined.Place,
//                label = "Preferred location",
//                value = uiState.preferredLocation.ifBlank { "Not set" },
//                onClick = { /* open location picker */ }
//            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Privacy Toggles ───────────────────────────────────────────────
            ToggleRow(
                title = "Show Avatar",
                subtitle = "Visible to players in your games",
                checked = uiState.showAvatar,
                onCheckedChange = { viewModel.setShowAvatar(it) }
            )
            ToggleRow(
                title = "Show my games only",
                subtitle = "Default list view filter",
                checked = uiState.showMyGamesOnly,
                onCheckedChange = { viewModel.setShowMyGamesOnly(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Log Out ───────────────────────────────────────────────────────
            OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0392B)),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x44C0392B))
            ) {
                Text("Log out", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Avatar source dialog ──────────────────────────────────────────────────
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            containerColor = DarkSurface,
            title = { Text("Change photo", color = Color.White, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = {
                            showAvatarDialog = false
                            // Check permission first before launching camera
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                // Permission granted, get URI and launch camera
                                val cameraUri = viewModel.getCameraUri()
                                cameraLauncher.launch(cameraUri)
                            } else {
                                // Request permission
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Take photo", color = OrangeAccent, fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = {
                            showAvatarDialog = false
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
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text("Cancel", color = TextMuted, fontSize = 13.sp)
                }
            }
        )
    }

    // ── Time Picker Dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        TimePickerDialog(
            currentTime = uiState.preferredTime,
            onTimeSelected = { timeString ->
                viewModel.setPreferredTime(timeString)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

// ── Time Picker Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    currentTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse current time or default to current hour
    val calendar = Calendar.getInstance()
    val (initialHour, initialMinute) = if (currentTime.isNotBlank()) {
        try {
            val parts = currentTime.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].substringBefore(" ").toInt()
            val isPM = currentTime.contains("PM")
            val hour24 = if (isPM && hour != 12) hour + 12 else if (!isPM && hour == 12) 0 else hour
            hour24 to minute
        } catch (e: Exception) {
            calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
        }
    } else {
        calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
    }

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                "Select preferred time",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0xFF1A1A2E),
                        selectorColor = OrangeAccent,
                        containerColor = DarkSurface,
                        periodSelectorBorderColor = DarkBorder,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = TextMuted,
                        periodSelectorSelectedContainerColor = OrangeAccent,
                        periodSelectorUnselectedContainerColor = Color(0xFF1A1A2E),
                        periodSelectorSelectedContentColor = Color.White,
                        periodSelectorUnselectedContentColor = TextMuted,
                        timeSelectorSelectedContainerColor = OrangeAccent,
                        timeSelectorUnselectedContainerColor = Color(0xFF1A1A2E),
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = TextMuted
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val amPm = if (hour >= 12) "PM" else "AM"
                    val displayHour = when (hour) {
                        0 -> 12
                        in 1..12 -> hour
                        else -> hour - 12
                    }
                    val timeString = String.format("%d:%02d %s", displayHour, minute, amPm)
                    onTimeSelected(timeString)
                }
            ) {
                Text("Set", color = OrangeAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontSize = 14.sp)
            }
        }
    )
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp)
    )
}

@Composable
private fun SportChipGrid(
    selectedSports: List<String>,
    onToggle: (SportType) -> Unit
) {
    // Chunk sports into rows of 3 to avoid FlowRow API incompatibility
    val rows = SportType.entries.chunked(3)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { rowSports ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowSports.forEach { sport ->
                    val selected = sport.name in selectedSports
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) OrangeAccent else DarkSurface)
                            .clickable { onToggle(sport) }
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
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun PreferenceRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = Color(0xFFCCCCCC), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextMuted, fontSize = 12.sp)
        Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFFCCCCCC), fontSize = 13.sp)
            Text(subtitle, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OrangeAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = DarkSurface,
                uncheckedBorderColor = DarkBorder
            )
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF181828))
}