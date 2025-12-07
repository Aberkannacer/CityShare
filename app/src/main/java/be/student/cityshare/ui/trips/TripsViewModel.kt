package be.student.cityshare.ui.trips

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.model.Trip
import be.student.cityshare.utils.uriToBase64
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

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

    init {
        listenToCities()
        listenToCategories()
        listenToTrips()
        loadUsers()
    }

    private fun listenToTrips() {
        db.collection("trips")
            .addSnapshotListener { snap, _ ->
                _trips.value = snap?.documents?.mapNotNull { doc ->
                    Trip(
                        id = doc.getString("id") ?: doc.id,
                        userId = doc.getString("userId") ?: "",
                        cityId = doc.getString("cityId") ?: "",
                        cityName = doc.getString("cityName") ?: "",
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
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError("Je moet ingelogd zijn om een trip toe te voegen.")
            return
        }

        if (city == null) {
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

        val trip = Trip(
            id = id,
            userId = userId,
            cityId = city.id,
            cityName = city.name,
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

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Opslaan mislukt")
            } finally {
                _saving.value = false
            }
        }
    }
}
