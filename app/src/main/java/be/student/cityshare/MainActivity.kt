package be.student.cityshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import be.student.cityshare.ui.auth.LoginScreen
import be.student.cityshare.ui.auth.RegisterScreen
import be.student.cityshare.ui.home.HomeScreen
import be.student.cityshare.ui.map.WorldMapScreen
import be.student.cityshare.ui.places.AddPlaceScreen
import be.student.cityshare.ui.places.PlaceDetailScreen
import be.student.cityshare.ui.places.PlacesListScreen
import be.student.cityshare.ui.places.PlacesViewModel
import be.student.cityshare.ui.profile.ProfileScreen
import be.student.cityshare.ui.theme.CityShareTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private val placesViewModel: PlacesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CityShareTheme {
                val navController = rememberNavController()
                val startDest = if (Firebase.auth.currentUser != null) "home" else "login"

                NavHost(navController = navController, startDestination = startDest) {

                    composable("login") {
                        LoginScreen(
                            onLoggedIn = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToRegister = { navController.navigate("register") }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegistered = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToLogin = { navController.popBackStack() }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            onNavigateToMap = { navController.navigate("map") },
                            onNavigateToPlaces = { navController.navigate("places") },
                            onNavigateToProfile = { navController.navigate("profile") }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            onBack = { navController.popBackStack() },
                            onLogout = {
                                Firebase.auth.signOut()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("map") {
                        WorldMapScreen(navController, placesViewModel)
                    }

                    composable("places") {
                        PlacesListScreen(
                            placesViewModel = placesViewModel,
                            onBack = { navController.popBackStack() },
                            onPlaceClick = { place ->
                                navController.navigate("place_detail/${place.id}")
                            }
                        )
                    }

                    composable("add_place/{lat}/{lng}") { backStackEntry ->
                        val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
                        val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
                        AddPlaceScreen(
                            lat = lat,
                            lng = lng,
                            onSaved = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() },
                            placesViewModel = placesViewModel
                        )
                    }

                    composable("place_detail/{placeId}") { backStackEntry ->
                        val placeId = backStackEntry.arguments?.getString("placeId")
                        if (placeId != null) {
                            PlaceDetailScreen(
                                placeId = placeId,
                                placesViewModel = placesViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
