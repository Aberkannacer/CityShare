package be.student.cityshare.ui.places

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.model.SavedPlace
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

    init {
        listenToPlaces()
    }

    private fun listenToPlaces() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("places")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                _places.value = snapshot?.toObjects() ?: emptyList()
            }
    }

    private suspend fun resolveOrCreateCity(
        context: Context,
        lat: Double,
        lng: Double
    ): Triple<String?, String?, String?> {
        return withContext(Dispatchers.IO) {

            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val geoRes = geocoder.getFromLocation(lat, lng, 1)

                if (geoRes.isNullOrEmpty()) return@withContext Triple(null, null, null)

                val cityName = geoRes[0].locality ?: return@withContext Triple(null, null, null)
                val country = geoRes[0].countryName ?: ""

                val query = db.collection("cities")
                    .whereEqualTo("name", cityName)
                    .whereEqualTo("country", country)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    val doc = query.documents.first()
                    return@withContext Triple(doc.id, cityName, country)
                }

                val newCityData = mapOf(
                    "name" to cityName,
                    "country" to country
                )

                val newDoc = db.collection("cities").add(newCityData).await()

                return@withContext Triple(newDoc.id, cityName, country)

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

            val base64Image = imageUri?.let { uriToBase64(context, it) }

            val address = withContext(Dispatchers.IO) {
                getAddressFromLocation(context, lat, lng)
            }

            val (cityId, cityName, country) = resolveOrCreateCity(context, lat, lng)

            val place = SavedPlace(
                id = id,
                userId = userId,
                title = title,
                latitude = lat,
                longitude = lng,
                category = category,
                imageBase64 = base64Image,
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
        if (placeId.isBlank()) return

        val updates = mapOf(
            "rating" to rating,
            "comment" to comment
        )

        db.collection("places")
            .document(placeId)
            .update(updates)
    }
}