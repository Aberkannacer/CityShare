package be.student.cityshare.ui.trips

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.model.Trip
import be.student.cityshare.model.TripReview
import be.student.cityshare.utils.uriToBase64
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class CityOption(
    val id: String,
    val name: String,
    val country: String
)

class TripsViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _cities = MutableStateFlow<List<CityOption>>(emptyList())
    val cities: StateFlow<List<CityOption>> = _cities

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    private val _userMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userMap: StateFlow<Map<String, String>> = _userMap

    private val _reviews = MutableStateFlow<Map<String, List<TripReview>>>(emptyMap())
    val reviews: StateFlow<Map<String, List<TripReview>>> = _reviews

    private var tripsListener: ListenerRegistration? = null
    private val reviewListeners = mutableMapOf<String, ListenerRegistration>()

    init {
        listenToCities()
        listenToCategories()
        listenToTrips()
        auth.addAuthStateListener { listenToTrips() }
        loadUsers()
    }

    private fun listenToTrips() {
        tripsListener?.remove()

        tripsListener = db.collection("trips")
            .addSnapshotListener { snap, _ ->
                _trips.value = snap?.documents?.mapNotNull { doc ->
                    Trip(
                        id = doc.getString("id") ?: doc.id,
                        userId = doc.getString("userId") ?: "",
                        cityId = doc.getString("cityId") ?: "",
                        cityName = doc.getString("cityName") ?: "",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        category = doc.getString("category") ?: "",
                        address = doc.getString("address") ?: "",
                        notes = doc.getString("notes") ?: "",
                        imageBase64 = doc.getString("imageBase64"),
                        rating = (doc.getLong("rating") ?: 0L).toInt(),
                        comment = doc.getString("comment") ?: ""
                    )
                } ?: emptyList()
            }
    }

    fun listenToReviews(tripId: String) {
        if (tripId.isBlank()) return
        reviewListeners[tripId]?.remove()

        val reg = db.collection("tripReviews")
            .whereEqualTo("tripId", tripId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    TripReview(
                        id = doc.id,
                        tripId = doc.getString("tripId") ?: tripId,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        rating = (doc.getLong("rating") ?: 0L).toInt(),
                        comment = doc.getString("comment") ?: ""
                    )
                } ?: emptyList()

                _reviews.value = _reviews.value.toMutableMap().apply {
                    this[tripId] = list
                }
            }

        reviewListeners[tripId] = reg
    }

    fun addReview(
        tripId: String,
        rating: Int,
        comment: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        val displayName = auth.currentUser?.displayName
            ?: auth.currentUser?.email
            ?: "Onbekende gebruiker"

        val data = mapOf(
            "tripId" to tripId,
            "userId" to userId,
            "userName" to displayName,
            "rating" to rating,
            "comment" to comment.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("tripReviews")
            .whereEqualTo("tripId", tripId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) {
                    val docId = existing.documents.first().id
                    db.collection("tripReviews").document(docId).set(data)
                } else {
                    db.collection("tripReviews").add(data)
                }
            }
    }

    fun updateTripReview(tripId: String, rating: Int, comment: String) {
        if (tripId.isBlank()) return

        val updates = mapOf(
            "rating" to rating,
            "comment" to comment.trim()
        )

        db.collection("trips")
            .document(tripId)
            .update(updates)
    }

    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val map = result.documents.associate { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val name = doc.getString("displayName") ?: doc.getString("email") ?: "Onbekende gebruiker"
                    uid to name
                }
                _userMap.value = map
            }
    }

    private fun listenToCities() {
        db.collection("cities")
            .orderBy("name")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { doc ->
                    CityOption(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        country = doc.getString("country") ?: ""
                    )
                }?.filter { it.name.isNotBlank() } ?: emptyList()

                _cities.value = list
            }
    }

    private fun listenToCategories() {
        db.collection("categories")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents
                    ?.mapNotNull { it.getString("name") }
                    ?: emptyList()

                _categories.value = list.sorted()
            }
    }

    fun saveTrip(
        context: Context,
        city: CityOption?,
        category: String?,
        address: String,
        notes: String,
        imageUri: Uri?,
        rating: Int,
        comment: String,
        cityNameOverride: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError("Je moet ingelogd zijn om een trip toe te voegen.")
            return
        }

        if (address.isBlank()) {
            onError("Adres is verplicht.")
            return
        }

        if (city == null && cityNameOverride.isNullOrBlank()) {
            onError("Kies eerst een stad.")
            return
        }

        if (category.isNullOrBlank()) {
            onError("Kies eerst een categorie.")
            return
        }

        viewModelScope.launch {
            _saving.value = true
            try {
                val id = UUID.randomUUID().toString()
                val imageBase64 = imageUri?.let { uriToBase64(context, it) }

                val (geoLat, geoLng, geoCity, geoCountry) = geocodeAddress(context, address, city?.name ?: cityNameOverride)

                val cityNameCandidate = city?.name ?: cityNameOverride ?: geoCity ?: ""
                val finalCity = city
                    ?: if (cityNameCandidate.isNotBlank()) {
                        createCityIfMissing(cityNameCandidate, userId, geoCountry ?: "")
                    } else null

                val finalLat = latitude ?: geoLat
                val finalLng = longitude ?: geoLng

                val trip = Trip(
                    id = id,
                    userId = userId,
                    cityId = finalCity?.id ?: "",
                    cityName = finalCity?.name ?: geoCity ?: cityNameOverride ?: "",
                    latitude = finalLat ?: 0.0,
                    longitude = finalLng ?: 0.0,
                    category = category,
                    address = address.trim(),
                    notes = notes.trim(),
                    imageBase64 = imageBase64,
                    rating = rating,
            comment = comment.trim()
        )

        val data = mapOf(
            "id" to trip.id,
            "userId" to trip.userId,
            "cityId" to trip.cityId,
            "cityName" to trip.cityName,
            "category" to trip.category,
            "address" to trip.address,
            "notes" to trip.notes,
            "imageBase64" to trip.imageBase64,
            "rating" to trip.rating,
            "comment" to trip.comment,
            "createdAt" to FieldValue.serverTimestamp()
        )

                db.collection("trips")
                    .document(id)
                    .set(data)
                    .await()

                if (rating > 0 || comment.isNotBlank()) {
                    addReview(id, rating, comment)
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Opslaan mislukt")
            } finally {
                _saving.value = false
            }
        }
    }

    private suspend fun createCityIfMissing(name: String, userId: String, country: String = ""): CityOption? {
        return try {
            val existing = db.collection("cities")
                .whereEqualTo("name", name)
                .whereEqualTo("country", country)
                .whereEqualTo("createdBy", userId)
                .get()
                .await()

            if (!existing.isEmpty) {
                val doc = existing.documents.first()
                CityOption(doc.id, name, doc.getString("country") ?: country)
            } else {
                val data = mapOf(
                    "name" to name,
                    "searchName" to name.lowercase(),
                    "country" to country,
                    "description" to "",
                    "createdBy" to userId
                )
                val doc = db.collection("cities").add(data).await()
                CityOption(doc.id, name, country)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun ensureCityExists(name: String, country: String): CityOption? {
        val userId = auth.currentUser?.uid ?: return null
        return createCityIfMissing(name, userId, country)
    }

    private suspend fun geocodeAddress(context: Context, addr: String, fallbackCity: String? = null): Quadruple<Double?, Double?, String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val geo = android.location.Geocoder(context, Locale.getDefault())
                val res = geo.getFromLocationName(addr, 1)
                val loc = res?.firstOrNull()
                if (loc != null) {
                    Quadruple(loc.latitude, loc.longitude, loc.locality ?: loc.subAdminArea, loc.countryName)
                } else if (!fallbackCity.isNullOrBlank()) {
                    val resCity = geo.getFromLocationName(fallbackCity, 1)
                    val locCity = resCity?.firstOrNull()
                    Quadruple(locCity?.latitude, locCity?.longitude, locCity?.locality ?: locCity?.subAdminArea, locCity?.countryName)
                } else {
                    Quadruple(null, null, null, null)
                }
            } catch (_: Exception) {
                Quadruple(null, null, null, null)
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
