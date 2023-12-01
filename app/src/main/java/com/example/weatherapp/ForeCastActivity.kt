package com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.adapter.ForeCastAdapter
import com.example.weatherapp.mvvm.WeatherVm
import com.example.weatherapp.service.LocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class ForeCastActivity : AppCompatActivity() {


    private lateinit var adapterForeCastAdapter: ForeCastAdapter
    lateinit var viM : WeatherVm
    lateinit var rvForeCast: RecyclerView


    var longi : String = ""
    var lati: String = ""


    private lateinit var locationHelper: LocationHelper


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fourdayforecast)





        viM = ViewModelProvider(this).get(WeatherVm::class.java)

        locationHelper = LocationHelper(this)



        adapterForeCastAdapter = ForeCastAdapter()

        rvForeCast = findViewById<RecyclerView>(R.id.rvForeCast)


        val sharedPrefs = SharedPrefs.getInstance(this)
        val city = sharedPrefs.getValueOrNull("city")


        Log.d("Prefs", city.toString())



        if (city!=null){


            viM.getForecastUpcoming(city)

        } else {


            if (locationHelper.isLocationPermissionGranted()) {
                // Permission is granted, request location updates
                requestLocationUpdates()
            } else {
                // Request location permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    Utils.LOCATION_PERMISSION_REQUEST_CODE
                )
            }


        }




        viM.forecastWeatherLiveData.observe(this, Observer {

            val setNewlist = it as List<WeatherList>



            Log.d("Forecast LiveData", setNewlist.toString())



            adapterForeCastAdapter.setList(setNewlist)


            rvForeCast.adapter = adapterForeCastAdapter



        })





    }




    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestLocationUpdates() {
        locationHelper.requestLocationUpdates { location ->
            // Log latitude and longitude here
            val latitude = location.latitude
            val longitude = location.longitude

            viM.getForecastUpcoming(null, latitude.toString(), longitude.toString())
            logLocation(latitude, longitude)
        }
    }

    private fun logLocation(latitude: Double, longitude: Double) {
        // Log the latitude and longitude
        val message = "Latitude: $latitude, Longitude: $longitude"

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Utils.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, request location updates
                requestLocationUpdates()
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(
                    this,
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }



}