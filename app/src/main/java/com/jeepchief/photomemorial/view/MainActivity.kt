package com.jeepchief.photomemorial.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.jeepchief.photomemorial.R
import com.jeepchief.photomemorial.databinding.ActivityMainBinding
import com.jeepchief.photomemorial.databinding.LayoutInfowindowPhotoBinding
import com.jeepchief.photomemorial.model.database.PhotoEntity
import com.jeepchief.photomemorial.model.database.PmDatabase
import com.jeepchief.photomemorial.util.Log
import com.jeepchief.photomemorial.viewmodel.MainViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private val viewModel: MainViewModel by viewModels()
    private lateinit var mapFragment: MapFragment
    private lateinit var naverMap: NaverMap
    private lateinit var infoWindow: InfoWindow
    private val markerList = mutableListOf<MutableMap<String, Marker>>()
    private val markerMap = mutableMapOf<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(LocationManager::class.java)

        observeViewModel()
        checkPermission()

        // init naver maps
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient("kd3ptmxe5c")

        val fm = supportFragmentManager
        mapFragment = fm.findFragmentById(R.id.fm_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.fm_map, it).commit()
            }

        // init UI
        binding.apply {
            rvSearchResult.apply {
                isVisible = false
                val manager = LinearLayoutManager(this@MainActivity)
                layoutManager = manager
                adapter
                addItemDecoration(DividerItemDecoration(
                    this@MainActivity, manager.orientation
                ))
            }
            setSupportActionBar(tbSearchBar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            btnAddPhoto.setOnClickListener {
                imagePickLauncher.launch("image/*")
            }

            svSearch.run {
                setOnClickListener {
                    //todo: replace fragment
                    Toast.makeText(this@MainActivity, "Replace fragment here", Toast.LENGTH_SHORT).show()
                    rvSearchResult.isVisible = true
//                    tvTest.isVisible = true
                }
                setOnCloseListener {
                    //todo: restore prev fragment
                    Toast.makeText(this@MainActivity, "Restore fragment here", Toast.LENGTH_SHORT).show()
                    rvSearchResult.isVisible = false
//                    tvTest.isVisible = false
                    false
                }
            }
        }
    }

    private val imagePickLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { result ->
        result?.let {
            it.forEach { uri ->
                if(!uri.toString().contains(getString(R.string.standard_uri))) {
                    Log.e(getString(R.string.msg_image_not_from_the_gallery))
                    Toast.makeText(this@MainActivity, getString(R.string.msg_only_allowed_from_gallery), Toast.LENGTH_SHORT).show()
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
                val address: String
                try {
                    address = getAddress(it[0], it[1])
                } catch(e: IllegalStateException) {
                    return
                }
                // Save with room
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        PmDatabase.getInstance(this@MainActivity).getPmDAO()
                            .insertPhoto(PhotoEntity(
                                uri, it[0], it[1], address
                            ))
                        withContext(Dispatchers.Main) {
                            initOverlay(uri, it[0], it[1], address)
                            naverMap.cameraPosition = CameraPosition(LatLng(it[0], it[1]), 15.0)
                            markerList.forEach { map ->
                                map[uri.toString()]?.performClick()
                            }
                        }
                    } catch(e: SQLiteConstraintException) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, getString(R.string.msg_not_allowed_duplication), Toast.LENGTH_SHORT).show()
                        }
                    }
                }

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
        markerMap.put(uri.toString(), marker)
        markerList.add(markerMap)

        infoWindow = InfoWindow()
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
                onClickListener = Overlay.OnClickListener { window ->
                    viewModel.getPhotoUri(this@MainActivity, marker.tag.toString())
                    (window as InfoWindow).close()
                    true
                }
                open(marker)
            }
        }

        true
    }


    @UiThread
    override fun onMapReady(map: NaverMap) {
        // For map setting.
        this@MainActivity.naverMap = map.apply {
            locationTrackingMode = LocationTrackingMode.NoFollow
            setOnMapClickListener { _, _ ->
                infoWindow.close()
                hideUi()
            }
            uiSettings.apply {
                isCompassEnabled = true
                isScaleBarEnabled = true
                isTiltGesturesEnabled = false
                isRotateGesturesEnabled = false
            }
        }

        initLocation()
        viewModel.getPhotoEntity(this@MainActivity)
    }

    private fun initLocation() {
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this@MainActivity, getString(R.string.msg_off_gps), Toast.LENGTH_SHORT).show()
            return
        }
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

    private fun observeViewModel() {
        viewModel.run {
            location.observe(this@MainActivity) {
                naverMap.apply {
                    cameraPosition = CameraPosition(LatLng(it), 15.0)
                    locationOverlay.apply {
                        isVisible = true
                        position = LatLng(it)
                    }
                }
            }

            photoEntity.observe(this@MainActivity) { entities ->
                if(entities.isEmpty()) return@observe
                entities.forEach { entity ->
                    makeOverlay(entity)
                }
            }

            photoUri.observe(this@MainActivity) { uri ->
                // check exist with photo
                val isExist = try {
                    this@MainActivity.contentResolver.openInputStream(uri)?.use {

                    }
                    true
                }
                catch (e: IOException) {
                    false
                }

                if(!isExist) {
                    Log.e("Not found photo")
                    Toast.makeText(this@MainActivity, getString(R.string.msg_deleted_photo), Toast.LENGTH_SHORT).show()
                    removeImage(uri)
                    return@observe
                }

                val dlgView = LayoutInfowindowPhotoBinding.inflate(layoutInflater)
                val dlg = AlertDialog.Builder(this@MainActivity).create().apply {
                    setView(dlgView.root)
                    setCancelable(false)
                }

                dlgView.apply {
                    ivInfowindow.setImageURI(uri)
                    btnCloseDialog.setOnClickListener {
                        dlg.dismiss()
                    }
                    btnRemoveImage.setOnClickListener {
                        removeImage(uri)
                        dlg.dismiss()
                    }
                    ivShareButton.setOnClickListener {
//                        val shareIntent = Intent(Intent.ACTION_SEND)
                        startActivity(Intent(
                            Intent.ACTION_SEND
                        ).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                        })
                    }
                }
                dlg.show()
            }

            deleteUriResult.observe(this@MainActivity) { rowCnt ->
                when(rowCnt) {
                    1 -> {
//                        mapFragment.getMapAsync(this@MainActivity)
                    }
                    else -> {
                        Log.e("Uri delete fail..")
                    }
                }
            }
        }
    }

    private fun checkPermission() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                mapFragment.getMapAsync(this@MainActivity)
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
        if(!address.contains("대한민국")) {
            Toast.makeText(this@MainActivity, getString(R.string.msg_not_available_foreign_country), Toast.LENGTH_SHORT).show()
            throw IllegalStateException()
        }
        if(address[address.lastIndex] == "대한민국") {
            val builder = StringBuilder()
            for(i in address.lastIndex -1 downTo 0) {
                builder.append("${address[i]} ")
            }
            return builder.toString()
        }
        return addressList[0].getAddressLine(0).toString().split("대한민국 ")[1]
    }

    private fun hideUi() {
        binding.btnAddPhoto.run {
            isVisible = !isVisible
        }
        if(supportActionBar?.isShowing == true) supportActionBar?.hide() else supportActionBar?.show()
    }

    //todo: Will adding share feature

    private fun removeImage(uri: Uri) {
        markerList.forEach { map ->
            map[uri.toString()]?.map = null
        }
        viewModel.deleteUri(this@MainActivity, uri)
    }
}