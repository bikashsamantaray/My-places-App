package com.bikash.myplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bikash.myplaces.database.DatabaseHandler
import com.bikash.myplaces.databinding.ActivityMainBinding
import com.bikash.myplaces.models.HappyPlaceModel
import com.happyplaces.adapters.HappyPlacesAdapter
import pl.kitek.rvswipetodelete.SwipeToDeleteCallback
import pl.kitek.rvswipetodelete.SwipeToEditCallback

class MainActivity : AppCompatActivity() {
    private var binding : ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.fabAddHappyPlace?.setOnClickListener{
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)

        }
        getHappyPlacesListFromLocalDB()
    }

    private fun setUpHappyPlacesRecyclerView(happyPlaceList: ArrayList<HappyPlaceModel>){
        binding?.rvHappyPlaceList?.layoutManager = LinearLayoutManager(this)
        binding?.rvHappyPlaceList?.setHasFixedSize(true)
        val placesAdapter = HappyPlacesAdapter(this,happyPlaceList)
        binding?.rvHappyPlaceList?.adapter = placesAdapter
        placesAdapter.setOnClickListener(object : HappyPlacesAdapter.OnClickListener{
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity, HappyPlaceDetailActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAILS, model)
                startActivity(intent)
            }
        })

        val editSwipeHandler = object : SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvHappyPlaceList?.adapter as HappyPlacesAdapter
                adapter.notifyEditItem(this@MainActivity,viewHolder.adapterPosition, ADD_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }

        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlaceList)


        val deleteSwipeHandler = object : SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvHappyPlaceList?.adapter as HappyPlacesAdapter
                adapter.removeAt(viewHolder.adapterPosition)

                getHappyPlacesListFromLocalDB()
            }
        }

        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlaceList)
    }

    private fun getHappyPlacesListFromLocalDB(){
        val dbHandler = DatabaseHandler(this)
        val getHappyPlaceList: ArrayList<HappyPlaceModel> = dbHandler.getHappyPlacesList()

        if (getHappyPlaceList.size > 0){
            for (i in getHappyPlaceList){
                binding?.rvHappyPlaceList?.visibility = View.VISIBLE
                binding?.tvNoRecordAvailable?.visibility = View.GONE
                setUpHappyPlacesRecyclerView(getHappyPlaceList)
            }
        }else{
            binding?.rvHappyPlaceList?.visibility = View.GONE
            binding?.tvNoRecordAvailable?.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                getHappyPlacesListFromLocalDB()
            }else{
                Log.e("Activity","Cancelled or back pressed")
            }
        }
    }

    companion object {
        var ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        var EXTRA_PLACE_DETAILS = "extra_place_details"
    }

}