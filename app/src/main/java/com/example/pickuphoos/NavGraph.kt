package com.example.pickuphoos.navigation

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pickuphoos.ui.screens.CreateAccountScreen
import com.example.pickuphoos.ui.screens.LoginScreen
import com.example.pickuphoos.ui.screens.MapScreen
import com.example.pickuphoos.viewmodel.AuthState
import com.example.pickuphoos.viewmodel.AuthViewModel
import com.example.pickuphoos.viewmodel.MapViewModel

// ─── Route constants ──────────────────────────────────────────────────────────

object Routes {
    const val LOGIN      = "login"
    const val CREATE     = "create_account"
    const val PREFERENCE = "preference"
    const val MAP        = "map"
    const val LIST       = "list"
    const val PROFILE    = "profile"
}

// ─── Root NavGraph ────────────────────────────────────────────────────────────

@Composable
fun PickupHoosNavGraph(navController: NavHostController) {

    val authViewModel: AuthViewModel = viewModel()
    val mapViewModel: MapViewModel = viewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val startDestination = if (authViewModel.currentUser != null) Routes.MAP else Routes.LOGIN
//    val startDestination = Routes.LOGIN
    // Use a flag so navigation only fires after NavHost has composed
    var navReady by remember { mutableStateOf(false) }

    LaunchedEffect(authState, navReady) {
        if (!navReady) return@LaunchedEffect
        when (authState) {
            is AuthState.Success -> {
                navController.navigate(Routes.MAP) {
                    popUpTo(0) { inclusive = true }
                }
                authViewModel.resetState()
            }
            is AuthState.NewUser -> {
                navController.navigate(Routes.PREFERENCE) {
                    popUpTo(0) { inclusive = true }
                }
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                Toast.makeText(
                    context,
                    (authState as AuthState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                authViewModel.resetState()
            }
            else -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(Routes.LOGIN) {
            LaunchedEffect(Unit) { navReady = true }
            LoginScreen(
                onSignInClick = { email, password ->
                    authViewModel.signInWithEmail(email, password)
                },
                onGoogleSignInClick = {
                    authViewModel.signInWithGoogle(context)
                },
                onCreateAccountClick = {
                    navController.navigate(Routes.CREATE)
                }
            )
        }

        composable(Routes.CREATE) {
            LaunchedEffect(Unit) { navReady = true }
            CreateAccountScreen(
                onCreateAccountClick = { email, name, password ->
                    authViewModel.createAccountWithEmail(email, name, password)
                },
                onGoogleSignUpClick = {
                    authViewModel.signInWithGoogle(context)
                },
                onSignInClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.PREFERENCE) {
            LaunchedEffect(Unit) { navReady = true }
            // TODO: replace with PreferenceScreen when built
            // For now navigate straight to map so app doesn't hang on blank screen
            LaunchedEffect(Unit) {
                navController.navigate(Routes.MAP) {
                    popUpTo(Routes.PREFERENCE) { inclusive = true }
                }
            }
        }

        composable(Routes.MAP) {
            LaunchedEffect(Unit) { navReady = true }
            MapScreen(
                viewModel = mapViewModel,
                onCreateGameClick = { navController.navigate("create_game") },
                onGameClick = { game -> navController.navigate("game_detail/${game.id}") },
                onListClick = { navController.navigate(Routes.LIST) },
                onProfileClick = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(Routes.LIST) {
            LaunchedEffect(Unit) { navReady = true }
            // TODO: ListScreen
        }

        composable(Routes.PROFILE) {
            LaunchedEffect(Unit) { navReady = true }
            // TODO: ProfileScreen
        }
    }
}