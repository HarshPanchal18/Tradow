package dev.harsh.tradow.util

import android.content.Context
import com.google.gson.Gson
import dev.harsh.tradow.model.Spot

object SharedPreferencesHelper {

    const val PREF_NAME = "Geo"
    private const val SPOT_ARRAY_KEY = "arrayOfSpots"
    private val gson = Gson()

    fun saveSpots(context: Context, spotArray: Array<Spot>) {
        val arr = loadSpots(context)
        val jsonArray = gson.toJson(arr.plus(spotArray))
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SPOT_ARRAY_KEY, jsonArray).apply()
    }

    fun updateSpots(context: Context,spotArray: Array<Spot>) {
        val jsonArray = gson.toJson(spotArray)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SPOT_ARRAY_KEY, jsonArray).apply()
    }

    fun loadSpots(context: Context): Array<Spot> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = prefs.getString(SPOT_ARRAY_KEY, null)

        if (jsonArray != null)
            return gson.fromJson(jsonArray, Array<Spot>::class.java)

        return emptyArray() // Default value if the array is not found
    }

    fun getActiveSpot(context: Context): Spot? {
        return loadSpots(context).firstOrNull { it.isSelected }
    }
}
