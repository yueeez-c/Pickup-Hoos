package com.example.pickuphoos.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.pickuphoos.model.SportType
import com.example.pickuphoos.ui.theme.*
import com.example.pickuphoos.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSignOutClick: () -> Unit,
    onMapClick: () -> Unit = {},
    onListClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Avatar picker (gallery) ───────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    // ── Camera launcher ───────────────────────────────────────────────────────
    val cameraUri = viewModel.getCameraUri()
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.uploadAvatar(cameraUri)
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
                onClick = { /* open time picker */ }
            )
            PreferenceRow(
                icon = Icons.Outlined.Place,
                label = "Preferred location",
                value = uiState.preferredLocation.ifBlank { "Not set" },
                onClick = { /* open location picker */ }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Privacy Toggles ───────────────────────────────────────────────
            ToggleRow(
                title = "Show contact info",
                subtitle = "Visible to players in your games",
                checked = uiState.showContactInfo,
                onCheckedChange = { viewModel.setShowContactInfo(it) }
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
                            cameraLauncher.launch(cameraUri)
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