package com.bikash.myplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bikash.myplaces.R
import com.bikash.myplaces.databinding.ActivityAddHappyPlaceBinding
import com.bikash.myplaces.databinding.ActivityHappyPlaceDetailBinding
import com.bikash.myplaces.databinding.ActivityMainBinding
import com.bikash.myplaces.models.HappyPlaceModel

class HappyPlaceDetailActivity : AppCompatActivity() {
    private var binding: ActivityHappyPlaceDetailBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        var happyPlaceDetailModel : HappyPlaceModel? = null
        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            happyPlaceDetailModel = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }
        if (happyPlaceDetailModel != null){
            setSupportActionBar(binding?.toolbarHappyPlaceDetail)
            supportActionBar!!.title = happyPlaceDetailModel.title

            if (supportActionBar != null){
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }

            binding?.toolbarHappyPlaceDetail?.setNavigationOnClickListener {
                onBackPressed()
            }

            binding?.ivPlaceImage?.setImageURI(Uri.parse(happyPlaceDetailModel.image))
            binding?.tvDescription?.text = happyPlaceDetailModel.description
            binding?.tvLocation?.text = happyPlaceDetailModel.location

            binding?.btnViewOnMap?.setOnClickListener{
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, happyPlaceDetailModel)
                startActivity(intent)
            }
        }

    }
}