package be.student.cityshare.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import be.student.cityshare.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onGoToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Account aanmaken", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(displayName, { displayName = it }, label = { Text("Naam") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password (min 6)") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth())

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !loading,
            onClick = {
                if (email.isBlank() || password.length < 6 || displayName.isBlank()) {
                    error = "Alle velden zijn verplicht"; return@Button
                }
                loading = true; error = null
                Firebase.auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val firebaseUser = authResult.user
                        if (firebaseUser != null) {
                            val user = User(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                displayName = displayName
                            )
                            Firebase.firestore.collection("users").document(firebaseUser.uid)
                                .set(user)
                                .addOnSuccessListener { onRegistered() }
                                .addOnFailureListener { e -> error = e.message }
                        } else {
                            onRegistered()
                        }
                    }
                    .addOnFailureListener { error = it.message }
                    .addOnCompleteListener { loading = false }
            }
        ) { Text("Register") }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onGoToLogin) { Text("Al een account? Inloggen") }
    }
}