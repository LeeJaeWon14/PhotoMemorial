package com.jeepchief.photomemorial.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.jeepchief.photomemorial.R
import com.jeepchief.photomemorial.databinding.ActivityMainBinding
import com.jeepchief.photomemorial.model.database.PhotoEntity
import com.jeepchief.photomemorial.model.database.PmDatabase
import com.jeepchief.photomemorial.util.Log
import com.jeepchief.photomemorial.viewmodel.MainViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private val viewModel: MainViewModel by viewModels()
    private lateinit var mapFragment: MapFragment
    private lateinit var naverMap: NaverMap
    private lateinit var infoWindow: InfoWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        checkPermission()
        locationManager = getSystemService(LocationManager::class.java)

        // init naver maps
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient("kd3ptmxe5c")

        val fm = supportFragmentManager
        mapFragment = fm.findFragmentById(R.id.fm_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.fm_map, it).commit()
            }

        // init UI
        binding.apply {
            btnAddPhoto.setOnClickListener {
                imagePickLauncher.launch("image/*")
            }
        }
    }

    private val imagePickLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { result ->
        result?.let {
            it.forEach { uri ->
                if(uri.toString().contains("com.android.providers.media")) {
                    Log.e("This uri is not picking from gallery, Maybe pick from providers..")
                    return@forEach
                }
                makeOverlay(uri)
            }
        }
    }

    private fun makeOverlay(uri: Uri) {
        getAbsolutePath(this@MainActivity, uri)?.let { path ->
            val exif = ExifInterface(path)
            exif.latLong?.let {
                val address = getAddress(it[0], it[1])
                // Save with room
                CoroutineScope(Dispatchers.IO).launch {
                    PmDatabase.getInstance(this@MainActivity).getPmDAO()
                        .insertPhoto(PhotoEntity(
                            uri, it[0], it[1], address
                        ))
                }
                initOverlay(uri, it[0], it[1], address)

            } ?: run {
                Toast.makeText(this@MainActivity, getString(R.string.msg_uri_is_null), Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this@MainActivity, getString(R.string.msg_illegal_uri), Toast.LENGTH_SHORT).show()
            Log.e("This uri illegal type.. $uri")
        }
    }

    private fun makeOverlay(entity: PhotoEntity) {
        entity.run {
            initOverlay(photo, latitude, longitude, address)
        }
    }

    private fun initOverlay(uri: Uri, lat: Double, lon: Double, address: String) {
        val marker = Marker()
        marker.apply {
            position = LatLng(lat, lon)
            isVisible = true
            map = naverMap
            onClickListener = markerListener
            iconTintColor = Color.RED
            tag = address
        }

        infoWindow = InfoWindow()

//        infoWindow = InfoWindow().apply {
//            onClickListener = Overlay.OnClickListener { overlay ->
//                val dlgView = layoutInflater.inflate(R.layout.layout_infowindow_photo, null, false)
//                val dlg = AlertDialog.Builder(this@MainActivity).create().apply {
//                    setView(dlgView)
//                    setCancelable(false)
//                }
//
//                dlgView.run {
//                    findViewById<ImageView>(R.id.iv_infowindow).run {
//                        setImageURI(uri)
//                        setOnClickListener { _->
//                            dlg.dismiss()
//                        }
//                    }
//                }
//
//                dlg.show()
//                true
//            }
//        }
    }

    private var markerListener = Overlay.OnClickListener { overlay ->
        val marker = overlay as Marker
        marker.infoWindow?.close() ?: run {
            infoWindow.apply {
                adapter = object : InfoWindow.DefaultTextAdapter(this@MainActivity) {
                    override fun getText(p0: InfoWindow): CharSequence {
                        return marker.tag.toString()
                    }
                }
                onClickListener = Overlay.OnClickListener { _->
                    viewModel.getPhotoUri(this@MainActivity, marker.tag.toString())

                    true
                }
                open(marker)
            }
        }

        true
    }


    @UiThread
    override fun onMapReady(map: NaverMap) {
        viewModel.getPhotoEntity(this@MainActivity)
        CoroutineScope(Dispatchers.Main).launch {
            // For map setting.
            this@MainActivity.naverMap = map.apply {
                cameraPosition = CameraPosition(LatLng(viewModel.location.value!!), 15.0)
                locationOverlay.apply {
                    isVisible = true
                    position = LatLng(viewModel.location.value!!)
                }
                locationTrackingMode = LocationTrackingMode.NoFollow
                setOnMapClickListener { _, _ ->
                    infoWindow.close()
                }
                uiSettings.apply {
                    isCompassEnabled = true
                    isScaleBarEnabled = true

                    isTiltGesturesEnabled = false
                    isRotateGesturesEnabled = false
                }
            }
        }
    }

    private fun initLocation() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                6000,
                300.0f,
                gpsListener
            )
            viewModel.location.value = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch(e: SecurityException) {
            /* no-op */
        }
    }

    private val gpsListener = LocationListener {
        viewModel.location.value = it
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun observeViewModel() {
        viewModel.run {
            location.observe(this@MainActivity) {
                mapFragment.getMapAsync(this@MainActivity)
            }

            photoEntity.observe(this@MainActivity) { entities ->
                if(entities.isEmpty()) return@observe
                entities.forEach { entity ->
                    makeOverlay(entity)
                }
            }

            photoUri.observe(this@MainActivity) { uri ->
                val dlgView = layoutInflater.inflate(R.layout.layout_infowindow_photo, null, false)
                val dlg = AlertDialog.Builder(this@MainActivity).create().apply {
                    setView(dlgView)
                    setCancelable(false)
                }

                dlgView.run {
                    findViewById<ImageView>(R.id.iv_infowindow).run {
//                        val mUri = MediaStore.getMediaUri(this@MainActivity, uri)
//                        val inStream = contentResolver.openInputStream(uri)
//                        val bitmap = BitmapFactory.decodeStream(inStream)
                        setImageURI(uri)
//                        setImageBitmap(rotateImage(bitmap))
                        setOnClickListener { _->
                            dlg.dismiss()
                        }
                    }
                }

                dlg.show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermission() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                initLocation()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                Toast.makeText(this@MainActivity, getString(R.string.str_permission_denied_message), Toast.LENGTH_SHORT).show()
            }
        }

        TedPermission.create().apply {
            setPermissionListener(permissionListener)
            setDeniedMessage(getString(R.string.str_permission_denied_message))
            setPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }.check()
    }

    //Uri를 절대경로(Absolute Path)로 변환
    private fun getAbsolutePath(uri : Uri) : String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val c : Cursor = contentResolver.query(uri, proj, null, null)!!
        val index = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        c.moveToFirst()

        val result = c.getString(index)

        return result
    }

    @SuppressLint("Range")
    private fun getAbsolutePath(context: Context, uri: Uri) : String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(uri, proj, null, null, null)
        cursor?.moveToNext()
        val path = cursor?.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
        val uri = Uri.fromFile(File(path))
        cursor?.close()
        return path
    }

    private fun getAddress(lat: Double, lon: Double) : String {
        val geo = Geocoder(this)
        val addressList = geo.getFromLocation(lat, lon, 10)
        val address = addressList[0].getAddressLine(0).split(" ")
        if(address[address.lastIndex] == "대한민국") {
            val builder = StringBuilder()
            for(i in address.lastIndex -1 downTo 0) {
                builder.append("${address[i]} ")
            }
            return builder.toString()
        }
        return addressList[0].getAddressLine(0).toString().split("대한민국 ")[1]
    }

    private fun rotateImage(bitmap: Bitmap, degree: Float = 90.0f) : Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}