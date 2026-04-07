package es.uc3m.android.chillmates.shoppinglist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlacePredictionUi(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

data class PlaceDetailUi(
    val name: String,
    val address: String,
    val latLng: LatLng
)

class PlacesSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val placesClient: PlacesClient = Places.createClient(application)
    private val sessionToken = AutocompleteSessionToken.newInstance()

    private val _predictions = MutableStateFlow<List<PlacePredictionUi>>(emptyList())
    val predictions: StateFlow<List<PlacePredictionUi>> = _predictions.asStateFlow()

    private val _selectedPlace = MutableStateFlow<PlaceDetailUi?>(null)
    val selectedPlace: StateFlow<PlaceDetailUi?> = _selectedPlace.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun searchPlaces(query: String) {
        if (query.isBlank()) {
            _predictions.value = emptyList()
            return
        }

        _isLoading.value = true
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                _predictions.value = response.autocompletePredictions.map {
                    PlacePredictionUi(
                        placeId = it.placeId,
                        primaryText = it.getPrimaryText(null).toString(),
                        secondaryText = it.getSecondaryText(null).toString()
                    )
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    fun selectPlace(prediction: PlacePredictionUi) {
        _isLoading.value = true
        val fields = listOf(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION)
        val request = FetchPlaceRequest.builder(prediction.placeId, fields)
            .setSessionToken(sessionToken)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                _selectedPlace.value = PlaceDetailUi(
                    name = place.displayName ?: "",
                    address = place.formattedAddress ?: "",
                    latLng = place.location ?: LatLng(0.0, 0.0)
                )
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    fun clearSelection() {
        _selectedPlace.value = null
    }
}
