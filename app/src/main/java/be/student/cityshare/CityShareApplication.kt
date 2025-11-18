package be.student.cityshare

import android.app.Application
import org.maplibre.android.MapLibre

class CityShareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
