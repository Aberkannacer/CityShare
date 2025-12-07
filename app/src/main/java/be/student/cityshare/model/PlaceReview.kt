package be.student.cityshare.model

data class PlaceReview(
    val id: String = "",
    val placeId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = ""
)
