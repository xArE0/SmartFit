package com.example.project_smartfit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

@Composable
fun Homepage() {
    var message by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("message")

        // Write a message to the database
        myRef.setValue("Hello, World!")

        // Read message from the database
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                message = snapshot.getValue(String::class.java) ?: "No data"
            }

            override fun onCancelled(error: DatabaseError) {
                message = "Error: ${error.message}"
            }
        })
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message)
    }
}
