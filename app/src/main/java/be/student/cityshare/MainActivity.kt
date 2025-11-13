package be.student.cityshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import be.student.cityshare.ui.auth.LoginScreen
import be.student.cityshare.ui.auth.RegisterScreen
import be.student.cityshare.ui.home.HomeScreen
import be.student.cityshare.ui.cities.AddCityScreen
import be.student.cityshare.ui.cities.CitiesScreen
import be.student.cityshare.ui.theme.CityShareTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CityShareTheme {
                val nav = rememberNavController()
                val startDest = if (Firebase.auth.currentUser != null) "home" else "login"

                NavHost(navController = nav, startDestination = startDest) {

                    composable("login") {
                        LoginScreen(
                            onLoggedIn = {
                                nav.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToRegister = {
                                nav.navigate("register")
                            }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegistered = {
                                nav.navigate("home") {
                                    // verwijder volledige auth-stack
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToLogin = {
                                nav.popBackStack()
                            }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            onLogout = {
                                nav.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onOpenCities = { nav.navigate("cities") }
                        )
                    }

                    composable("cities") {
                        CitiesScreen(
                            onAddCity = { nav.navigate("cities/add") },
                            onBack = { nav.popBackStack() }
                        )
                    }

                    composable("cities/add") {
                        AddCityScreen(
                            onSaved = { nav.popBackStack() },
                            onCancel = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
