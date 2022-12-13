package com.jeepchief.photomemorial.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jeepchief.photomemorial.model.rest.MapService

class MainViewModel : ViewModel() {
//    private val mapService: MapService

    val location: MutableLiveData<Location> by lazy { MutableLiveData<Location>() }

}