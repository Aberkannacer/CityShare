package be.student.cityshare.model

data class SavedPlace(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String = "",
    val imageUrl: String = "",
    val imageBase64: String? = null,
    val rating: Int = 0,
    val comment: String = "",
    val cityId: String? = null,
    val cityName: String? = null,
    val country: String? = null,
    val address: String? = null
)