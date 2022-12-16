package com.jeepchief.photomemorial.viewmodel

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeepchief.photomemorial.model.database.PhotoEntity
import com.jeepchief.photomemorial.model.database.PmDatabase
import com.jeepchief.photomemorial.model.rest.MapService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher

class MainViewModel : ViewModel() {
//    private val mapService: MapService

    val location: MutableLiveData<Location> by lazy { MutableLiveData<Location>() }

    private val _photoEntity: MutableLiveData<List<PhotoEntity>> by lazy { MutableLiveData<List<PhotoEntity>>() }
    val photoEntity: LiveData<List<PhotoEntity>> get() = _photoEntity
    fun getPhotoEntity(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _photoEntity.postValue(
                    PmDatabase.getInstance(context).getPmDAO().selectPhoto()
                )
            }
        }
    }
}