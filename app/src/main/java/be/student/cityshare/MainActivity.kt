package be.student.cityshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.preference.PreferenceManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import be.student.cityshare.ui.auth.LoginScreen
import be.student.cityshare.ui.auth.RegisterScreen
import be.student.cityshare.ui.cities.AddCityScreen
import be.student.cityshare.ui.cities.CitiesScreen
import be.student.cityshare.ui.places.CityDetailScreen
import be.student.cityshare.ui.home.HomeScreen
import be.student.cityshare.ui.map.OsmdroidWorldMapScreen
import be.student.cityshare.ui.places.AddPlaceScreen
import be.student.cityshare.ui.places.PlaceDetailScreen
import be.student.cityshare.ui.places.PlacesListScreen
import be.student.cityshare.ui.places.PlacesViewModel
import be.student.cityshare.ui.profile.ProfileScreen
import be.student.cityshare.ui.theme.CityShareTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private val placesViewModel: PlacesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid config
        val ctx = applicationContext
        Configuration.getInstance().load(
            ctx,
            PreferenceManager.getDefaultSharedPreferences(ctx)
        )
        Configuration.getInstance().userAgentValue = ctx.packageName

        setContent {
            CityShareTheme {
                val navController = rememberNavController()
                val startDest = if (Firebase.auth.currentUser != null) "home" else "login"

                NavHost(
                    navController = navController,
                    startDestination = startDest
                ) {
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
                            onNavigateToCities = { navController.navigate("cities") },
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
                        OsmdroidWorldMapScreen(
                            navController = navController,
                            placesViewModel = placesViewModel
                        )
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

                    composable("cities") {
                        CitiesScreen(
                            onAddCity = { navController.navigate("add_city") },
                            onBack = { navController.popBackStack() },
                            onCityClick = { city ->
                                val encodedName = java.net.URLEncoder.encode(city.name, "UTF-8")
                                navController.navigate("city_detail/${city.id}/$encodedName")
                            }
                        )
                    }

                    composable("city_detail/{cityId}/{cityName}") { backStackEntry ->
                        val cityId = backStackEntry.arguments?.getString("cityId") ?: ""
                        val cityNameEncoded = backStackEntry.arguments?.getString("cityName") ?: ""
                        val cityName = java.net.URLDecoder.decode(cityNameEncoded, "UTF-8")

                        CityDetailScreen(
                            cityId = cityId,
                            cityName = cityName,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("add_city") {
                        AddCityScreen(
                            onSaved = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}