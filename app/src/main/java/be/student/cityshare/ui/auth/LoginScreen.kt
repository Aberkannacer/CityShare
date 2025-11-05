package be.student.cityshare.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login / Register", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(16.dp))

        Row {
            Button(
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        errorMessage = "Geef een geldig email en min. 6 tekens"
                        return@Button
                    }
                    loading = true
                    Firebase.auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { onLoggedIn() }
                        .addOnFailureListener { errorMessage = it.message }
                        .addOnCompleteListener { loading = false }
                },
                enabled = !loading
            ) {
                Text("Login")
            }

            Spacer(Modifier.width(12.dp))

            OutlinedButton(
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        errorMessage = "Geef een geldig email en min. 6 tekens"
                        return@OutlinedButton
                    }
                    loading = true
                    Firebase.auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { onLoggedIn() }
                        .addOnFailureListener { errorMessage = it.message }
                        .addOnCompleteListener { loading = false }
                },
                enabled = !loading
            ) {
                Text("Register")
            }
        }
    }
}
