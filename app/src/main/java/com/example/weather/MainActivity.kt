package com.example.weather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.weatherapp.models.Coord

import com.weatherapp.models.WeatherResponse
import com.weatherapp.network.WeatherService
import com.weatherapp.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var mLatitude: Double =0.0

    // A global variable for Current Longitude
    private var mLongitude: Double =0.00
    private var mcustomDialog:Dialog?=null
    private lateinit var msharedprefrences:SharedPreferences
    private lateinit var mFusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchView.setOnSearchClickListener {
            var intent=Intent(Intent.ACTION_CHOOSER)
            intent.setData(Uri.parse("geo:47.4925,19.0513"))
            val choose=Intent.createChooser(intent,"Map")
            startActivity(choose)

        }

        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
msharedprefrences=getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)
        setupUi()
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
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

    /**
     * A function which is used to verify that the location or GPS is enable or not of the user's device.
     */
    private fun isLocationEnabled(): Boolean {

        // This provides access to the system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    /**
     * A function to request the current location. Using the fused location provider client.
     */



    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority =LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()


        )}


    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            Log.i("Current Latitude", "$mLatitude")
             mLongitude = mLastLocation.longitude
            Log.i("Current Longitude", "$mLongitude")

            // TODO (STEP 6: Pass the latitude and longitude as parameters in function)
            getLocationWeatherDetails()
        }
    }


    /**
     * Function is used to get the weather details of the current location based on the latitude longitude
     */
    private fun getLocationWeatherDetails() {

        if (Constants.isNetworkAvailable(this@MainActivity)) {


            val retrofit: Retrofit = Retrofit.Builder()
                // API base URL.
                .baseUrl(Constants.BASE_URL)
                /** Add converter factory for serialization and deserialization of objects. */
                /**
                 * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
                 * decoding from JSON (when no charset is specified by a header) will use UTF-8.
                 */
                .addConverterFactory(GsonConverterFactory.create())
                /** Create the Retrofit instances. */
                .build()
            // END

            // TODO (STEP 5: Further step for API call)
            // START
            /**
             * Here we map the service interface in which we declares the end point and the API type
             *i.e GET, POST and so on along with the request parameter which are required.
             */
            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
             * Here we pass the required param in the service
             */
            val listCall: Call<WeatherResponse> = service.getcitydata(
                mLatitude,mLongitude,Constants.METRIC_UNIT,Constants.APP_ID
            )
            showcustomdialog()

            // Callback methods are executed using the Retrofit callback executor.
            listCall.enqueue(object : Callback<WeatherResponse> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    response: Response<WeatherResponse>,
                    retrofit: Retrofit
                ) {

                    // Check weather the response is success or not.
                    if (response.isSuccess) {
                        hidecustomdialog()

                        /** The de-serialized response body of a successful response. */
                        val weatherList: WeatherResponse = response.body()
                        val weatherresponseJsonstring=Gson().toJson(weatherList)
                        val editor=msharedprefrences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherresponseJsonstring)
                        editor.apply()
                        setupUi()
                        Log.i("Response Result", "$weatherList")
                    } else {
                        // If the response is not success then we check the response code.
                        val sc = response.code()
                        when (sc) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    hidecustomdialog()
                    Log.e("Errorrrrr", t.message.toString())
                }
            })
            // END

        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun showcustomdialog(){
        mcustomDialog= Dialog(this)
        mcustomDialog!!.setContentView(R.layout.dialog)
        mcustomDialog!!.show()


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.refresh,menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_refresh->{
                getLocationWeatherDetails()

                return true
                }


            else -> super.onOptionsItemSelected(item)
        }

        }



    private fun hidecustomdialog(){
        if (mcustomDialog!=null){
            mcustomDialog!!.dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUi(){
        val weatherjsonstring=msharedprefrences.getString(Constants.WEATHER_RESPONSE_DATA,"")
        if (!weatherjsonstring.isNullOrEmpty()){
            val weatherlist=Gson().fromJson(weatherjsonstring,WeatherResponse::class.java)
            for (i in weatherlist.weather.indices){
                Log.i("weather is",weatherlist.weather.toString())
                tv_main.text=weatherlist.weather[i].main
                tv_main_description.text=weatherlist.weather[i].description
                tv_humidity.text=weatherlist.main.humidity.toString()+"per cent"
                tv_temp.text=weatherlist.main.temp.toString()+getUnit(application.resources.configuration.locales.toString())
                tv_sunrise_time.text=unixtime(weatherlist.sys.sunrise.toLong())
                tv_sunset_time.text=unixtime(weatherlist.sys.sunset.toLong())
                tv_min.text=weatherlist.main.temp_min.toString() + "min"
                tv_max.text=weatherlist.main.temp_max.toString() + "max"
                tv_speed.text=weatherlist.wind.toString()
                tv_name.text=weatherlist.name
                tv_country.text=weatherlist.sys.country.toString()
                when (weatherlist.weather[i].icon) {
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }

            }
        }

    }
    private fun getUnit(value:String):String{
        var value="C"
        if ("US"==value||"LR"==value||"MM"==value){
            value="F"
        }

        return value
    }
    private fun unixtime(timex:Long):String?{
        val date=Date(timex*1000L)
        val sdf=SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone= TimeZone.getDefault()
        return sdf.format(date)


    }
}


