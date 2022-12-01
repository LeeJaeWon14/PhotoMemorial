package com.jeepchief.photomemorial.model.database

import androidx.room.*

@Dao
interface PmDAO {
    @Query("SELECT * FROM PhotoEntity")
    fun selectPhoto() : PhotoEntity

    @Insert
    fun insertPhoto(entity: PhotoEntity)

    @Update
    fun updatePhoto(entity: PhotoEntity)

    @Delete
    fun deletePhoto(entity: PhotoEntity)
}