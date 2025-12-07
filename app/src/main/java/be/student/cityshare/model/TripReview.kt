package be.student.cityshare.model

data class TripReview(
    val id: String = "",
    val tripId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = ""
)
