package be.student.cityshare.model

data class SavedPlace(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String = "",
    val imageUrl: String = ""
)