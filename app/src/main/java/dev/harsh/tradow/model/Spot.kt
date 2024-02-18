package dev.harsh.tradow.model

data class Spot(
    val title: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var isSelected: Boolean = false
)
