package com.bikash.myplaces.models

import android.icu.text.CaseMap
import java.io.Serializable

data class HappyPlaceModel(
    val id: Int,
    val title: String,
    val image: String,
    val description: String,
    val date: String,
    val location: String,
    val latitude: Double,
    val longitude: Double
): Serializable