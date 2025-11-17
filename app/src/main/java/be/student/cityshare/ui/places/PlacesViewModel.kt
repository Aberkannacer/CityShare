package be.student.cityshare.ui.places

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.model.SavedPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class PlacesViewModel : ViewModel() {

    private val _places = MutableStateFlow<List<SavedPlace>>(emptyList())
    val places: StateFlow<List<SavedPlace>> = _places

    fun savePlace(
        title: String,
        lat: Double,
        lng: Double,
        category: String,
        imageUri: Uri?
    ) {
        if (imageUri == null) return

        viewModelScope.launch {
            val newPlace = SavedPlace(
                id = UUID.randomUUID().toString(),
                title = title,
                latitude = lat,
                longitude = lng,
                category = category,
                imageUrl = imageUri.toString()
            )
            _places.value = _places.value + newPlace
        }
    }
}