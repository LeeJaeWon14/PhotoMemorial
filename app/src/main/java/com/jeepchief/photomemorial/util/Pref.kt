package com.jeepchief.photomemorial.util

import android.content.Context
import android.content.SharedPreferences
import com.jeepchief.photomemorial.R

class Pref(private val context : Context) {
    private val PREF_NAME = context.getString(R.string.app_name)
    private var preference : SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
//        const val PREF_DATA = "Preference Test"
        const val TUTORIAL_SHOWN = "TUTORIAL SHOWN"

        private var instance : Pref? =null
        @Synchronized
        fun getInstance(context: Context) : Pref? {
            if(instance == null)
                instance = Pref(context)

            return instance
        }
    }

    fun getString(id: String) : String? = preference.getString(id, "")

    fun getBoolean(id: String) : Boolean = preference.getBoolean(id, false)

    fun setValue(id: String?, value: String) : Boolean {
        return preference.edit()
            .putString(id, value)
            .commit()
    }

    fun setValue(id: String?, value: Boolean) : Boolean {
        return preference.edit()
            .putBoolean(id, value)
            .commit()
    }

    fun removeValue(id: String?) : Boolean {
        return preference.edit()
            .remove(id)
            .commit()
    }
}