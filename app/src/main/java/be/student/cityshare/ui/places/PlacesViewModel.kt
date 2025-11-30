package be.student.cityshare.ui.places

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.model.SavedPlace
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import android.location.Geocoder
import com.google.firebase.Timestamp
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

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
                if (error != null) {
                    return@addSnapshotListener
                }

                val list: List<SavedPlace> =
                    snapshot?.toObjects<SavedPlace>() ?: emptyList()

                _places.value = list
            }
    }

    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            val fileName = "place_${System.currentTimeMillis()}.jpg"
            val dir = File(context.filesDir, "places")

            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun savePlace(
        context: Context,
        title: String,
        lat: Double,
        lng: Double,
        category: String,
        imageUri: Uri?,
        rating: Int,
        comment: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val localPath = imageUri?.let { saveImageToInternalStorage(context, it) } ?: ""

            val cityRef = resolveOrCreateCityForLocation(context, lat, lng)

            val place = SavedPlace(
                id = id,
                userId = userId,
                title = title,
                latitude = lat,
                longitude = lng,
                category = category,
                imageUrl = localPath,
                rating = rating,
                comment = comment,
                cityId = cityRef?.id,
                cityName = cityRef?.name,
                country = cityRef?.country
            )

            db.collection("places")
                .document(id)
                .set(place)
        }
    }
    private suspend fun resolveOrCreateCityForLocation(
        context: Context,
        lat: Double,
        lng: Double
    ): CityRef? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocation(lat, lng, 1)
            val addr = results?.firstOrNull() ?: return@withContext null

            val cityName = addr.locality
                ?: addr.subAdminArea
                ?: addr.adminArea
                ?: return@withContext null

            val countryName = addr.countryName
            val searchName = cityName.trim().lowercase()

            val citiesQuery = db.collection("cities")
                .whereEqualTo("searchName", searchName)

            val snapshot = Tasks.await(citiesQuery.get())
            val existingDoc = snapshot.documents.firstOrNull()

            if (existingDoc != null) {
                return@withContext CityRef(
                    id = existingDoc.id,
                    name = existingDoc.getString("name") ?: cityName,
                    country = existingDoc.getString("country") ?: countryName
                )
            }

            val userId = auth.currentUser?.uid
            val newDoc = db.collection("cities").document()

            val data = hashMapOf(
                "name" to cityName,
                "searchName" to searchName,
                "country" to (countryName ?: ""),
                "description" to "",
                "createdBy" to userId,
                "createdAt" to Timestamp.now()
            )

            Tasks.await(newDoc.set(data))

            CityRef(
                id = newDoc.id,
                name = cityName,
                country = countryName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    data class CityRef(
        val id: String,
        val name: String,
        val country: String?
    )
}