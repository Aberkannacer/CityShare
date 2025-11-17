package be.student.cityshare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import be.student.cityshare.ui.auth.LoginScreen
import be.student.cityshare.ui.auth.RegisterScreen
import be.student.cityshare.ui.cities.AddCityScreen
import be.student.cityshare.ui.cities.CitiesScreen
import be.student.cityshare.ui.home.HomeScreen
import be.student.cityshare.ui.map.WorldMapScreen
import be.student.cityshare.ui.places.AddPlaceScreen
import be.student.cityshare.ui.places.PlaceDetailScreen
import be.student.cityshare.ui.places.PlacesViewModel
import be.student.cityshare.ui.theme.CityShareTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }

        setContent {
            CityShareTheme {
                val nav = rememberNavController()
                val startDest = if (Firebase.auth.currentUser != null) "home" else "login"

                val placesViewModel: PlacesViewModel = viewModel()

                NavHost(
                    navController = nav,
                    startDestination = startDest
                ) {

                    composable("login") {
                        LoginScreen(
                            onLoggedIn = {
                                nav.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToRegister = { nav.navigate("register") }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegistered = {
                                nav.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToLogin = { nav.popBackStack() }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            onLogout = {
                                nav.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onOpenCities = { nav.navigate("cities") },
                            onOpenMap = { nav.navigate("map") }
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

                    composable("map") {
                        WorldMapScreen(
                            navController = nav,
                            placesViewModel = placesViewModel
                        )
                    }

                    composable(
                        route = "add_place/{lat}/{lng}",
                        arguments = listOf(
                            navArgument("lat") { type = NavType.StringType },
                            navArgument("lng") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val lat = backStackEntry.arguments
                            ?.getString("lat")
                            ?.toDoubleOrNull() ?: 0.0
                        val lng = backStackEntry.arguments
                            ?.getString("lng")
                            ?.toDoubleOrNull() ?: 0.0

                        AddPlaceScreen(
                            lat = lat,
                            lng = lng,
                            onSaved = { nav.popBackStack() },
                            onCancel = { nav.popBackStack() },
                            placesViewModel = placesViewModel
                        )
                    }

                    composable(
                        route = "place_detail/{placeId}",
                        arguments = listOf(
                            navArgument("placeId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val placeId = backStackEntry.arguments?.getString("placeId") ?: ""

                        PlaceDetailScreen(
                            placeId = placeId,
                            onBack = { nav.popBackStack() },
                            placesViewModel = placesViewModel
                        )
                    }
                }
            }
        }
    }
}