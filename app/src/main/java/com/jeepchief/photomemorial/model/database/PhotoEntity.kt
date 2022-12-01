package com.jeepchief.photomemorial.model.database

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class PhotoEntity(
    @ColumnInfo(name = "photo")
    var photo: Bitmap,

    @ColumnInfo(name = "latitude")
    var latitude: Double,

    @ColumnInfo(name = "longitude")
    var longitude: Double,

    @ColumnInfo(name = "address")
    var address: String
)