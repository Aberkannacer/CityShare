package be.student.cityshare.utils

import android.content.Context
import android.location.Geocoder
import java.util.Locale

fun getAddressFromLocation(
    context: Context,
    latitude: Double,
    longitude: Double
): String? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = geocoder.getFromLocation(latitude, longitude, 1)

        if (!results.isNullOrEmpty()) {
            results[0].getAddressLine(0)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}