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
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

@Composable
fun Login(
    navController: NavController,
    sessionManager: SessionManager
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        FirebaseFirestore.getInstance()
                            .collection("UserAuth")
                            .whereEqualTo("email", email)
                            .whereEqualTo("password", password)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val doc = documents.documents.first()
                                    val userId = doc.getString("userId") ?: ""
                                    // Save session
                                    sessionManager.saveSession(userId, email)
                                    message = "Login successful!"
                                    navController.navigate(NavHomepage)
                                } else {
                                    message = "Invalid email or password"
                                }
                            }
                            .addOnFailureListener {
                                message = "Login failed: ${it.message}"
                            }
                    } else {
                        message = "Please enter email and password"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Don't have an account? Sign up",
                color = Color.Blue,
                modifier = Modifier.clickable { navController.navigate(NavSignup) }
            )
        }
    }
}