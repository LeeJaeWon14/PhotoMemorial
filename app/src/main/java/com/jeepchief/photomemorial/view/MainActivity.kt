package com.jeepchief.photomemorial.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.exifinterface.media.ExifInterface
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.jeepchief.photomemorial.R
import com.jeepchief.photomemorial.databinding.ActivityMainBinding
import com.jeepchief.photomemorial.util.GeoDegree
import com.jeepchief.photomemorial.viewmodel.MainViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import java.io.File

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private val viewModel: MainViewModel by viewModels()
    private lateinit var mapFragment: MapFragment
    private lateinit var naverMap: NaverMap

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

        binding.apply {
            btnAddPhoto.setOnClickListener {
                imagePickLauncher.launch("image/*")
            }
        }
    }

    private val imagePickLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { result ->
        result?.let {
            it.forEach { uri ->

                getAbsolutePath(this@MainActivity, uri)?.let { path ->
                    val exif = ExifInterface(path)
//                    val degree = GeoDegree(exif)
                    val marker = Marker()
                    marker.apply {
                        position = LatLng(exif.latLong?.get(0)!!, exif.latLong?.get(1)!!)
                        isVisible = true
                        map = naverMap
                    }
                }
            }

        }
    }

    @UiThread
    override fun onMapReady(map: NaverMap) {
        // For map setting.
        this.naverMap = map.apply {
            cameraPosition = CameraPosition(LatLng(viewModel.location.value!!), 15.0)
            locationOverlay.apply {
                isVisible = true
                position = LatLng(viewModel.location.value!!)
            }
            locationTrackingMode = LocationTrackingMode.NoFollow
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

    private fun observeViewModel() {
        viewModel.run {
            location.observe(this@MainActivity) {
                mapFragment.getMapAsync(this@MainActivity)
            }
        }
    }

    private suspend fun setMarkers() {

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermission() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
//                Toast.makeText(this@MainActivity, "Granted", Toast.LENGTH_SHORT).show()
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
                Manifest.permission.ACCESS_MEDIA_LOCATION
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
}