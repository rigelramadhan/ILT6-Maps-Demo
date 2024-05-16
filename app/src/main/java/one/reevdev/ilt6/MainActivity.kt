package one.reevdev.ilt6

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import one.reevdev.ilt6.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val sampleData = listOf(
        Triple("Test 1", -6.175867, 106.827238),
        Triple("Test 2", -6.275867, 106.727238),
        Triple("Test 3", -6.375867, 106.627238),
        Triple("Test 4", -6.475867, 106.527238),
        Triple("Test 5", -6.575867, 106.427238),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(this, BuildConfig.API_KEY)
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
        binding.btnSampleLocation.setOnClickListener {
            val boundsBuilder = LatLngBounds.Builder()
            mMap.clear()

            sampleData.forEach {
                val position = LatLng(it.second, it.third)
                addMarker(position, it.first)
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

        binding.btnGetNearby.setOnClickListener {
            val query = binding.edtCategory.text.toString()
            if (query.isNotBlank()) {
                checkPermission {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        nearbySearch(query, LatLng(it.latitude, it.longitude), 1000.0)
                    }
                }
            }
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
                addMarker(it.latLng, it.name)

                animateCamera(CameraUpdateFactory.newLatLng(it.latLng))
                Toast.makeText(this@MainActivity, "This is ${it.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun nearbySearch(query: String, position: LatLng, radius: Double) {
        val returnFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)

        val searchByTextRequest = SearchByTextRequest.builder(query, returnFields)
            .setLocationBias(CircularBounds.newInstance(position, radius))
            .build()

        val placesClient = Places.createClient(this)
        placesClient.searchByText(searchByTextRequest)
            .addOnSuccessListener {
                val boundsBuilder = LatLngBounds.builder()

                it.places.forEach { place ->
                    with(place) {
                        latLng?.let { placeLatLng ->
                            boundsBuilder.include(placeLatLng)
                            addMarker(placeLatLng, name, address)
                        }
                    }
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

    private fun addMarker(position: LatLng, title: String?, snippet: String? = null) {
        mMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(title)
                .snippet(snippet)
        )
    }

    private fun checkPermission(action: (() -> Unit)? = null) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        } else {
            action?.invoke()
        }
    }

    private fun getMyLocation() {
        checkPermission {
            mMap.isMyLocationEnabled = true
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