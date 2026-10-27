package com.satguess.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

@Serializable
data class Location(
    val id: String,
    val lat: Double,
    val lon: Double,
    val cat: String,
    @SerialName("name_fr") val nameFr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("desc_fr") val descFr: String? = null,
    @SerialName("desc_en") val descEn: String? = null,
    val country: String? = null,
    val sitelinks: Int,
    @SerialName("view_km") val viewKm: Double,
) {
    private val french get() = Locale.getDefault().language == "fr"

    val name: String get() = if (french) nameFr else nameEn

    /** Courte phrase explicative (description Wikidata), dans la langue de l'appareil si possible. */
    val description: String? get() = (if (french) descFr ?: descEn else descEn ?: descFr)
        ?.replaceFirstChar { it.titlecase() }

    /** Nom du pays dans la langue de l'appareil, ou null (Antarctique, haute mer…). */
    val countryName: String?
        get() = country?.let { Locale("", it).displayCountry.takeIf(String::isNotBlank) }
}

@Serializable
data class LocationList(val version: Int, val locations: List<Location>)

object LocationRepository {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: LocationList? = null

    /** Charge la liste embarquée (assets/locations.json). À appeler hors du thread principal. */
    fun load(context: Context): LocationList = cached ?: synchronized(this) {
        cached ?: context.assets.open("locations.json").bufferedReader().use {
            json.decodeFromString<LocationList>(it.readText())
        }.also { cached = it }
    }
}
