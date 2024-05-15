package one.reevdev.ilt6

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import one.reevdev.ilt6.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(maps: GoogleMap) {
        mMap = maps

        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isIndoorLevelPickerEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }

        val monas = LatLng(-6.175867, 106.827238)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(monas, 15f))

        setupOnMapClick()
        setupOnPoiClick()
        setupButtons()

        getMyLocation()
    }

    private fun setupButtons() {
        val data = listOf(
            Triple("Test 1", -6.175867, 106.827238),
            Triple("Test 2", -6.275867, 106.727238),
            Triple("Test 3", -6.375867, 106.627238),
            Triple("Test 4", -6.475867, 106.527238),
            Triple("Test 5", -6.575867, 106.427238),
        )

        val boundsBuilder = LatLngBounds.Builder()

        binding.btnSampleLocation.setOnClickListener {
            mMap.clear()

            data.forEach {
                val position = LatLng(it.second, it.third)

                mMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(it.first)
                )

                boundsBuilder.include(position)
            }

            val bounds = boundsBuilder.build()

            mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    resources.displayMetrics.widthPixels,
                    resources.displayMetrics.heightPixels,
                    300
                )
            )
        }
    }

    private fun setupOnMapClick() {
        with(mMap) {
            setOnMapClickListener {
                clear()
                addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("You clicked here!")
                        .snippet("Click on any position to go there.")
                )

                animateCamera(CameraUpdateFactory.newLatLng(it))
            }
        }
    }

    private fun setupOnPoiClick() {
        with(mMap) {
            setOnPoiClickListener {
                clear()
                addMarker(
                    MarkerOptions()
                        .position(it.latLng)
                        .title(it.name)
                )

                animateCamera(CameraUpdateFactory.newLatLng(it.latLng))
                Toast.makeText(this@MainActivity, "This is ${it.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getMyLocation()
        }
    }
}