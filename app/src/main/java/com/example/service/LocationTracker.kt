package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class LocationInfo(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val city: String = "पता लगाया जा रहा है...",
    val state: String = "",
    val fullAddress: String = "लाइव GPS सिग्नल कनेक्ट हो रहा है...",
    val isTracking: Boolean = false,
    val lastUpdatedTime: Long = System.currentTimeMillis()
)

class LocationTracker(private val context: Context) : LocationListener {

    companion object {
        private const val TAG = "AmmuLocationTracker"
        @Volatile
        private var instance: LocationTracker? = null

        fun getInstance(context: Context): LocationTracker {
            return instance ?: synchronized(this) {
                instance ?: LocationTracker(context.applicationContext).also { instance = it }
            }
        }
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _locationInfo = MutableStateFlow(LocationInfo())
    val locationInfo: StateFlow<LocationInfo> = _locationInfo.asStateFlow()

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasLocationPermission()) {
            _locationInfo.value = _locationInfo.value.copy(
                fullAddress = "लोकेशन परमिशन की आवश्यकता है",
                isTracking = false
            )
            return
        }

        try {
            val hasGps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val hasNetwork = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            // Fetch last known location first for instantaneous display
            val lastGps = if (hasGps) locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNetwork = if (hasNetwork) locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
            val bestLast = lastGps ?: lastNetwork

            if (bestLast != null) {
                updateLocationDetails(bestLast)
            }

            if (hasGps) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds
                    5f,    // 5 meters
                    this
                )
            }
            if (hasNetwork) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    this
                )
            }

            _locationInfo.value = _locationInfo.value.copy(isTracking = true)
            Log.i(TAG, "Location tracking started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location tracking", e)
        }
    }

    fun stopTracking() {
        try {
            locationManager?.removeUpdates(this)
            _locationInfo.value = _locationInfo.value.copy(isTracking = false)
            Log.i(TAG, "Location tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location tracking", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        updateLocationDetails(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun updateLocationDetails(location: Location) {
        scope.launch {
            val geocoded = reverseGeocode(location.latitude, location.longitude)
            _locationInfo.value = LocationInfo(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                speed = location.speed,
                city = geocoded.first,
                state = geocoded.second,
                fullAddress = geocoded.third,
                isTracking = true,
                lastUpdatedTime = System.currentTimeMillis()
            )
        }
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): Triple<String, String, String> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("hi", "IN"))
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "भारत"
                val state = addr.adminArea ?: ""
                val thoroughfare = addr.subLocality ?: addr.thoroughfare ?: ""
                val formatted = listOfNotNull(thoroughfare.takeIf { it.isNotBlank() }, city, state).joinToString(", ")
                return@withContext Triple(city, state, formatted.ifBlank { "अक्षांश: $lat, देशांतर: $lon" })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding error", e)
        }
        return@withContext Triple("वर्तमान स्थान", "", "अक्षांश: ${String.format(Locale.US, "%.4f", lat)}, देशांतर: ${String.format(Locale.US, "%.4f", lon)}")
    }

    // --- Home Location & Google Maps Navigation Guidance ---
    private val prefs = context.getSharedPreferences("ammu_location_prefs", Context.MODE_PRIVATE)

    fun setHomeLocation(lat: Double, lng: Double, address: String = "घर") {
        prefs.edit()
            .putFloat("home_lat", lat.toFloat())
            .putFloat("home_lng", lng.toFloat())
            .putString("home_address", address)
            .apply()
        Log.i(TAG, "Home location set: $lat, $lng, $address")
    }

    fun setHomeLocationFromCurrent(): Boolean {
        val curr = _locationInfo.value
        if (curr.latitude != 0.0 && curr.longitude != 0.0) {
            setHomeLocation(curr.latitude, curr.longitude, curr.fullAddress)
            return true
        }
        return false
    }

    fun getHomeLocation(): Pair<Double, Double>? {
        if (!prefs.contains("home_lat")) return null
        val lat = prefs.getFloat("home_lat", 0f).toDouble()
        val lng = prefs.getFloat("home_lng", 0f).toDouble()
        return Pair(lat, lng)
    }

    fun getHomeAddress(): String {
        return prefs.getString("home_address", "घर सेट नहीं है") ?: "घर सेट नहीं है"
    }

    fun getDistanceToHomeKm(): Float? {
        val home = getHomeLocation() ?: return null
        val curr = _locationInfo.value
        if (curr.latitude == 0.0 && curr.longitude == 0.0) return null

        val results = FloatArray(1)
        Location.distanceBetween(curr.latitude, curr.longitude, home.first, home.second, results)
        return results[0] / 1000f // Return in Kilometers
    }

    fun getHomeNavigationSpeech(): String {
        val home = getHomeLocation()
        if (home == null) {
            return "कमांडर, आपने अभी तक घर की लोकेशन सेट नहीं की है। आप कंट्रोल सेंटर में 'सेट होम लोकेशन' दबाकर अपना घर सेट कर सकते हैं!"
        }

        val distKm = getDistanceToHomeKm()
        return if (distKm != null) {
            val distFormatted = String.format(Locale.US, "%.1f", distKm)
            val estMinutes = (distKm * 2.5).toInt().coerceAtLeast(3) // rough road estimate
            "कमांडर! अभी आप अपने घर से लगभग $distFormatted किलोमीटर दूर हैं। अगर आप इस मुख्य रास्ते से जाएंगे तो लगभग $estMinutes मिनट में जल्दी पहुँच जाएंगे! क्या मैं गूगल मैप्स नेविगेशन स्टार्ट कर दूँ?"
        } else {
            "कमांडर, आपकी वर्तमान GPS लोकेशन कनेक्ट हो रही है, 2 सेकंड में दूरी बता देती हूँ!"
        }
    }

    fun openGoogleMapsToHome(context: Context): Boolean {
        val home = getHomeLocation() ?: return false
        return try {
            val uri = android.net.Uri.parse("google.navigation:q=${home.first},${home.second}&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${home.first},${home.second}")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Google Maps", e)
            false
        }
    }

    fun getSpokenCurrentLocation(): String {
        val info = _locationInfo.value
        return if (info.latitude != 0.0 && info.longitude != 0.0) {
            if (info.city.isNotBlank() && info.city != "पता लगाया जा रहा है...") {
                "कमांडर! आप अभी ${info.fullAddress} में मौजूद हैं। आपकी लाइव लोकेशन एक्टिव है!"
            } else {
                "कमांडर! आपकी लोकेशन अक्षांश ${String.format(Locale.US, "%.2f", info.latitude)} और देशांतर ${String.format(Locale.US, "%.2f", info.longitude)} पर ट्रैक हो रही है!"
            }
        } else {
            "कमांडर! कृपया लोकेशन जीपीएस ऑन करें, मैं तुरंत आपकी लाइव जगह ट्रैक करके बता देती हूँ!"
        }
    }
}
