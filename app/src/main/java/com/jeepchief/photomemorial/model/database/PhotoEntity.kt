package com.jeepchief.photomemorial.model.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ted.gun0912.clustering.clustering.TedClusterItem
import ted.gun0912.clustering.geometry.TedLatLng

@Entity
data class PhotoEntity(
    @PrimaryKey
    @ColumnInfo(name = "photo")
    val photo: Uri,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "take_date")
    val takeDate: String
) : TedClusterItem {
    override fun getTedLatLng(): TedLatLng {
        return TedLatLng(latitude, longitude)
    }
}