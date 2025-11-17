package be.student.cityshare.model

data class SavedPlace(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val imageUrl: String
)