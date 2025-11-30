package be.student.cityshare

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import be.student.cityshare.ui.cities.AddCityScreen
import be.student.cityshare.ui.cities.CitiesScreen
import be.student.cityshare.ui.places.CityDetailScreen   // 🔹 NIEUW
import be.student.cityshare.ui.home.HomeScreen
import be.student.cityshare.ui.map.OsmdroidWorldMapScreen
import be.student.cityshare.ui.places.AddPlaceScreen
import be.student.cityshare.ui.places.PlaceDetailScreen
import be.student.cityshare.ui.places.PlacesListScreen
import be.student.cityshare.ui.places.PlacesViewModel
import be.student.cityshare.ui.profile.ProfileScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.net.URLEncoder
import java.net.URLDecoder

@Composable
fun CityShareApp() {
    val navController = rememberNavController()
    val placesViewModel: PlacesViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
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
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
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

        composable("map") {
            OsmdroidWorldMapScreen(
                navController = navController,
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

        composable(
            route = "place_detail/{placeId}",
            arguments = listOf(
                navArgument("placeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId")

            if (placeId != null) {
                PlaceDetailScreen(
                    placeId = placeId,
                    onBack = { navController.popBackStack() },
                    placesViewModel = placesViewModel
                )
            }
        }

        composable("cities") {
            CitiesScreen(
                onAddCity = { navController.navigate("add_city") },
                onBack = { navController.popBackStack() },
                onCityClick = { city ->
                    val encodedName = URLEncoder.encode(city.name, "UTF-8")
                    navController.navigate("city_detail/${city.id}/$encodedName")
                }
            )
        }

        composable(
            route = "city_detail/{cityId}/{cityName}",
            arguments = listOf(
                navArgument("cityId") { type = NavType.StringType },
                navArgument("cityName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val cityId = backStackEntry.arguments?.getString("cityId") ?: ""
            val cityNameEncoded = backStackEntry.arguments?.getString("cityName") ?: ""
            val cityName = URLDecoder.decode(cityNameEncoded, "UTF-8")

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