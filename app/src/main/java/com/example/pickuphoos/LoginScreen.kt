package com.example.pickuphoos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun LoginScreen(
    onSignInClick: (email: String, password: String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    fun validate(): Boolean {
        var valid = true
        emailError = ""
        passwordError = ""
        if (email.isBlank()) {
            emailError = "Email is required"
            valid = false
        }
        if (password.isBlank()) {
            passwordError = "Password is required"
            valid = false
        }
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
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Logo + App Name
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(OrangeAccent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Replace with your actual app icon
                Text("P", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Pickup Hoos",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "UVA Sports · Sign in to continue",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

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

            // Password Field
            PickupHoosTextField(
                value = password,
                onValueChange = { password = it; passwordError = "" },
                label = "PASSWORD",
                placeholder = "••••••••",
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

            Spacer(modifier = Modifier.height(20.dp))

            // Sign In Button
            Button(
                onClick = { if (validate()) onSignInClick(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Sign In",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider
            OrDivider()

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign-In Button
            OutlinedButton(
                onClick = onGoogleSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
            ) {
                // Replace with actual Google icon drawable
                // Icon(painter = painterResource(R.drawable.ic_google), contentDescription = "Google", modifier = Modifier.size(18.dp))
                Text("G  ", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)
                Text(
                    text = "Sign in with Google",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account Link
            Row(horizontalArrangement = Arrangement.Center) {
                Text("Don't have an account? ", color = TextMuted, fontSize = 13.sp)
                TextButton(
                    onClick = onCreateAccountClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Create one",
                        color = OrangeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
