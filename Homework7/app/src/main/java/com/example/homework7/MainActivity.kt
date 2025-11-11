package com.example.homework7

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import android.widget.Button

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import android.graphics.Color

import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.maps.internal.PolylineEncoding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    private val taipei101 = LatLng(25.033611, 121.565000)
    private val taipeiMainStation = LatLng(25.047924, 121.517081)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true

            map.addMarker(MarkerOptions().position(taipei101).title("台北101"))
            map.addMarker(MarkerOptions().position(taipeiMainStation).title("台北車站"))

            drawRoute(TravelMode.WALKING)

            setupModeButtons()

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 0
            )
        }
    }

    private fun setupModeButtons() {
        val btnDriving = findViewById<Button>(R.id.btn_driving)
        val btnWalking = findViewById<Button>(R.id.btn_walking)
        val btnBicycling = findViewById<Button>(R.id.btn_bicycling)

        btnDriving.setOnClickListener { drawRoute(TravelMode.DRIVING) }
        btnWalking.setOnClickListener { drawRoute(TravelMode.WALKING) }
        btnBicycling.setOnClickListener { drawRoute(TravelMode.BICYCLING) }
    }

    private fun drawRoute(mode: TravelMode) {
        map.clear()
        map.addMarker(MarkerOptions().position(taipei101).title("台北101"))
        map.addMarker(MarkerOptions().position(taipeiMainStation).title("台北車站"))


        val apiKey = packageManager.getApplicationInfo(packageName,
            PackageManager.GET_META_DATA)
            .metaData.getString("com.google.android.geo.API_KEY")

        if (apiKey.isNullOrEmpty()) {
            return
        }

        val geoApiContext = GeoApiContext.Builder()
            .apiKey(apiKey)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val directionsResult =
                    DirectionsApi.newRequest(geoApiContext)
                        .origin(com.google.maps.model.LatLng(taipei101.latitude, taipei101.longitude))
                        .destination(com.google.maps.model.LatLng(taipeiMainStation.latitude, taipeiMainStation.longitude))
                        .mode(mode)
                        .await()

                CoroutineScope(Dispatchers.Main).launch {
                    if (directionsResult.routes.isNotEmpty()) {
                        val route = directionsResult.routes[0]

                        val decodedPath =
                            PolylineEncoding.decode(route.overviewPolyline.encodedPath)

                        val polylineOptions = PolylineOptions()
                            .addAll(decodedPath.map { LatLng(it.lat, it.lng) })
                            .color(Color.RED)
                            .width(15f)

                        map.addPolyline(polylineOptions)

                        val bounds = LatLngBounds.Builder()
                        decodedPath.map { LatLng(it.lat, it.lng) }.forEach { bounds.include(it) }
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}