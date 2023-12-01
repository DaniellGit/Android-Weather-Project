package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.adapter.WeatherToday
import com.example.weatherapp.databinding.TestlayoutBinding
import com.example.weatherapp.mvvm.WeatherVm
import com.example.weatherapp.service.LocationHelper
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.DelicateCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    lateinit var viM: WeatherVm
    lateinit var adapter: WeatherToday
    private lateinit var binding: TestlayoutBinding
    var longi: String = ""
    var lati: String = ""
    private lateinit var locationHelper: LocationHelper

    private lateinit var gClient: GoogleSignInClient



    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationhelper = NotificationHelper(this)

        binding = DataBindingUtil.setContentView(this, R.layout.testlayout)
        viM = ViewModelProvider(this).get(WeatherVm::class.java)

        binding.lifecycleOwner = this
        binding.vm = viM

        locationHelper = LocationHelper(this)

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

        adapter = WeatherToday()

        val sharedPrefs = SharedPrefs.getInstance(this@MainActivity)
        sharedPrefs.clearCityValue()

        viM.todayWeatherLiveData.observe(this, Observer {
            val setNewlist = it as List<WeatherList>
            Log.e("TODayweather list", it.toString())
            adapter.setList(setNewlist)
            binding.forecastRecyclerView.adapter = adapter
        })

        viM.closetorexactlysameweatherdata.observe(this, Observer {
            val temperatureFahrenheit = it!!.main?.temp
            val temperatureCelsius = (temperatureFahrenheit?.minus(273.15))
            val temperatureFormatted = String.format("%.2f", temperatureCelsius)

            for (i in it.weather) {
                binding.descMain.text = i.description
                if (i.main.toString() == "Rain" ||
                    i.main.toString() == "Drizzle" ||
                    i.main.toString() == "Thunderstorm" ||
                    i.main.toString() == "Clear"
                ){
                    notificationhelper.startNotification()
                    Log.e("MAIN", i.main.toString())
                }
            }

            binding.tempMain.text = "$temperatureFormatted°"
            binding.humidityMain.text = it.main!!.humidity.toString()
            binding.windSpeed.text = it.wind?.speed.toString()

            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = inputFormat.parse(it.dtTxt!!)
            val outputFormat = SimpleDateFormat("d MMMM EEEE", Locale.getDefault())
            val dateanddayname = outputFormat.format(date!!)
            binding.dateDayMain.text = dateanddayname
            binding.chanceofrain.text = "${it.pop.toString()}%"

            // setting the icon
            for (i in it.weather) {
                when (i.icon) {
                    "01d" -> binding.imageMain.setImageResource(R.drawable.d1)
                    "01n" -> binding.imageMain.setImageResource(R.drawable.n1)
                    "02d" -> binding.imageMain.setImageResource(R.drawable.twod)
                    "02n" -> binding.imageMain.setImageResource(R.drawable.twon)
                    "03d", "03n" -> binding.imageMain.setImageResource(R.drawable.threedn)
                    "10d" -> binding.imageMain.setImageResource(R.drawable.tend)
                    "10n" -> binding.imageMain.setImageResource(R.drawable.tenn)
                    "04d", "04n" -> binding.imageMain.setImageResource(R.drawable.fourdn)
                    "09d", "09n" -> binding.imageMain.setImageResource(R.drawable.ninedn)
                    "11d", "11n" -> binding.imageMain.setImageResource(R.drawable.elevend)
                    "13d", "13n" -> binding.imageMain.setImageResource(R.drawable.thirteend)
                    "50d", "50n" -> binding.imageMain.setImageResource(R.drawable.fiftydn)
                }
            }
        })

        val searchEditText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(Color.WHITE)

        binding.next5Days.setOnClickListener {
            startActivity(Intent(this, ForeCastActivity::class.java))
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val sharedPrefs = SharedPrefs.getInstance(this@MainActivity)
                sharedPrefs.setValueOrNull("city", query!!)
                if (!query.isNullOrEmpty()) {
                    viM.getWeather(query)
                    binding.searchView.setQuery("", false)
                    binding.searchView.clearFocus()
                    binding.searchView.isIconified = true
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        val faqButton = findViewById<Button>(R.id.faqButton)

        faqButton.setOnClickListener {
            val intent = Intent(this, FaqActivity::class.java)
            startActivity(intent)
        }

        val logoutButton = findViewById<Button>(R.id.logout)


        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Log out the user (Firebase Authentication)

            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestLocationUpdates() {
        locationHelper.requestLocationUpdates { location ->
            val latitude = location.latitude
            val longitude = location.longitude
            viM.getWeather(null, latitude.toString(), longitude.toString())
            logLocation(latitude, longitude)
        }
    }

    private fun logLocation(latitude: Double, longitude: Double) {
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
                requestLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
