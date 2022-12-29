package com.jeepchief.photomemorial.model.database

import android.net.Uri
import androidx.room.*

@Dao
interface PmDAO {
    @Query("SELECT * FROM PhotoEntity")
    fun selectPhoto() : List<PhotoEntity>

    @Insert
    fun insertPhoto(entity: PhotoEntity)

    @Update
    fun updatePhoto(entity: PhotoEntity)

    @Delete
    fun deletePhoto(entity: PhotoEntity)

    @Query("SELECT photo FROM PhotoEntity WHERE address = :address")
    fun getPhotoUri(address: String) : Uri

    @Query("DELETE FROM PhotoEntity WHERE photo = :uri")
    fun deleteRowByUri(uri: Uri) : Int

    @Query("SELECT * FROM PhotoEntity WHERE address LIKE '%'||:query||'%'")
    fun searchAddress(query: String) : List<PhotoEntity>
}