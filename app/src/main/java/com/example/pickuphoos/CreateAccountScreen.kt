package com.example.pickuphoos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickuphoos.ui.components.OrDivider
import com.example.pickuphoos.ui.components.PickupHoosTextField
import com.example.pickuphoos.ui.theme.OrangeAccent
import com.example.pickuphoos.ui.theme.DarkNavy
import com.example.pickuphoos.ui.theme.DarkSurface
import com.example.pickuphoos.ui.theme.DarkBorder
import com.example.pickuphoos.ui.theme.TextMuted
import com.example.pickuphoos.ui.theme.TextPrimary
@Composable
fun CreateAccountScreen(
    onCreateAccountClick: (email: String, name: String, password: String) -> Unit,
    onGoogleSignUpClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    // Show soft warning if email filled and not @virginia.edu
    val showUvaWarning by remember(email) {
        derivedStateOf {
            email.isNotBlank() &&
            email.contains("@") &&
            !email.endsWith("@virginia.edu")
        }
    }

    fun validate(): Boolean {
        var valid = true
        emailError = ""
        nameError = ""
        passwordError = ""
        confirmPasswordError = ""

        if (email.isBlank()) { emailError = "Email is required"; valid = false }
        if (name.isBlank()) { nameError = "Name is required"; valid = false }
        if (password.isBlank()) { passwordError = "Password is required"; valid = false }
        else if (password.length < 6) { passwordError = "Password must be at least 6 characters"; valid = false }
        if (confirmPassword.isBlank()) { confirmPasswordError = "Please confirm your password"; valid = false }
        else if (password != confirmPassword) { confirmPasswordError = "Passwords do not match"; valid = false }

        return valid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Create Account",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "Join Pickup Hoos",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // UVA Email Warning Banner
            AnimatedVisibility(
                visible = showUvaWarning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                UvaWarningBanner()
            }

            // Email Field
            PickupHoosTextField(
                value = email,
                onValueChange = { email = it; emailError = "" },
                label = "EMAIL",
                placeholder = "computing_id@virginia.edu",
                keyboardType = KeyboardType.Email,
                errorMessage = emailError
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name Field
            PickupHoosTextField(
                value = name,
                onValueChange = { name = it; nameError = "" },
                label = "NAME",
                placeholder = "e.g. Wahoo",
                keyboardType = KeyboardType.Text,
                errorMessage = nameError
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            PickupHoosTextField(
                value = password,
                onValueChange = { password = it; passwordError = "" },
                label = "PASSWORD",
                placeholder = "Min. 6 characters",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            color = OrangeAccent,
                            fontSize = 12.sp
                        )
                    }
                },
                errorMessage = passwordError
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password Field
            PickupHoosTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmPasswordError = "" },
                label = "CONFIRM PASSWORD",
                placeholder = "Re-enter password",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Text(
                            text = if (confirmPasswordVisible) "Hide" else "Show",
                            color = OrangeAccent,
                            fontSize = 12.sp
                        )
                    }
                },
                errorMessage = confirmPasswordError
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Create Account Button
            Button(
                onClick = { if (validate()) onCreateAccountClick(email, name, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Create Account",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OrDivider()

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign-Up Button
            OutlinedButton(
                onClick = onGoogleSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
            ) {
                Text("G  ", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)
                Text(
                    text = "Sign up with Google",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign In Link
            Row(horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", color = TextMuted, fontSize = 13.sp)
                TextButton(
                    onClick = onSignInClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Sign in",
                        color = OrangeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun UvaWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .background(Color(0xFF2A1F10), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("⚠  ", color = Color(0xFFE8A04A), fontSize = 14.sp)
        Text(
            text = "Pickup Hoos is designed for UVA students. We recommend using your @virginia.edu email.",
            color = Color(0xFFE8A04A),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
