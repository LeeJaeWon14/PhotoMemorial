package com.jeepchief.photomemorial.viewmodel

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeepchief.photomemorial.model.database.PhotoEntity
import com.jeepchief.photomemorial.model.database.PmDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    val location: MutableLiveData<Location> by lazy { MutableLiveData<Location>() }
    var nowAddress: String = ""

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

    private val _deleteUriResult: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val deleteUriResult: LiveData<Int> get() = _deleteUriResult
    fun deleteUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _deleteUriResult.postValue(
                    PmDatabase.getInstance(context).getPmDAO().deleteRowByUri(uri)
                )
            }
        }
    }

    private val _searchPhotoList: MutableLiveData<List<PhotoEntity>> by lazy { MutableLiveData<List<PhotoEntity>>() }
    val searchPhotoList: LiveData<List<PhotoEntity>> get() = _searchPhotoList
    fun searchPhoto(context: Context, query: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _searchPhotoList.postValue(
                    PmDatabase.getInstance(context).getPmDAO().searchAddress(query)
                )
            }
        }
    }

    val photoLocationBySearch: MutableLiveData<PhotoEntity> by lazy { MutableLiveData<PhotoEntity>() }
    val updateAddressInWindow: MutableLiveData<String> by lazy { MutableLiveData<String>() }

    private val _searchAroundPhotoList: MutableLiveData<List<PhotoEntity>> by lazy { MutableLiveData<List<PhotoEntity>>() }
    val searchAroundPhotoList: LiveData<List<PhotoEntity>> get() = _searchAroundPhotoList
    fun searchAroundPhoto(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _searchAroundPhotoList.postValue(
                    PmDatabase.getInstance(context).getPmDAO().selectPhoto()
                )
            }
        }
    }
}