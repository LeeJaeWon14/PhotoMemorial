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
    var photo: Uri,

    @ColumnInfo(name = "latitude")
    var latitude: Double,

    @ColumnInfo(name = "longitude")
    var longitude: Double,

    @ColumnInfo(name = "address")
    var address: String
) : TedClusterItem {
    override fun getTedLatLng(): TedLatLng {
        return TedLatLng(latitude, longitude)
    }
}