package be.student.cityshare.ui.cities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.data.local.CityDatabase
import be.student.cityshare.data.local.CityEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val cityDao = CityDatabase.get(application).cityDao()
    private var listener: ListenerRegistration? = null

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            cityDao.getAllFlow().collectLatest { local ->
                _cities.value = local.map { it.toCity() }
            }
        }
        listenToRemote()
    }

    private fun listenToRemote() {
        listener?.remove()
        listener = db.collection("cities")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                val items = snap?.documents?.map { doc ->
                    CityEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        country = doc.getString("country") ?: "",
                        description = doc.getString("description") ?: ""
                    )
                } ?: emptyList()
                viewModelScope.launch {
                    cityDao.insertAll(items)
                }
                _error.value = null
            }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val snap = db.collection("cities").get().await()
                val items = snap.documents.map { doc ->
                    CityEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        country = doc.getString("country") ?: "",
                        description = doc.getString("description") ?: ""
                    )
                }
                cityDao.insertAll(items)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}

private fun CityEntity.toCity() = City(
    id = id,
    name = name,
    country = country,
    description = description
)
