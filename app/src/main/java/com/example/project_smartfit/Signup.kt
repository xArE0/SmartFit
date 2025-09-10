package com.example.project_smartfit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Signup(navController: NavController) {
    var step by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }

    // Step 1: Welcome
    // Step 2: Name/Nickname, Gender, Fitness Level
    // Step 3: Age, Weight, Height
    // Step 4: Email, Password

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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (step) {
            0 -> {
                // Welcome Step
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Text("Welcome to SmartFit!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Let's get started with your fitness journey.")
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Already have an account? Login",
                        color = Color.Blue,
                        modifier = Modifier
                            .clickable { navController.navigate(NavLogin) }
                            .padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Next")
                    }
                }
            }
            1 -> {
                // Name/Nickname, Gender, Fitness Level
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Tell us about yourself", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name or Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Gender dropdown
                    var genderExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded }
                    ) {
                        TextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            genders.forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        gender = it
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Fitness Level dropdown
                    var fitnessExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = fitnessExpanded,
                        onExpandedChange = { fitnessExpanded = !fitnessExpanded }
                    ) {
                        TextField(
                            value = fitnessLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fitness Level") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = fitnessExpanded,
                            onDismissRequest = { fitnessExpanded = false }
                        ) {
                            fitnessLevels.forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        fitnessLevel = it
                                        fitnessExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && gender.isNotBlank() && fitnessLevel.isNotBlank()) {
                                step = 2
                            } else {
                                message = "Please fill all fields"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Next")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, color = Color.Red)
                }
            }
            2 -> {
                // Age, Weight, Height
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Your Metrics", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = age,
                        onValueChange = { age = it.filter { c -> c.isDigit() } },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = height,
                        onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (age.isNotBlank() && weight.isNotBlank() && height.isNotBlank()) {
                                if (!ValidationUtils.isValidAge(age)) {
                                    message = "Please enter a valid age (5-120)"
                                    return@Button
                                }
                                if (!ValidationUtils.isValidWeight(weight)) {
                                    message = "Please enter a realistic weight (20-400 kg)"
                                    return@Button
                                }
                                if (!ValidationUtils.isValidHeight(height)) {
                                    message = "Please enter a realistic height (50-250 cm)"
                                    return@Button
                                }
                                step = 3
                            } else {
                                message = "Please fill all fields"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Next")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, color = Color.Red)
                }
            }
            3 -> {
                // Email, Password
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Create your account", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                if (!ValidationUtils.isValidEmail(email)) {
                                    message = "Please enter a valid email"
                                    return@Button
                                }
                                if (!ValidationUtils.isStrongPassword(password)) {
                                    message = "Password must be at least 8 characters, include upper and lower case, a digit, and a special character"
                                    return@Button
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
                                                message = "Signup successful!"
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Up")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, color = if (message.contains("success")) Color.Green else Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Already have an account? Login",
                        color = Color.Blue,
                        modifier = Modifier.clickable { navController.navigate(NavLogin) }
                    )
                }
            }
        }
    }
}