package be.student.cityshare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import be.student.cityshare.ui.auth.LoginScreen
import be.student.cityshare.ui.auth.RegisterScreen
import be.student.cityshare.ui.cities.AddCityScreen
import be.student.cityshare.ui.cities.CitiesScreen
import be.student.cityshare.ui.cities.AddCityMapScreen
import be.student.cityshare.ui.places.CityDetailScreen
import be.student.cityshare.ui.home.HomeScreen
import be.student.cityshare.ui.map.OsmdroidWorldMapScreen
import be.student.cityshare.ui.messaging.ChatScreen
import be.student.cityshare.ui.messaging.MessagingViewModel
import be.student.cityshare.ui.messaging.UserListScreen
import be.student.cityshare.ui.places.AddPlaceScreen
import be.student.cityshare.ui.places.PlaceDetailScreen
import be.student.cityshare.ui.places.PlacesListScreen
import be.student.cityshare.ui.places.PlacesViewModel
import be.student.cityshare.ui.profile.ProfileScreen
import be.student.cityshare.ui.trips.AddTripScreen
import be.student.cityshare.ui.trips.TripsViewModel
import be.student.cityshare.ui.trips.TripsListScreen
import be.student.cityshare.ui.trips.TripDetailScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.net.URLEncoder
import java.net.URLDecoder

@Composable
fun CityShareApp() {
    val navController = rememberNavController()
    val placesViewModel: PlacesViewModel = viewModel()
    val tripsViewModel: TripsViewModel = viewModel()
    val messagingViewModel: MessagingViewModel = viewModel()

    val hasUnreadMessages by messagingViewModel.hasUnreadMessages.collectAsState()

    val startDestination = if (Firebase.auth.currentUser != null) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                onNavigateToPlaces = { navController.navigate("trips") },
                onNavigateToCities = { navController.navigate("cities") },
                onNavigateToAddTrip = { navController.navigate("add_trip") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToMessaging = { navController.navigate("user_list") },
                hasUnreadMessages = hasUnreadMessages
            )
        }

        composable("user_list") {
            UserListScreen(
                messagingViewModel = messagingViewModel,
                onUserClick = { user ->
                    val encodedEmail = URLEncoder.encode(user.email, "UTF-8")
                    navController.navigate("chat/${user.uid}/$encodedEmail")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "chat/{receiverId}/{receiverEmail}",
            arguments = listOf(
                navArgument("receiverId") { type = NavType.StringType },
                navArgument("receiverEmail") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            val receiverEmailEncoded = backStackEntry.arguments?.getString("receiverEmail") ?: ""
            val receiverEmail = URLDecoder.decode(receiverEmailEncoded, "UTF-8")
            ChatScreen(
                receiverId = receiverId,
                receiverEmail = receiverEmail,
                onBack = { navController.popBackStack() },
                messagingViewModel = messagingViewModel
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
                onAddCity = { navController.navigate("add_city_map") },
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
                onBack = { navController.popBackStack() },
                onPlaceClick = { place ->
                    navController.navigate("place_detail/${place.id}")
                }
            )
        }

        composable("add_city") {
            AddCityScreen(
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("trips") {
            TripsListScreen(
                tripsViewModel = tripsViewModel,
                onBack = { navController.popBackStack() },
                onTripClick = { tripId -> navController.navigate("trip_detail/$tripId") }
            )
        }

        composable("trip_detail/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            TripDetailScreen(
                tripId = tripId,
                tripsViewModel = tripsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("add_city_map") {
            AddCityMapScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable("add_trip") {
            AddTripScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                tripsViewModel = tripsViewModel
            )
        }
    }
}
