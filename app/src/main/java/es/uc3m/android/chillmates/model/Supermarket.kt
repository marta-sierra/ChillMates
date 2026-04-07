package es.uc3m.android.chillmates.model

import com.google.firebase.firestore.Exclude
import com.google.android.gms.maps.model.LatLng

data class Supermarket(
    @get:Exclude
    var id: String? = null,
    val name: String = "",         // Agregado valor por defecto
    val latitude: Double = 0.0,    // Agregado valor por defecto
    val longitude: Double = 0.0,   // Agregado valor por defecto
    val address: String = ""
) {
    // Helper para Google Maps
    fun toLatLng(): LatLng = LatLng(latitude, longitude)

    // Opcional: útil si usas este objeto directamente en un Selector/Dropdown
    override fun toString(): String = name
}