package com.example.project_smartfit.presentation.screens.auth.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.R
import com.example.project_smartfit.data.local.SessionManager
import com.example.project_smartfit.domain.util.ValidationUtils
import com.example.project_smartfit.presentation.components.*
import com.example.project_smartfit.presentation.navigation.NavHome
import com.example.project_smartfit.presentation.navigation.NavLogin
import com.example.project_smartfit.presentation.navigation.NavSignup
import com.example.project_smartfit.presentation.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    navController: NavController,
    sessionManager: SessionManager
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Entrance animations
    val logoAlpha = remember { Animatable(0f) }
    val formAlpha = remember { Animatable(0f) }
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

    DarkAuroraBackground(
        modifier = Modifier.fillMaxSize()
    ) {
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

            // Logo section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .offset(y = floatOffset.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Glow effect
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        GovGreen.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    // Logo (copied from SubsidyGuard)
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
                    color = Slate300,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Login card
            GlassCard(
                variant = GlassCardVariant.Dark,
                borderGradient = true,
                animateEntrance = true,
                entranceDelay = 200,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.alpha(formAlpha.value)
                ) {
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SGTextField(
                        value = email,
                        onValueChange = { 
                            email = it.trim()
                            message = "" 
                        },
                        label = "Email",
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        isError = message.contains("email", ignoreCase = true),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SGTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            message = ""
                        },
                        label = "Password",
                        placeholder = "Enter your password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { /* Trigger login */ },
                        isError = message.contains("password", ignoreCase = true),
                        enabled = !isLoading
                    )

                    if (message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRedLight,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Column(
                modifier = Modifier.alpha(buttonsAlpha.value)
            ) {
                SGButton(
                    text = "Login",
                    onClick = {
                        if (!ValidationUtils.isValidEmail(email)) {
                            message = "Please enter a valid email"
                            return@SGButton
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
                                        navController.navigate(NavHome) {
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
                    variant = SGButtonVariant.Primary,
                    isLoading = isLoading,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate300
                    )
                    TextButton(
                        onClick = { navController.navigate(NavSignup) },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Sign Up",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = GovGreenLight
                        )
                    }
                }
            }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
