package osga.gps.whereami

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest

import osga.gps.whereami.ui.theme.WhereAmITheme
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhereAmITheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                }
            }
        }
    }
}

@Composable
fun Greeting( modifier: Modifier = Modifier) {

    var name by remember { mutableStateOf("") }
    var greetingMessage by remember { mutableStateOf("") }
    var currentLatitude by remember { mutableDoubleStateOf(0.0) }
    var currentLongitude by remember { mutableDoubleStateOf(0.0) }

    val context = LocalContext.current
    var locationProvider = LocationServices.getFusedLocationProviderClient(context)
    var locationCallback = object : LocationCallback() {
        @SuppressLint("MissingPermission")
        override fun onLocationResult(result: LocationResult) {

            for (location in result.locations) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                Log.d("Location:", "${location.latitude} ${location.longitude}")
            }

            locationProvider.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        Log.d("Location2:", "${location.latitude} ${location.longitude}")
                    }
                }
                .addOnFailureListener {
                    Log.e("Location_error", "${it.message}")
                }

        }
    }

    @SuppressLint("MissingPermission")
    var locationUpdate : ()->Unit = {
        locationCallback.let {
            val locationRequest: LocationRequest =
                LocationRequest.create().apply {
                    interval = 5*1000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
            locationProvider.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {isGranted: Map<String, Boolean> ->
        Log.d("PERMISSIONS", "Launcher result: $isGranted")
        if (isGranted.containsValue(false)) {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            locationUpdate.invoke()
        }
    }

    LaunchedEffect(true) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (!hasPermissions(permissions, context)) {
            permissionLauncher.launch(permissions)
        } else {
            locationUpdate.invoke()
        }
    }

    Column {
        Text(
            text = greetingMessage,
            modifier = modifier
        )
        TextField(value = name, onValueChange = {newName ->
            name = newName
        })
        Button(onClick = {
            greetingMessage = "Bem-vindo ao android $name"
        }) {
            Text(text = "Cumprimentar")
        }
        Button(onClick = {
            greetingMessage = "partilhou a sua localização em: ${currentLatitude} ${currentLongitude} "
            shareLocation(context,  currentLatitude, currentLongitude, name)
        }) {
            Text(text = "Partilhar localização")
        }
    }


}

fun hasPermissions(permission: Array<String>, context: Context): Boolean {
    for (p in permission) {
        if (ActivityCompat.checkSelfPermission(
                context,
                p
            ) != PackageManager.PERMISSION_GRANTED
        ) return false
    }
    return true
}

fun shareLocation(context:Context, latitude : Double, longitude: Double, name : String){
    val uri = "https://www.google.com/maps/?q=$latitude,$longitude"
    val sharingIntent = Intent(Intent.ACTION_SEND)
    sharingIntent.type = "text/plain"
    sharingIntent.putExtra(Intent.EXTRA_TEXT, uri)
    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Olá eu sou o $name e estou em...")
    context.startActivity(Intent.createChooser(sharingIntent, "Partilhar localização"))
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WhereAmITheme {
        Greeting()
    }
}

