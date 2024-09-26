package com.example.landmarkremark
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var gMap: GoogleMap? = null
    private var searchView: SearchView? = null
    private var btNote: Button? = null
    private var markerAdapter: MarkerAdapter? = null
    private val markerList: MutableList<MarkerData> = ArrayList()
    private var currentMarker: Marker? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btNote = findViewById(R.id.btNote)
        btNote?.setOnClickListener(View.OnClickListener { showNoteDialog() })
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.id_Map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        //Request location access
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1)
            return
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationClient.lastLocation
                .addOnSuccessListener(this) { location: Location? ->
                    if (location != null) {
                        val currentLocation = LatLng(location.latitude, location.longitude)
                        gMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                    }
                }
        }
        searchView = findViewById(R.id.searchView)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchMarkersByUsername(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        val rvMarkers = findViewById<RecyclerView>(R.id.rvMarkers)
        rvMarkers.layoutManager = LinearLayoutManager(this)
        markerAdapter = MarkerAdapter(markerList, object : MarkerAdapter.OnMarkerClickListener {
            override fun onMarkerClick(markerData: MarkerData) {
                onMarkerdataClick(markerData)
            }
        })

        rvMarkers.adapter = markerAdapter

    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap!!.uiSettings.isMyLocationButtonEnabled = true
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }
        gMap!!.isMyLocationEnabled = true
        loadMarkersFromFirebase()
    }

    private fun showNoteDialog() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.layout_notemark, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()
        val btSave = dialogView.findViewById<Button>(R.id.btSave)
        val etName = dialogView.findViewById<EditText>(R.id.editTextName)
        val etNote = dialogView.findViewById<EditText>(R.id.editTextContent)
        fusedLocationClient.lastLocation
            .addOnSuccessListener(this) { location: Location? ->
                if (location != null) {
                    // Get the current location
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    btSave.setOnClickListener(View.OnClickListener {
                        val name = etName.text.toString()
                        val content = etNote.text.toString()
                        val database = FirebaseDatabase.getInstance()
                        val markersRef = database.getReference("markers")
                        val markerId = currentLocation.latitude.toString()
                            .replace(".", "_") + "_" + currentLocation.longitude.toString()
                            .replace(".", "_")
                        val markerData = MarkerData(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            name,
                            content
                        )
                        // Store the marker data using the location as the key
                        markersRef.child(markerId).setValue(markerData)
                        gMap!!.addMarker(
                            MarkerOptions()
                                .position(currentLocation)
                                .title(name)
                                .snippet(content)
                        )
                        gMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 17f))
                        onMarkerdataClick(markerData);
                        dialog.dismiss()
                    })
                } else {
                }
            }
        val btClose = dialogView.findViewById<Button>(R.id.btnClose)
        btClose.setOnClickListener { dialog.dismiss() }
    }

    private fun loadMarkersFromFirebase() {
        // Firebase database reference
        val database = FirebaseDatabase.getInstance()
        val markersRef = database.getReference("markers")
        // Listen for changes in the Firebase database and add markers to the map
        markersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gMap!!.clear()
                markerList.clear()
                // Loop through all the marker data and add each marker to the map
                for (markerSnapshot in snapshot.children) {
                    val markerData = markerSnapshot.getValue(MarkerData::class.java)
                    if (markerData != null) {
                        val position = LatLng(markerData.latitude, markerData.longitude)
                        gMap!!.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(markerData.name)
                                .snippet(markerData.content)
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun searchMarkersByUsername(username: String) {
        var username = username
        val database = FirebaseDatabase.getInstance()
        val markersRef = database.getReference("markers")
        markersRef.orderByChild("name").startAt(username).endAt(username + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    markerList.clear()
                    for (snapshot in dataSnapshot.children) {
                        val markerData = snapshot.getValue(MarkerData::class.java)
                        if (markerData != null) {
                            markerList.add(markerData)
                        }
                    }
                    markerAdapter!!.notifyDataSetChanged()
                    if (!markerList.isEmpty()) {
                        findViewById<View>(R.id.rvMarkers).visibility = View.VISIBLE
                        val firstPosition = LatLng(markerList[0].latitude, markerList[0].longitude)
                        gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 15f))
                    } else {
                        findViewById<View>(R.id.rvMarkers).visibility = View.GONE
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    fun onMarkerdataClick(markerData: MarkerData) {
        findViewById<View>(R.id.rvMarkers).visibility = View.GONE
        if (currentMarker != null) {
            currentMarker!!.remove()
        }
        val markerOptions = MarkerOptions()
            .position(LatLng(markerData.latitude, markerData.longitude))
            .title(markerData.name)
            .snippet(markerData.content)
        currentMarker = gMap!!.addMarker(markerOptions)
        val position = LatLng(markerData.latitude, markerData.longitude)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, 17f)
        gMap!!.animateCamera(cameraUpdate, 1000, null)
        currentMarker!!.showInfoWindow()
    }
}