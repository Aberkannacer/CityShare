package be.student.cityshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.student.cityshare.ui.auth.LoginScreen
import be.student.cityshare.ui.theme.CityShareTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CityShareTheme {
                val user = Firebase.auth.currentUser

                if (user != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Ingelogd als: ${user.email ?: "Onbekend"}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { Firebase.auth.signOut() }) {
                            Text("Sign out")
                        }
                    }
                } else {
                    LoginScreen(onLoggedIn = {
                    })
                }
            }
        }
    }
}