package com.example.project_smartfit.presentation.screens.auth.signup

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.domain.util.ValidationUtils
import com.example.project_smartfit.presentation.components.*
import com.example.project_smartfit.presentation.navigation.*
import com.example.project_smartfit.presentation.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavController) {
    var step by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Step data
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var fitnessLevel by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val fitnessLevels = listOf("Beginner", "Intermediate", "Advanced")
    val genders = listOf("Male", "Female", "Other")

    // Entrance animations
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    LightAuroraBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (step == 0) "Welcome to SmartFit" else "Create Account",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (step == 0) "Transform your posture with AI" else "Join us for a healthier lifestyle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Step Indicator
            if (step > 0) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 200)) + scaleIn(tween(600, delayMillis = 200))
                ) {
                    SignupStepIndicator(currentStep = step - 1, totalSteps = 3)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Glass Card with steps
            GlassCard(
                variant = GlassCardVariant.Light,
                borderGradient = true,
                animateEntrance = true,
                entranceDelay = 300,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> width } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "signup_steps"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> WelcomeStep(
                            onNext = { step = 1 },
                            onLoginClick = { navController.navigate(NavLogin) }
                        )
                        1 -> PersonalInfoStep(
                            name = name,
                            onNameChange = { name = it },
                            gender = gender,
                            onGenderChange = { gender = it },
                            fitnessLevel = fitnessLevel,
                            onFitnessLevelChange = { fitnessLevel = it },
                            genders = genders,
                            fitnessLevels = fitnessLevels,
                            message = message
                        )
                        2 -> MetricsStep(
                            age = age,
                            onAgeChange = { age = it.filter { c -> c.isDigit() } },
                            weight = weight,
                            onWeightChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                            height = height,
                            onHeightChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                            message = message
                        )
                        3 -> AccountStep(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            message = message,
                            isLoading = isLoading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons
            if (step > 0) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 400)) + slideInVertically(tween(600, delayMillis = 400), initialOffsetY = { it / 2 })
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SGButton(
                            text = "Back",
                            onClick = { 
                                step--
                                message = ""
                            },
                            variant = SGButtonVariant.Outline,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        )

                        SGButton(
                            text = if (step == 3) "Sign Up" else "Next",
                            onClick = {
                                when (step) {
                                    1 -> {
                                        if (name.isNotBlank() && gender.isNotBlank() && fitnessLevel.isNotBlank()) {
                                            step = 2
                                            message = ""
                                        } else {
                                            message = "Please fill all fields"
                                        }
                                    }
                                    2 -> {
                                        if (age.isNotBlank() && weight.isNotBlank() && height.isNotBlank()) {
                                            if (!ValidationUtils.isValidAge(age)) {
                                                message = "Please enter a valid age (5-120)"
                                                return@SGButton
                                            }
                                            if (!ValidationUtils.isValidWeight(weight)) {
                                                message = "Please enter a realistic weight (20-400 kg)"
                                                return@SGButton
                                            }
                                            if (!ValidationUtils.isValidHeight(height)) {
                                                message = "Please enter a realistic height (50-250 cm)"
                                                return@SGButton
                                            }
                                            step = 3
                                            message = ""
                                        } else {
                                            message = "Please fill all fields"
                                        }
                                    }
                                    3 -> {
                                        if (email.isNotBlank() && password.isNotBlank()) {
                                            if (!ValidationUtils.isValidEmail(email)) {
                                                message = "Please enter a valid email"
                                                return@SGButton
                                            }
                                            if (!ValidationUtils.isStrongPassword(password)) {
                                                message = "Password must be at least 8 characters, include upper and lower case, a digit, and a special character"
                                                return@SGButton
                                            }
                                            isLoading = true
                                            val userId = UUID.randomUUID().toString()
                                            val user = hashMapOf(
                                                "userId" to userId,
                                                "email" to email,
                                                "password" to password
                                            )
                                            val metrics = hashMapOf(
                                                "userId" to userId,
                                                "name" to name,
                                                "gender" to gender,
                                                "fitnessLevel" to fitnessLevel,
                                                "age" to age,
                                                "weight" to weight,
                                                "height" to height
                                            )
                                            val db = FirebaseFirestore.getInstance()
                                            db.collection("UserAuth")
                                                .document(userId)
                                                .set(user)
                                                .addOnSuccessListener {
                                                    db.collection("UserMetrics")
                                                        .document(userId)
                                                        .set(metrics)
                                                        .addOnSuccessListener {
                                                            navController.navigate(NavLogin)
                                                        }
                                                        .addOnFailureListener {
                                                            message = "Failed to save metrics: ${it.message}"
                                                            isLoading = false
                                                        }
                                                }
                                                .addOnFailureListener {
                                                    message = "Signup failed: ${it.message}"
                                                    isLoading = false
                                                }
                                        } else {
                                            message = "Please enter email and password"
                                        }
                                    }
                                }
                            },
                            variant = if (step == 3) SGButtonVariant.Primary else SGButtonVariant.Secondary,
                            modifier = Modifier.weight(1f),
                            isLoading = isLoading,
                            enabled = !isLoading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Link
            if (step > 0) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(if (isVisible) 1f else 0f)
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500
                    )
                    TextButton(
                        onClick = { navController.navigate(NavLogin) },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Login",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = GovGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SignupStepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            // Step circle
            val isActive = index <= currentStep
            val isCurrent = index == currentStep
            
            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.1f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "step_scale"
            )

            val backgroundColor by animateColorAsState(
                targetValue = if (isActive) GovGreen else Slate200,
                animationSpec = tween(300),
                label = "step_bg"
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                if (isActive && !isCurrent) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text(text = "${index + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isActive) Color.White else Slate500)
                }
            }

            if (index < totalSteps - 1) {
                val isCompleted = index < currentStep
                val progress by animateFloatAsState(targetValue = if (isCompleted) 1f else 0f, animationSpec = columnAnimationSpec(), label = "line_progress")
                
                Box(modifier = Modifier.width(40.dp).height(3.dp).padding(horizontal = 4.dp)) {
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(1.5.dp)).background(Slate200))
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(1.5.dp)).background(GovGreen))
                }
            }
        }
    }
}

@Composable
fun columnAnimationSpec() = tween<Float>(durationMillis = 400)

@Composable
private fun WelcomeStep(onNext: () -> Unit, onLoginClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
        Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, tint = GovGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Begin Your Journey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "We'll customize your experience based on your fitness goals and posture needs.", style = MaterialTheme.typography.bodyMedium, color = Slate600, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        SGButton(text = "Start Creating Account", onClick = onNext, variant = SGButtonVariant.Primary)
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onLoginClick) {
            Text("Already have an account? Login", color = GovGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PersonalInfoStep(
    name: String, onNameChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    fitnessLevel: String, onFitnessLevelChange: (String) -> Unit,
    genders: List<String>, fitnessLevels: List<String>,
    message: String
) {
    Column {
        Text(text = "Tell us about yourself", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
        Spacer(modifier = Modifier.height(20.dp))
        SGTextField(value = name, onValueChange = onNameChange, label = "Full Name", placeholder = "Enter your name", leadingIcon = Icons.Default.Person)
        Spacer(modifier = Modifier.height(16.dp))
        
        DropdownField(value = gender, label = "Gender", options = genders, onSelected = onGenderChange, icon = Icons.Default.People)
        Spacer(modifier = Modifier.height(16.dp))
        
        DropdownField(value = fitnessLevel, label = "Fitness Level", options = fitnessLevels, onSelected = onFitnessLevelChange, icon = Icons.Default.FitnessCenter)
        
        if (message.isNotEmpty() && message.contains("fields")) {
            Text(text = message, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun MetricsStep(
    age: String, onAgeChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    message: String
) {
    Column {
        Text(text = "Fitness Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
        Spacer(modifier = Modifier.height(20.dp))
        SGTextField(value = age, onValueChange = onAgeChange, label = "Age", placeholder = "e.g. 25", leadingIcon = Icons.Default.CalendarToday, keyboardType = KeyboardType.Number)
        Spacer(modifier = Modifier.height(16.dp))
        SGTextField(value = weight, onValueChange = onWeightChange, label = "Weight (kg)", placeholder = "e.g. 70", leadingIcon = Icons.Default.MonitorWeight, keyboardType = KeyboardType.Decimal)
        Spacer(modifier = Modifier.height(16.dp))
        SGTextField(value = height, onValueChange = onHeightChange, label = "Height (cm)", placeholder = "e.g. 175", leadingIcon = Icons.Default.Straighten, keyboardType = KeyboardType.Decimal)
        
        if (message.isNotEmpty()) {
            Text(text = message, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun AccountStep(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    message: String, isLoading: Boolean
) {
    Column {
        Text(text = "Account Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
        Spacer(modifier = Modifier.height(20.dp))
        SGTextField(value = email, onValueChange = onEmailChange, label = "Email", placeholder = "your@email.com", leadingIcon = Icons.Default.Email, keyboardType = KeyboardType.Email, enabled = !isLoading)
        Spacer(modifier = Modifier.height(16.dp))
        SGTextField(value = password, onValueChange = onPasswordChange, label = "Password", placeholder = "Min 8 characters", leadingIcon = Icons.Default.Lock, isPassword = true, enabled = !isLoading)
        
        if (message.isNotEmpty()) {
            Text(text = message, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    value: String,
    label: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = if (value.isNotEmpty()) GovGreen else Slate400) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GovGreen,
                unfocusedBorderColor = Slate300,
                focusedLabelColor = GovGreen,
                unfocusedLabelColor = Slate500,
                focusedLeadingIconColor = GovGreen,
                unfocusedLeadingIconColor = Slate400
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
