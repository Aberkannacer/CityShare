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
import be.student.cityshare.ui.places.PlacesViewModel
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
                onLogout = {
                    Firebase.auth.signOut()
                },
                onOpenCities = {
                },
                onOpenMap = {
                    navController.navigate("map")
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
            val lat = backStackEntry.arguments
                ?.getString("lat")
                ?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments
                ?.getString("lng")
                ?.toDoubleOrNull() ?: 0.0

            AddPlaceScreen(
                lat = lat,
                lng = lng,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                placesViewModel = placesViewModel
            )
        }
    }
}