package com.bikash.myplaces.activities

import android.Manifest
import android.R.attr
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Address
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import com.bikash.myplaces.R
import com.bikash.myplaces.database.DatabaseHandler
import com.bikash.myplaces.databinding.ActivityAddHappyPlaceBinding
import com.bikash.myplaces.models.HappyPlaceModel
import com.bikash.myplaces.utils.GetAddressFromLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*






class AddHappyPlaceActivity : AppCompatActivity(),View.OnClickListener {
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private var mHappyPlaceDetails: HappyPlaceModel? = null
    private var binding : ActivityAddHappyPlaceBinding? = null

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbarAddPlace)
        if (supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (!Places.isInitialized()) {
            Places.initialize(
                this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key)
            )
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        dateSetListener = DatePickerDialog.OnDateSetListener {
                view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            updateDateInView()

        }
        updateDateInView()

        if (mHappyPlaceDetails != null){
            supportActionBar?.title = "EDIT HAPPY PLACE"

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude
            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)
            binding?.btnSave?.text = "UPDATE"

        }

        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
        binding?.tvSelectCurrentLocation?.setOnClickListener(this)

    }

    private fun isLocationEnabled(): Boolean{
        val locationManager : LocationManager= getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestNewLocationData() {
        var mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = LocationRequest.QUALITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1

        mFusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            mLatitude = mLastLocation!!.latitude
            Log.i("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.i("Current Longitude", "$mLongitude")

            val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity, mLatitude, mLongitude)
            addressTask.setAddressListener(object: GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String?){
                    binding?.etLocation?.setText(address)

                }
                override fun onError(){
                    Log.e("Get Address::","Something went wrong")

                }
            })
            addressTask.getAddress()
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener, // This is the variable which have created globally and initialized in setupUI method.
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR), // Here the cal instance is created globally and used everywhere in the class where it is required.
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems =
                    arrayOf("Select photo from gallery", "Capture photo from camera")
                pictureDialog.setItems(
                    pictureDialogItems
                ) { dialog, which ->
                    when (which) {
                        // Here we have create the methods for image selection from GALLERY
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }

            R.id.btn_save -> {

                when {
                    binding?.etTitle?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }
                    binding?.etDescription?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT)
                            .show()
                    }
                    binding?.etLocation?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT)
                            .show()
                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {


                        val happyPlaceModel = HappyPlaceModel(
                            // TODO(Step 2: Changing the id if it is for edit.)
                            // START
                            if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,

                            binding?.etTitle?.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding?.etDescription?.text.toString(),
                            binding?.etDate?.text.toString(),
                            binding?.etLocation?.text.toString(),
                            mLatitude,
                            mLongitude
                        )


                        val dbHandler = DatabaseHandler(this)


                        if (mHappyPlaceDetails == null) {
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)

                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        } else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)

                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }

                    }
                }
            }

            R.id.et_location -> {
                try {
                    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this@AddHappyPlaceActivity)
                    @Suppress("DEPRECATION")
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()){
                    Toast.makeText(this,"Your provider is turned off please turn it on",Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else{
                    Dexter.withActivity(this)
                        .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        .withListener(object : MultiplePermissionsListener {
                            @RequiresApi(Build.VERSION_CODES.S)
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                if (report!!.areAllPermissionsGranted()) {

                                    requestNewLocationData()
                                }
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                            ) {
                                showRationalDialogForPermissions()
                            }
                        }).onSameThread()
                        .check()
                }
            }

        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY){
                if(data != null){
                    val contentURI = data.data
                    try {
                        @Suppress("DEPRECATION")
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("Saved image: ", "Path :: $saveImageToInternalStorage")
                        binding?.ivPlaceImage!!.setImageBitmap(selectedImageBitmap)
                    }catch (e: IOException){
                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity,"Failed to load image from gallery",Toast.LENGTH_SHORT).show()
                    }
                }
            }else if (requestCode == CAMERA){
                val thumbnail : Bitmap = data!!.extras!!.get("data") as Bitmap
                saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)
                Log.e("Saved image: ", "Path :: $saveImageToInternalStorage")
                binding?.ivPlaceImage?.setImageBitmap(thumbnail)
            }else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {

                val place:Place = Autocomplete.getPlaceFromIntent(data!!)

                binding?.etLocation?.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("Cancelled", "Cancelled")
        }
    }
    private fun updateDateInView(){
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat,Locale.getDefault())
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    private fun choosePhotoFromGallery() {

        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object: MultiplePermissionsListener{
            override fun  onPermissionsChecked( report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                    startActivityForResult(galleryIntent, GALLERY)
                }
            }
            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>? ,
                token: PermissionToken?
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread()
            .check()
    }


    private fun takePhotoFromCamera(){
        @Suppress("DEPRECATION")
        Dexter.withActivity(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA).withListener(object: MultiplePermissionsListener{
            override fun  onPermissionsChecked( report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    //@Suppress("DEPRECATION")
                    startActivityForResult(intent, CAMERA)
                }
            }
            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>? , token: PermissionToken?)
            {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }


    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage("it seems like you have turned off this feature . it can be enabled under application setting")
            .setPositiveButton("Go to settings")
        {_,_ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }catch (e : ActivityNotFoundException){
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel"){dialog,
                                      _ ->
            dialog.dismiss()
        }.show()
    }

    private fun saveImageToInternalStorage(bitmap:Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()

        }catch (e: IOException){
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)
    }

    companion object{
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "MyPlacesImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }

}