package be.student.cityshare.ui.places

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.model.SavedPlace
import be.student.cityshare.model.PlaceReview
import be.student.cityshare.utils.getAddressFromLocation
import be.student.cityshare.utils.uriToBase64
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class PlacesViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _places = MutableStateFlow<List<SavedPlace>>(emptyList())
    val places: StateFlow<List<SavedPlace>> = _places

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _placeReviews = MutableStateFlow<Map<String, List<PlaceReview>>>(emptyMap())
    val placeReviews: StateFlow<Map<String, List<PlaceReview>>> = _placeReviews

    private val reviewListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    private val _userMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userMap: StateFlow<Map<String, String>> = _userMap

    init {
        listenToPlaces()
        listenToCategories()
        loadUsers()
    }

    private fun listenToPlaces() {
        db.collection("places")
            .addSnapshotListener { snap, error ->
                if (error != null) return@addSnapshotListener
                _places.value = snap?.toObjects() ?: emptyList()
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

    private fun loadUsers() {
        db.collection("users").get().addOnSuccessListener { snap ->
            val map = snap.documents.associate { doc ->
                val uid = doc.getString("uid") ?: doc.id
                val name = doc.getString("displayName")
                    ?: doc.getString("email")
                    ?: uid
                uid to name
            }
            _userMap.value = map
        }
    }

    private suspend fun resolveOrCreateCity(
        context: Context,
        lat: Double,
        lng: Double
    ): Triple<String?, String?, String?> {

        return withContext(Dispatchers.IO) {

            try {
                val geo = Geocoder(context, Locale.getDefault())
                val res = geo.getFromLocation(lat, lng, 1)

                if (res.isNullOrEmpty()) return@withContext Triple(null, null, null)

                val cityName = res[0].locality ?: return@withContext Triple(null, null, null)
                val country = res[0].countryName ?: ""

                val found = db.collection("cities")
                    .whereEqualTo("name", cityName)
                    .whereEqualTo("country", country)
                    .get()
                    .await()

                if (!found.isEmpty) {
                    val doc = found.documents.first()
                    return@withContext Triple(doc.id, cityName, country)
                }

                val newCity = mapOf(
                    "name" to cityName,
                    "country" to country
                )

                val doc = db.collection("cities").add(newCity).await()
                return@withContext Triple(doc.id, cityName, country)

            } catch (e: Exception) {
                e.printStackTrace()
                Triple(null, null, null)
            }
        }
    }

    fun savePlace(
        context: Context,
        title: String,
        lat: Double,
        lng: Double,
        category: String,
        imageUri: Uri?
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {

            val id = UUID.randomUUID().toString()

            val imageBase64 = imageUri?.let { uriToBase64(context, it) }

            val address = withContext(Dispatchers.IO) {
                getAddressFromLocation(context, lat, lng)
            }

            val (cityId, cityName, country) =
                resolveOrCreateCity(context, lat, lng)

            val place = SavedPlace(
                id = id,
                userId = userId,
                title = title,
                latitude = lat,
                longitude = lng,
                category = category,
                imageBase64 = imageBase64,
                imageUrl = "",
                rating = 0,
                comment = "",
                address = address,
                cityId = cityId,
                cityName = cityName,
                country = country
            )

            db.collection("places")
                .document(id)
                .set(place)
        }
    }

    fun updatePlaceDetails(placeId: String, rating: Int, comment: String) {
        // legacy; no-op retained for compatibility
    }

    fun listenToPlaceReviews(placeId: String) {
        if (placeId.isBlank()) return
        reviewListeners[placeId]?.remove()

        val reg = db.collection("placeReviews")
            .whereEqualTo("placeId", placeId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.toObjects<PlaceReview>() ?: emptyList()
                _placeReviews.value = _placeReviews.value.toMutableMap().apply {
                    this[placeId] = list
                }
            }
        reviewListeners[placeId] = reg
    }

    fun addPlaceReview(placeId: String, rating: Int, comment: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = _userMap.value[userId]
            ?: auth.currentUser?.displayName
            ?: auth.currentUser?.email
            ?: "Onbekende gebruiker"

        viewModelScope.launch {
            try {
                val existing = db.collection("placeReviews")
                    .whereEqualTo("placeId", placeId)
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    val docId = existing.documents.first().id
                    val updates = mapOf(
                        "rating" to rating,
                        "comment" to comment.trim(),
                        "userName" to userName,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    db.collection("placeReviews").document(docId).update(updates)
                } else {
                    val data = mapOf(
                        "placeId" to placeId,
                        "userId" to userId,
                        "userName" to userName,
                        "rating" to rating,
                        "comment" to comment.trim(),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    db.collection("placeReviews").add(data)
                }
            } catch (_: Exception) {
            }
        }
    }
}
