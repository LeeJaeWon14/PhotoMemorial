package com.jeepchief.photomemorial.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.jeepchief.photomemorial.R
import com.jeepchief.photomemorial.databinding.*
import com.jeepchief.photomemorial.model.database.PhotoEntity
import com.jeepchief.photomemorial.model.database.PmDatabase
import com.jeepchief.photomemorial.util.Log
import com.jeepchief.photomemorial.util.Pref
import com.jeepchief.photomemorial.view.adapter.SearchListAdapter
import com.jeepchief.photomemorial.view.adapter.ShowAroundAdapter
import com.jeepchief.photomemorial.viewmodel.MainViewModel
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import kotlinx.coroutines.*
import ted.gun0912.clustering.naver.TedNaverClustering
import java.io.File
import java.io.IOException
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private val viewModel: MainViewModel by viewModels()
    private lateinit var mapFragment: MapFragment
    private lateinit var naverMap: NaverMap
    private var infoWindow: InfoWindow? = null
    private val markerList = mutableListOf<MutableMap<String, Marker>>()
    private val markerMap = mutableMapOf<String, Marker>()
    private lateinit var photoListDlgView: LayoutPhotoListBinding
    private lateinit var photoListDialog: AlertDialog
    private var isSearching = false
    private var isInfoWindowShowing = false
    private val savedPhotoCount: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    // 임시 flag
    private var usePhotoPicker = false

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("onCreate()")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        locationManager = getSystemService(LocationManager::class.java)

        observeViewModel()
        checkPref()
        checkPermission()
        if(!Environment.isExternalStorageManager()) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                addCategory("android.intent.category.DEFAULT")
                data = Uri.parse("package:${applicationContext.packageName}")
            })
        }

        // init naver maps
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient("kd3ptmxe5c")

        val fm = supportFragmentManager
        mapFragment = fm.findFragmentById(R.id.fm_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.fm_map, it).commit()
            }

        initUi()
        savedPhotoCount.observe(this) { count ->
            supportActionBar?.title = String.format(
                getString(R.string.toolbar_title),
                count
            )
        }
    }

    private fun initUi() {
        // init UI
        binding.apply {
            tbSearchBar.apply {
                setTitleTextColor(Color.WHITE)
                setSupportActionBar(this)
            }
            supportActionBar?.setDisplayShowTitleEnabled(true)

            btnAddPhoto.setOnClickListener {
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//                    photoPickLauncher.launch(PickVisualMediaRequest())
//                else
                    imagePickLauncher.launch("image/*")
            }
            btnAddPhoto.setOnLongClickListener {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    photoPickLauncher.launch(PickVisualMediaRequest())
                true
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

    // Android 13 이상 PhotoPicker 사용
    private val photoPickLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { result ->
        result?.let {
            usePhotoPicker = true
            it.forEach { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) // 액세스 권한 유지
                makeOverlay_13(uri)
            }
        }
    }

    private fun makeOverlay_13(uri: Uri) {
        Log.e("makeOverlay()")
        Log.e("uri >> $uri")

        if(usePhotoPicker) {
            contentResolver.openInputStream(uri).use { inStream ->
                inStream?.let { stream ->
                    Log.e("inputStream not null ~")
//                    val targetFile = File(filesDir, "tempPhoto")
//                    val fos = FileOutputStream(targetFile)
//                    val buffer = ByteArray(4 * 1024)
//                    targetFile.inputStream().use {
//                        while(it.read(buffer) != -1) {
//                            fos.write(buffer, 0, it.read(buffer, 0, 1024))
//                        }
//                    }
//
//                    return targetFile.absolutePath
                    val exif = ExifInterface(stream)
                    exif.latLong?.let {
                        val address: String
                        try {
                            address = getAddress(it[0], it[1])
                        } catch(e: IllegalStateException) {
                            return
                        }
                        // Save into room
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                PmDatabase.getInstance(this@MainActivity).getPmDAO()
                                    .insertPhoto(PhotoEntity(
                                        uri, it[0], it[1], address, convertTakeDateTime(exif.getAttribute(ExifInterface.TAG_DATETIME)!!)
                                    ))
                                withContext(Dispatchers.Main) {
                                    initOverlay(uri, it[0], it[1], address)
                                    naverMap.cameraPosition = CameraPosition(LatLng(it[0], it[1]), 15.0)
                                    markerList.forEach { map ->
                                        map[uri.toString()]?.performClick()
                                    }
                                    savedPhotoCount.value = savedPhotoCount.value?.plus(1)
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
                    Log.e("inputStream not null.. not found file with uri")
//                    return null
                }
            }
        }
    }
    private fun makeOverlay(uri: Uri) {
        Log.e("makeOverlay()")
        Log.e("uri >> $uri")
        getAbsolutePath(this@MainActivity, uri)?.let { path ->
            Log.e("path of uri.. >> $path")
            val exif = ExifInterface(path)
            exif.latLong?.let {
                val address: String
                try {
                    address = getAddress(it[0], it[1])
                } catch(e: IllegalStateException) {
                    return
                }
                // Save into room
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        PmDatabase.getInstance(this@MainActivity).getPmDAO()
                            .insertPhoto(PhotoEntity(
                                uri, it[0], it[1], address, convertTakeDateTime(exif.getAttribute(ExifInterface.TAG_DATETIME)!!)
                            ))
                        withContext(Dispatchers.Main) {
                            initOverlay(uri, it[0], it[1], address)
                            naverMap.cameraPosition = CameraPosition(LatLng(it[0], it[1]), 15.0)
                            markerList.forEach { map ->
                                map[uri.toString()]?.performClick()
                            }
                            savedPhotoCount.value = savedPhotoCount.value?.plus(1)
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
            viewModel.updateAddressInWindow.value = marker.tag.toString()
        }

        true
    }


    @UiThread
    override fun onMapReady(map: NaverMap) {
        Log.e("onMapReady()")
        // For map setting.
        this@MainActivity.naverMap = map.apply {
            locationTrackingMode = LocationTrackingMode.NoFollow
            setOnMapClickListener { _, _ ->
                infoWindow?.close()
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
//                updateMarkerCluster(entities)
                savedPhotoCount.value = entities.size // Saved photo count is updated.
                entities.forEach { entity ->
                    makeOverlay(entity)
                }
            }

            deleteUriResult.observe(this@MainActivity) { rowCnt ->
                when(rowCnt) {
                    1 -> {
//                        mapFragment.getMapAsync(this@MainActivity)
                        savedPhotoCount.value = savedPhotoCount.value?.minus(1)
                    }
                    else -> {
                        Log.e("Uri delete fail..")
                    }
                }
            }

            searchPhotoList.observe(this@MainActivity) { list ->
                when(list.size) {
                    1 -> {
                        list[0].run {
                            // check exist with photo
                            val isExist = try {
                                this@MainActivity.contentResolver.openInputStream(photo)?.use {
                                }
                                true
                            }
                            catch (e: IOException) {
                                false
                            }

                            if(!isExist) {
                                Log.e("Not found photo")
                                Toast.makeText(this@MainActivity, getString(R.string.msg_deleted_photo), Toast.LENGTH_SHORT).show()
                                removeImage(photo)
                                return@observe
                            }

                            val dlgView = LayoutInfowindowPhotoBinding.inflate(layoutInflater)
                            val dlg = AlertDialog.Builder(this@MainActivity).create().apply {
                                setView(dlgView.root)
                                setCancelable(false)
                                window?.setBackgroundDrawableResource(R.drawable.dialog_border)
                            }

                            dlgView.apply {
                                ivInfowindow.run {
                                    setImageURI(photo)
                                    setOnScaleChangeListener { scaleFactor, focusX, focusY ->
                                        if(scaleFactor <= 1.0f) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                delay(100)
                                                llButtonLayout.isVisible = true
                                                llPhotoInfo.isVisible = true
                                            }
                                        }
                                        else {
                                            llButtonLayout.isVisible = false
                                            llPhotoInfo.isVisible = false
//                                            setViewVisible(llButtonLayout, llPhotoInfo)
                                        }
                                    }
                                    setOnClickListener {
                                        llButtonLayout.isVisible = true
                                        llPhotoInfo.isVisible = true
//                                        setViewVisible(llButtonLayout, llPhotoInfo)
                                    }
                                }
                                btnCloseDialog.setOnClickListener {
                                    dlg.dismiss()
                                }
                                btnRemoveImage.setOnClickListener {
                                    removeImage(photo)
                                    dlg.dismiss()
                                }
                                ivShareButton.setOnClickListener {
                                    val popup = PopupMenu(this@MainActivity, it)
                                    menuInflater.inflate(R.menu.menu_share, popup.menu)
                                    popup.apply {
                                        setOnMenuItemClickListener {
                                            when(it.itemId) {
                                                R.id.menu_to_kakao -> {
                                                    if(ShareClient.instance.isKakaoTalkSharingAvailable(this@MainActivity)) {
                                                        ShareClient.instance.shareDefault(
                                                            this@MainActivity,
                                                            TextTemplate(
                                                                text = address,
                                                                link = Link()
                                                            )
                                                        ) { result, error ->
                                                            error?.let {
                                                                Toast.makeText(
                                                                    this@MainActivity,
                                                                    it.toString(),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                            result?.let {
                                                                startActivity(it.intent)
                                                            }
                                                        }
                                                    }
                                                    else {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "카카오톡 미설치..",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                            .show()
                                                    }
                                                }
                                                R.id.menu_to_other -> {
                                                    startActivity(Intent(
                                                        Intent.ACTION_SEND
                                                    ).apply {
                                                        type = "image/*"
                                                        putExtra(Intent.EXTRA_STREAM, photo)
                                                    })
                                                }
                                            }
                                            return@setOnMenuItemClickListener true
                                        }
                                        show()
                                    }
                                }
                                tvTakeDate.text = takeDate
                                tvTakeLocation.text = address
                            }
                            dlg.show()
                        }
                    }
                    else -> photoListDlgView.rvSearchResultInList.adapter =
                        SearchListAdapter(
                            list, { photoListDialog.dismiss() }, searchAction
                        )
                }
            }

            photoLocationBySearch.observe(this@MainActivity) { entity ->
                entity.run {
                    naverMap.cameraPosition = CameraPosition(LatLng(latitude, longitude), 15.0)
                    updateAddressInWindow.value = entity.address
                }
            }

            updateAddressInWindow.observe(this@MainActivity) { address ->
                infoWindow?.apply {
                    adapter = object : InfoWindow.DefaultTextAdapter(this@MainActivity) {
                        override fun getText(p0: InfoWindow): CharSequence {
                            return address
                        }
                    }
                    onClickListener = Overlay.OnClickListener { window ->
                        isInfoWindowShowing = true
//                        viewModel.getPhotoUri(this@MainActivity, address)
                        viewModel.searchPhoto(this@MainActivity, address)
                        (window as InfoWindow).close()
                        true
                    }
//                    open(marker)

                    markerList.forEach { map ->
                        map.keys.forEach {
                            if(map[it]?.tag == address)
                                open(map[it]!!)
                        }
                    }
                }
            }

            searchAroundPhotoList.observe(this@MainActivity) { list ->
                Log.e("around photo list count is ${list.size}")
                BottomSheetDialog(this@MainActivity).also { sheet ->
                    val dismiss = { sheet.dismiss() }
                    val sheetBinding = LayoutShowAroundBinding.inflate(layoutInflater).apply {
                        tvAroundAddress.text = viewModel.nowAddress
                        rvShowAround.apply {
                            layoutManager = LinearLayoutManager(this@MainActivity)
                            // todo: Will adding room query and this if statement.
                            if(list.none { it.address.contains(viewModel.nowAddress) }) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.msg_cannot_found_from_around),
                                    Toast.LENGTH_SHORT
                                ).show()
                                progressDlgDismiss()
                                return@observe
                            }
                            else {
                                adapter = ShowAroundAdapter(
                                    list.filter { it.address.contains(viewModel.nowAddress) },
                                    searchAction,
                                    dismiss
                                )
                            }
                        }
                    }
                    sheet.setContentView(sheetBinding.root)
                }.show()
                progressDlgDismiss()
            }
        }
    }

    private fun progressDlgDismiss() = CoroutineScope(Dispatchers.Main).launch {
        delay(500)
        DialogHelper.progressDialog(this@MainActivity).dismiss()
    }

    private fun checkPermission() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                Log.e("onPermissionGranted()")
                mapFragment.getMapAsync(this@MainActivity)
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                Log.e("onPermissionDenied()")
                Log.e("denied permissions >> $deniedPermissions")
                Toast.makeText(this@MainActivity, getString(R.string.str_permission_denied_message), Toast.LENGTH_SHORT).show()
            }
        }

        TedPermission.create().apply {
            setPermissionListener(permissionListener)
            setDeniedMessage(getString(R.string.str_permission_denied_message))

            val permissionArray = arrayListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            val minSdk = applicationContext.applicationInfo.minSdkVersion
            val targetSdk = applicationContext.applicationInfo.targetSdkVersion

            // SDK 버전에 따른 권한 추가
            for(i in minSdk.absoluteValue .. targetSdk.absoluteValue) {
                when(i) {
                    Build.VERSION_CODES.Q -> permissionArray.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                    Build.VERSION_CODES.TIRAMISU -> {
                        permissionArray.apply {
                            add(Manifest.permission.READ_MEDIA_IMAGES)
                            add(Manifest.permission.READ_MEDIA_VIDEO)
                            remove(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                }
            }

            setPermissions(*permissionArray.toTypedArray())
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
        val addressList = geo.getFromLocation(lat, lon, 10)!!
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
        binding.apply {
            btnAddPhoto.run {
                isVisible = !isVisible
            }
        }
        if(supportActionBar?.isShowing == true) supportActionBar?.hide() else supportActionBar?.show()
    }

    private fun removeImage(uri: Uri) {
        markerList.forEach { map ->
            map[uri.toString()]?.map = null
        }
        viewModel.deleteUri(this@MainActivity, uri)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_now_location -> {
                naverMap.cameraPosition = CameraPosition(
                    LatLng(viewModel.location.value!!), 15.0
                )
            }
            R.id.menu_photo_list -> {
                photoListDlgView = LayoutPhotoListBinding.inflate(layoutInflater)
                photoListDialog = AlertDialog.Builder(this@MainActivity).create().apply {
                    setCancelable(false)
                    setView(photoListDlgView.root)
                    window?.setBackgroundDrawableResource(R.drawable.dialog_border)
                }

                photoListDlgView.apply {
                    svSearchPhoto.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            newText?.let {
                                viewModel.searchPhoto(this@MainActivity, it)
                            }
                            return false
                        }
                    })

                    photoListDlgView.rvSearchResultInList.apply {
                        val dismiss = { photoListDialog.dismiss() }
                        val manager = LinearLayoutManager(this@MainActivity)
                        layoutManager = manager
                        adapter = SearchListAdapter(viewModel.photoEntity.value!!, dismiss, searchAction)
                        addItemDecoration(DividerItemDecoration(
                            this@MainActivity, manager.orientation
                        ))
                    }

                    btnExitPhotoList.setOnClickListener { photoListDialog.dismiss() }
                }

                photoListDialog.show()
            }
            R.id.menu_show_around -> {
                DialogHelper.progressDialog(this).show()
                val latlng = naverMap.cameraPosition.target
                try {
                    viewModel.nowAddress = run {
                        val baseAddress = getAddress(latlng.latitude, latlng.longitude).split(" ")
                        "${baseAddress[0]} ${baseAddress[1]} ${baseAddress[2]}" // ex) 경기도 의정부시
                    }.also { Log.e("now Address >> $it") }
                    viewModel.searchAroundPhoto(this)
                } catch(e: Exception) {
                    Log.e(e.toString())
                    Toast.makeText(this@MainActivity, getString(R.string.msg_cannot_get_address), Toast.LENGTH_SHORT).show()
                    progressDlgDismiss()
                }
            }
        }
        return false
    }

    private fun updateMarkerCluster(list: List<PhotoEntity>) {
        TedNaverClustering.with<PhotoEntity>(this, naverMap)
            .items(list)
//            .markerClickListener {
//                viewModel.getPhotoUri(this@MainActivity, )
//            }
            .make()
    }

    private val searchAction = { entity: PhotoEntity ->
        viewModel.photoLocationBySearch.value = entity
    }

    private fun checkPref() {
        if (Pref.getInstance(this)?.getBoolean(Pref.TUTORIAL_SHOWN)
                .also { Log.e("tutorial shown is >> $it") } == false
        ) {
            showTutorial()
        }
    }

    private fun showTutorial() {
        val dlgView = LayoutTutorialDialogBinding.inflate(layoutInflater)
        DialogHelper.customDialog(
            this,
            ColorDrawable(Color.TRANSPARENT)
        ) { dlg ->
            dlgView.apply {
                btnTutorialClose.setOnClickListener {
                    if (chkTutorialRemove.isChecked)
                        Pref.getInstance(this@MainActivity)
                            ?.setValue(Pref.TUTORIAL_SHOWN, chkTutorialRemove.isChecked)
                    dlg.dismiss()
                }
            }
        }.show()
    }

    private fun convertTakeDateTime(takeDate: String) : String {
        val strArr = takeDate.split(" ")
        return strArr[0].replace(":", "/").plus(" ${strArr[1]}")
            .also { Log.e("return String >> $it") }
    }

    private fun setViewVisible(vararg views: View) = views.forEach { view -> view.isVisible = !view.isVisible }
}