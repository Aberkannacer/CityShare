package be.student.cityshare

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import be.student.cityshare.ui.home.HomeScreen
import be.student.cityshare.ui.map.WorldMapScreen
import be.student.cityshare.ui.places.AddPlaceScreen
import be.student.cityshare.ui.places.PlaceDetailScreen
import be.student.cityshare.ui.places.PlacesListScreen
import be.student.cityshare.ui.places.PlacesViewModel
import be.student.cityshare.ui.profile.ProfileScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

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
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    Firebase.auth.signOut()
                    // Keer terug naar het startpunt en wis de backstack
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
            WorldMapScreen(
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
    }
}
