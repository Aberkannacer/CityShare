package be.student.cityshare.model

data class Trip(
    val id: String = "",
    val userId: String = "",
    val cityId: String = "",
    val cityName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String = "",
    val address: String = "",
    val notes: String = "",
    val imageBase64: String? = null,
    val rating: Int = 0,
    val comment: String = ""
)
