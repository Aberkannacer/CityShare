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

            val place = SavedPlace(
                id = id,
                userId = userId,
                title = title,
                latitude = lat,
                longitude = lng,
                category = category,
                imageUrl = localPath,
                rating = rating,
                comment = comment
            )

            db.collection("places")
                .document(id)
                .set(place)
        }
    }
}