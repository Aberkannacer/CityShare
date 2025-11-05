package be.student.cityshare.ui.cities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddCityScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Stad toevoegen", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Naam") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = country,
            onValueChange = { country = it },
            label = { Text("Land") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Beschrijving (optioneel)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                enabled = !loading,
                onClick = onCancel
            ) { Text("Annuleren") }

            Button(
                enabled = !loading,
                onClick = {
                    if (name.isBlank()) { error = "Naam is verplicht"; return@Button }
                    loading = true; error = null
                    val userId = Firebase.auth.currentUser?.uid
                    val data = hashMapOf(
                        "name" to name.trim(),
                        "country" to country.trim(),
                        "description" to description.trim(),
                        "createdBy" to userId,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    FirebaseFirestore.getInstance().collection("cities")
                        .add(data)
                        .addOnSuccessListener { onSaved() }
                        .addOnFailureListener { error = it.message ?: "Opslaan mislukt" }
                        .addOnCompleteListener { loading = false }
                }
            ) { Text("Opslaan") }
        }
    }
}
