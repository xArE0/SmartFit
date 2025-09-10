package com.example.project_smartfit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

// Exercise-themed color scheme
private val PrimaryGreen = Color(0xFF2E8B57)
private val SecondaryOrange = Color(0xFFFF6B35)
private val AccentBlue = Color(0xFF1E90FF)
private val DarkGray = Color(0xFF2C2C2C)
private val LightGray = Color(0xFFF5F5F5)
private val TextDark = Color(0xFF1A1A1A)
private val ErrorRed = Color(0xFFE74C3C)
private val SuccessGreen = Color(0xFF27AE60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Signup(navController: NavController) {
    var step by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }

    // Step 2 fields
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var fitnessLevel by remember { mutableStateOf("") }

    // Step 3 fields
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    // Step 4 fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val fitnessLevels = listOf("Beginner", "Intermediate", "Advanced")
    val genders = listOf("Male", "Female", "Other")

    // Pick a background image for each step
    val backgroundRes = when (step) {
        0 -> R.drawable.bg
        1 -> R.drawable.bg
        2 -> R.drawable.bg
        3 -> R.drawable.bg
        else -> R.drawable.bg
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Background image (lowest layer)
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
        )

        // 2. Gradient overlay for better readability (middle layer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
                .zIndex(1f)
        )

        // 3. Main content (highest layer)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .zIndex(2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Progress indicator always at the top
            if (step > 0) {
                ProgressIndicator(currentStep = step, totalSteps = 3)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Center the island in the remaining space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
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
                        message = message,
                        onNext = {
                            if (name.isNotBlank() && gender.isNotBlank() && fitnessLevel.isNotBlank()) {
                                step = 2
                                message = ""
                            } else {
                                message = "Please fill all fields"
                            }
                        }
                    )
                    2 -> MetricsStep(
                        age = age,
                        onAgeChange = { age = it.filter { c -> c.isDigit() } },
                        weight = weight,
                        onWeightChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        height = height,
                        onHeightChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                        message = message,
                        onNext = {
                            if (age.isNotBlank() && weight.isNotBlank() && height.isNotBlank()) {
                                if (!ValidationUtils.isValidAge(age)) {
                                    message = "Please enter a valid age (5-120)"
                                    return@MetricsStep
                                }
                                if (!ValidationUtils.isValidWeight(weight)) {
                                    message = "Please enter a realistic weight (20-400 kg)"
                                    return@MetricsStep
                                }
                                if (!ValidationUtils.isValidHeight(height)) {
                                    message = "Please enter a realistic height (50-250 cm)"
                                    return@MetricsStep
                                }
                                step = 3
                                message = ""
                            } else {
                                message = "Please fill all fields"
                            }
                        }
                    )
                    3 -> AccountStep(
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        message = message,
                        onSignUp = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                if (!ValidationUtils.isValidEmail(email)) {
                                    message = "Please enter a valid email"
                                    return@AccountStep
                                }
                                if (!ValidationUtils.isStrongPassword(password)) {
                                    message = "Password must be at least 8 characters, include upper and lower case, a digit, and a special character"
                                    return@AccountStep
                                }
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
                                                message = "Account created successfully!"
                                                navController.navigate(NavLogin)
                                            }
                                            .addOnFailureListener {
                                                message = "Failed to save metrics: ${it.message}"
                                            }
                                    }
                                    .addOnFailureListener {
                                        message = "Signup failed: ${it.message}"
                                    }
                            } else {
                                message = "Please enter email and password"
                            }
                        },
                        onLoginClick = { navController.navigate(NavLogin) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressIndicator(currentStep: Int, totalSteps: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < currentStep) PrimaryGreen else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
    onLoginClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(min = 380.dp, max = 500.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Welcome to SmartFit!",
                style = MaterialTheme.typography.headlineMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Transform your fitness journey with personalized workouts and tracking",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6C757D),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Already have an account? Sign In",
                color = PrimaryGreen,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onLoginClick() }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    fitnessLevel: String,
    onFitnessLevelChange: (String) -> Unit,
    genders: List<String>,
    fitnessLevels: List<String>,
    message: String,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(min = 420.dp, max = 600.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tell us about yourself",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name or Nickname") },
                leadingIcon = {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryGreen)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    focusedLabelColor = PrimaryGreen,
                    focusedLeadingIconColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gender dropdown
            var genderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    leadingIcon = {
                        Icon(Icons.Default.People, contentDescription = null, tint = PrimaryGreen)
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        focusedLabelColor = PrimaryGreen,
                        focusedLeadingIconColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    genders.forEach { genderOption ->
                        DropdownMenuItem(
                            text = { Text(genderOption) },
                            onClick = {
                                onGenderChange(genderOption)
                                genderExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fitness Level dropdown
            var fitnessExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = fitnessExpanded,
                onExpandedChange = { fitnessExpanded = !fitnessExpanded }
            ) {
                OutlinedTextField(
                    value = fitnessLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fitness Level") },
                    leadingIcon = {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PrimaryGreen)
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fitnessExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        focusedLabelColor = PrimaryGreen,
                        focusedLeadingIconColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = fitnessExpanded,
                    onDismissRequest = { fitnessExpanded = false }
                ) {
                    fitnessLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                onFitnessLevelChange(level)
                                fitnessExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricsStep(
    age: String,
    onAgeChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    message: String,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(min = 420.dp, max = 600.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = SecondaryOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your Fitness Metrics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("Age") },
                leadingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SecondaryOrange)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryOrange,
                    focusedLabelColor = SecondaryOrange,
                    focusedLeadingIconColor = SecondaryOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Weight (kg)") },
                leadingIcon = {
                    Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = SecondaryOrange)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryOrange,
                    focusedLabelColor = SecondaryOrange,
                    focusedLeadingIconColor = SecondaryOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = height,
                onValueChange = onHeightChange,
                label = { Text("Height (cm)") },
                leadingIcon = {
                    Icon(Icons.Default.Straighten, contentDescription = null, tint = SecondaryOrange)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryOrange,
                    focusedLabelColor = SecondaryOrange,
                    focusedLeadingIconColor = SecondaryOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryOrange
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountStep(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    message: String,
    onSignUp: () -> Unit,
    onLoginClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(min = 420.dp, max = 600.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Create your account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = AccentBlue)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    focusedLabelColor = AccentBlue,
                    focusedLeadingIconColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentBlue)
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    focusedLabelColor = AccentBlue,
                    focusedLeadingIconColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = if (message.contains("success")) SuccessGreen else ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = onSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Already have an account? Sign In",
                color = AccentBlue,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLoginClick() }
                    .padding(vertical = 8.dp)
            )
        }
    }
}