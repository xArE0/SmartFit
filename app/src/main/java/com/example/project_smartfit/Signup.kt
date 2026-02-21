package com.example.project_smartfit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.util.UUID

// ── Shared design tokens ───────────────────────────────────────────────
private val SBgDark      = Color(0xFF0A0A0F)
private val SCardBg      = Color(0xFF161622).copy(alpha = 0.90f)
private val SCardBorder  = Color(0xFF00E676).copy(alpha = 0.25f)
private val SNeonGreen   = Color(0xFF00E676)
private val SNeonSub     = Color(0xFF69F0AE)
private val SSlateLabel  = Color(0xFF94A3B8)
private val SSlateBorder = Color(0xFF334155)
private val SSlate700    = Color(0xFF334155)
private val SSlate900    = Color(0xFF0F172A)
private val SSlate400    = Color(0xFF94A3B8)
private val SErrorRed    = Color(0xFFFF6B6B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Signup(navController: NavController) {
    var step      by remember { mutableIntStateOf(0) }
    var message   by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Step data
    var name         by remember { mutableStateOf("") }
    var gender       by remember { mutableStateOf("") }
    var fitnessLevel by remember { mutableStateOf("") }
    var age          by remember { mutableStateOf("") }
    var weight       by remember { mutableStateOf("") }
    var height       by remember { mutableStateOf("") }
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }

    val fitnessLevels = listOf("Beginner", "Intermediate", "Advanced")
    val genders       = listOf("Male", "Female", "Other")

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Full background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SBgDark, Color(0xFF0D1B2A), SBgDark)
                )
            )
    ) {
        // Aurora glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SNeonGreen.copy(alpha = 0.08f), Color.Transparent)
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

                // ── Header ───────────────────────────────────────────
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (step == 0) "Welcome to SmartFit" else "Create Account",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (step == 0) "Transform your posture with AI" else "Join us for a healthier lifestyle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SSlateLabel,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Step indicator ───────────────────────────────────
                if (step > 0) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(600, delayMillis = 200)) + scaleIn(tween(600, delayMillis = 200))
                    ) {
                        SignupStepIndicator(currentStep = step - 1, totalSteps = 3)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // ── Card with steps ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SCardBg, RoundedCornerShape(20.dp))
                        .padding(1.dp)
                        .background(SCardBg, RoundedCornerShape(20.dp))
                        .padding(24.dp)
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
                                name = name, onNameChange = { name = it },
                                gender = gender, onGenderChange = { gender = it },
                                fitnessLevel = fitnessLevel, onFitnessLevelChange = { fitnessLevel = it },
                                genders = genders, fitnessLevels = fitnessLevels,
                                message = message
                            )
                            2 -> MetricsStep(
                                age = age, onAgeChange = { age = it.filter { c -> c.isDigit() } },
                                weight = weight, onWeightChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                                height = height, onHeightChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                                message = message
                            )
                            3 -> AccountStep(
                                email = email, onEmailChange = { email = it },
                                password = password, onPasswordChange = { password = it },
                                message = message, isLoading = isLoading
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Navigation buttons ───────────────────────────────
                if (step > 0) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(600, delayMillis = 400)) +
                                slideInVertically(tween(600, delayMillis = 400), initialOffsetY = { it / 2 })
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Back button
                            OutlinedButton(
                                onClick = { step--; message = "" },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SNeonGreen),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SNeonGreen.copy(alpha = 0.5f))
                            ) {
                                Text("Back", fontWeight = FontWeight.SemiBold)
                            }

                            // Next / Sign Up button
                            Button(
                                onClick = {
                                    when (step) {
                                        1 -> {
                                            if (name.isNotBlank() && gender.isNotBlank() && fitnessLevel.isNotBlank()) {
                                                step = 2; message = ""
                                            } else {
                                                message = "Please fill all fields"
                                            }
                                        }
                                        2 -> {
                                            if (age.isNotBlank() && weight.isNotBlank() && height.isNotBlank()) {
                                                if (!ValidationUtils.isValidAge(age)) { message = "Please enter a valid age (5-120)"; return@Button }
                                                if (!ValidationUtils.isValidWeight(weight)) { message = "Please enter a realistic weight (20-400 kg)"; return@Button }
                                                if (!ValidationUtils.isValidHeight(height)) { message = "Please enter a realistic height (50-250 cm)"; return@Button }
                                                step = 3; message = ""
                                            } else {
                                                message = "Please fill all fields"
                                            }
                                        }
                                        3 -> {
                                            if (email.isNotBlank() && password.isNotBlank()) {
                                                if (!ValidationUtils.isValidEmail(email)) { message = "Please enter a valid email"; return@Button }
                                                if (!ValidationUtils.isStrongPassword(password)) { message = "Password must be at least 8 characters, include upper and lower case, a digit, and a special character"; return@Button }
                                                isLoading = true
                                                val userId = UUID.randomUUID().toString()
                                                val user = hashMapOf("userId" to userId, "email" to email, "password" to password)
                                                val metrics = hashMapOf(
                                                    "userId" to userId, "name" to name,
                                                    "gender" to gender, "fitnessLevel" to fitnessLevel,
                                                    "age" to age, "weight" to weight, "height" to height
                                                )
                                                val db = FirebaseFirestore.getInstance()
                                                db.collection("UserAuth").document(userId).set(user)
                                                    .addOnSuccessListener {
                                                        db.collection("UserMetrics").document(userId).set(metrics)
                                                            .addOnSuccessListener { navController.navigate(NavLogin) }
                                                            .addOnFailureListener { message = "Failed to save metrics: ${it.message}"; isLoading = false }
                                                    }
                                                    .addOnFailureListener { message = "Signup failed: ${it.message}"; isLoading = false }
                                            } else {
                                                message = "Please enter email and password"
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SNeonGreen,
                                    contentColor = SSlate900,
                                    disabledContainerColor = SNeonGreen.copy(alpha = 0.4f),
                                    disabledContentColor = SSlate900.copy(alpha = 0.5f)
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = SSlate900,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (step == 3) "Sign Up" else "Next",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Login link ───────────────────────────────────────
                if (step > 0) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(if (isVisible) 1f else 0f)
                    ) {
                        Text(
                            text = "Already have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SSlateLabel
                        )
                        TextButton(
                            onClick = { navController.navigate(NavLogin) },
                            enabled = !isLoading
                        ) {
                            Text(
                                text = "Login",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SNeonSub
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ── Step indicator ────────────────────────────────────────────────────

@Composable
private fun SignupStepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive  = index <= currentStep
            val isCurrent = index == currentStep

            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.1f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "step_scale"
            )
            val backgroundColor by animateColorAsState(
                targetValue = if (isActive) SNeonGreen else SSlate700,
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
                    Icon(Icons.Default.Check, contentDescription = null,
                        tint = SSlate900, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) SSlate900 else SSlate400
                    )
                }
            }

            if (index < totalSteps - 1) {
                val isCompleted = index < currentStep
                val progress by animateFloatAsState(
                    targetValue = if (isCompleted) 1f else 0f,
                    animationSpec = tween(durationMillis = 400),
                    label = "line_progress"
                )
                Box(modifier = Modifier.width(40.dp).height(3.dp).padding(horizontal = 4.dp)) {
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(1.5.dp)).background(SSlate700))
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(1.5.dp)).background(SNeonGreen))
                }
            }
        }
    }
}

// ── Step 0: Welcome ───────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit, onLoginClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null,
            tint = SNeonGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Begin Your Journey", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "We'll customize your experience based on your fitness goals and posture needs.",
            style = MaterialTheme.typography.bodyMedium, color = SSlateLabel, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SNeonGreen, contentColor = SSlate900)
        ) {
            Text("Start Creating Account", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onLoginClick) {
            Text("Already have an account? Login", color = SNeonGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Step 1: Personal Info ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInfoStep(
    name: String, onNameChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    fitnessLevel: String, onFitnessLevelChange: (String) -> Unit,
    genders: List<String>, fitnessLevels: List<String>,
    message: String
) {
    Column {
        Text("Tell us about yourself", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))
        StepTextField(value = name, onValueChange = onNameChange, label = "Full Name",
            placeholder = "Enter your name", icon = Icons.Default.Person)
        Spacer(modifier = Modifier.height(16.dp))
        DropdownField(value = gender, label = "Gender", options = genders,
            onSelected = onGenderChange, icon = Icons.Default.People)
        Spacer(modifier = Modifier.height(16.dp))
        DropdownField(value = fitnessLevel, label = "Fitness Level", options = fitnessLevels,
            onSelected = onFitnessLevelChange, icon = Icons.Default.FitnessCenter)
        if (message.isNotEmpty() && message.contains("fields")) {
            Text(text = message, color = SErrorRed,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ── Step 2: Metrics ───────────────────────────────────────────────────

@Composable
private fun MetricsStep(
    age: String, onAgeChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    message: String
) {
    Column {
        Text("Fitness Metrics", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))
        StepTextField(value = age, onValueChange = onAgeChange, label = "Age",
            placeholder = "e.g. 25", icon = Icons.Default.CalendarToday,
            keyboardType = KeyboardType.Number)
        Spacer(modifier = Modifier.height(16.dp))
        StepTextField(value = weight, onValueChange = onWeightChange, label = "Weight (kg)",
            placeholder = "e.g. 70", icon = Icons.Default.MonitorWeight,
            keyboardType = KeyboardType.Decimal)
        Spacer(modifier = Modifier.height(16.dp))
        StepTextField(value = height, onValueChange = onHeightChange, label = "Height (cm)",
            placeholder = "e.g. 175", icon = Icons.Default.Straighten,
            keyboardType = KeyboardType.Decimal)
        if (message.isNotEmpty()) {
            Text(text = message, color = SErrorRed,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ── Step 3: Account ───────────────────────────────────────────────────

@Composable
private fun AccountStep(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    message: String, isLoading: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        Text("Account Details", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))
        StepTextField(value = email, onValueChange = onEmailChange, label = "Email",
            placeholder = "your@email.com", icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email, enabled = !isLoading)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            placeholder = { Text("Min 8 characters") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null,
                    tint = if (password.isNotEmpty()) SNeonGreen else SSlateLabel)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = SSlateLabel
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SNeonGreen, unfocusedBorderColor = SSlateBorder,
                focusedLabelColor = SNeonGreen, unfocusedLabelColor = SSlateLabel,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = SNeonGreen
            )
        )
        if (message.isNotEmpty()) {
            Text(text = message, color = SErrorRed,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ── Reusable text field for steps ────────────────────────────────────

@Composable
private fun StepTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(icon, contentDescription = null,
                tint = if (value.isNotEmpty()) SNeonGreen else SSlateLabel)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SNeonGreen, unfocusedBorderColor = SSlateBorder,
            focusedLabelColor = SNeonGreen, unfocusedLabelColor = SSlateLabel,
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            cursorColor = SNeonGreen
        )
    )
}

// ── Dropdown for gender / fitness level ──────────────────────────────

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
            leadingIcon = {
                Icon(icon, contentDescription = null,
                    tint = if (value.isNotEmpty()) SNeonGreen else SSlateLabel)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SNeonGreen, unfocusedBorderColor = SSlateBorder,
                focusedLabelColor = SNeonGreen, unfocusedLabelColor = SSlateLabel,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
