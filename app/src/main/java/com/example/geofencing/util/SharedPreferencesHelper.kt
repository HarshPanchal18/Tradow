package com.example.geofencing.util

import android.content.Context
import com.example.geofencing.model.Spot
import com.google.gson.Gson

object SharedPreferencesHelper {

    const val PREF_NAME = "Geo"
    private const val SPOT_ARRAY_KEY = "arrayOfSpots"
    private val gson = Gson()

    fun saveArray(context: Context, spotArray: Array<Spot>) {
        val arr = loadArray(context)
        val jsonArray = gson.toJson(arr.plus(spotArray))
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SPOT_ARRAY_KEY, jsonArray).apply()
    }

    fun updateArray(context: Context,spotArray: Array<Spot>) {
        val jsonArray = gson.toJson(spotArray)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SPOT_ARRAY_KEY, jsonArray).apply()
    }

    fun loadArray(context: Context): Array<Spot> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = prefs.getString(SPOT_ARRAY_KEY, null)

        if (jsonArray != null)
            return gson.fromJson(jsonArray, Array<Spot>::class.java)

        return emptyArray() // Default value if the array is not found
    }

    fun getActiveSpot(context: Context): Spot {
        return loadArray(context).first { it.isSelected }
    }
}
