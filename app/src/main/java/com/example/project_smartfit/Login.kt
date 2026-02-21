package com.example.project_smartfit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

// ── Design tokens matching dark theme ─────────────────────────────────
private val BgDark       = Color(0xFF0A0A0F)
private val CardBg       = Color(0xFF161622).copy(alpha = 0.85f)
private val CardBorder   = Color(0xFF00E676).copy(alpha = 0.25f)
private val NeonGreen    = Color(0xFF00E676)
private val NeonGreenSub = Color(0xFF69F0AE)
private val SlateLabel   = Color(0xFF94A3B8)
private val SlateBorder  = Color(0xFF334155)
private val ErrorRed     = Color(0xFFFF6B6B)

@Composable
fun Login(
    navController: NavController,
    sessionManager: SessionManager
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message  by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Entrance animations
    val logoAlpha    = remember { Animatable(0f) }
    val formAlpha    = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        sessionManager.clearSession()
        logoAlpha.animateTo(1f, animationSpec = tween(600))
    }
    LaunchedEffect(Unit) {
        delay(200)
        formAlpha.animateTo(1f, animationSpec = tween(600))
    }
    LaunchedEffect(Unit) {
        delay(400)
        buttonsAlpha.animateTo(1f, animationSpec = tween(600))
    }

    // Floating animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_float"
    )

    // Full background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgDark, Color(0xFF0D1B2A), BgDark)
                )
            )
    ) {
        // Aurora glow top-right
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // ── Logo ─────────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(logoAlpha.value)
                        .offset(y = floatOffset.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(NeonGreen.copy(alpha = 0.3f), Color.Transparent)
                                    )
                                )
                        )
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "SmartFit Logo",
                            modifier = Modifier.size(150.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SmartFit",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "AI-Powered Posture Training",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateLabel,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // ── Login card ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = CardBg,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .then(
                            Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(CardBorder, Color.Transparent, CardBorder)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        )
                        .padding(1.dp)
                        .background(CardBg, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.alpha(formAlpha.value)) {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Email field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim(); message = "" },
                            label = { Text("Email") },
                            placeholder = { Text("Enter your email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null,
                                    tint = if (email.isNotEmpty()) NeonGreen else SlateLabel)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            isError = message.contains("email", ignoreCase = true),
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = SlateBorder,
                                focusedLabelColor = NeonGreen,
                                unfocusedLabelColor = SlateLabel,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = NeonGreen,
                                errorBorderColor = ErrorRed,
                                errorLabelColor = ErrorRed
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; message = "" },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null,
                                    tint = if (password.isNotEmpty()) NeonGreen else SlateLabel)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = SlateLabel
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            isError = message.contains("password", ignoreCase = true),
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = SlateBorder,
                                focusedLabelColor = NeonGreen,
                                unfocusedLabelColor = SlateLabel,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = NeonGreen,
                                errorBorderColor = ErrorRed,
                                errorLabelColor = ErrorRed
                            )
                        )

                        if (message.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Buttons ──────────────────────────────────────────
                Column(modifier = Modifier.alpha(buttonsAlpha.value)) {
                    Button(
                        onClick = {
                            if (!ValidationUtils.isValidEmail(email)) {
                                message = "Please enter a valid email"
                                return@Button
                            }
                            isLoading = true
                            FirebaseFirestore.getInstance()
                                .collection("UserAuth")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (!documents.isEmpty) {
                                        val doc = documents.documents.first()
                                        if (doc.getString("password") == password) {
                                            val userId = doc.getString("userId") ?: ""
                                            sessionManager.saveSession(userId, email)
                                            navController.navigate(NavHomepage) {
                                                popUpTo(NavLogin) { inclusive = true }
                                            }
                                        } else {
                                            message = "Incorrect password"
                                        }
                                    } else {
                                        message = "Email not found"
                                    }
                                    isLoading = false
                                }
                                .addOnFailureListener {
                                    message = "Login failed: ${it.message}"
                                    isLoading = false
                                }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color(0xFF0A0A0F),
                            disabledContainerColor = NeonGreen.copy(alpha = 0.4f),
                            disabledContentColor = Color(0xFF0A0A0F).copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF0A0A0F),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Login", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateLabel
                        )
                        TextButton(
                            onClick = { navController.navigate(NavSignup) },
                            enabled = !isLoading
                        ) {
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreenSub
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
