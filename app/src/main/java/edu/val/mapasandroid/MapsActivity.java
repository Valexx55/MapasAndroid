package edu.val.mapasandroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;

import edu.val.mapasandroid.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;//la m dice qu es un atributo de la clase "variable miembro"
    private ActivityMapsBinding binding;
    private LocationManager locationManager;//nos permite comprobar si el GPS está activo (u otro proveedor de ubicación)

    private FusedLocationProviderClient fusedLocationProviderClient;//este obtiene la ubicación
    private LocationRequest locationRequest;//la precisión y la frencuencia, la determina éste
    private LocationCallback locationCallback;//cuando el fused, tiene el dato, llama a éste, que es el callback

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("ETIQUETA_LOG", "El mapa se ha cargado");
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng lat_long_sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(lat_long_sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(lat_long_sydney));


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void mostrarUbicacionMapa (View view)
    {
        Log.d("ETIQUETA_LOG", "quiere conocer su ubicación");
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 535);
    }

    private boolean gpsActivado ()
    {
        boolean gps_activo = false;

            gps_activo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        return  gps_activo;
    }

    private void mostrarDireccionPostal (Location ubicacion)
    {
        try{
            Geocoder geocoder = new Geocoder(this, new Locale("es"));
            List<Address> dirs= geocoder.getFromLocation(ubicacion.getLatitude(),ubicacion.getLongitude(), 1);
            if (dirs!=null && dirs.size()>0) {
                Address direccion = dirs.get(0);
                Log.d("ETIQUETA_LOG", "Dirección = " + direccion.getAddressLine(0) + " CP "+
                        direccion.getPostalCode() + " Localidad "
                        + direccion.getLocality());

            }
        }catch (Exception e)
        {
            Log.e("ETIQUETA_LOG", "Error obteniendo la dirección postal", e);
        }
    }

    private void mostrarUbicacionObtenida (Location location)
    {
        Log.d("ETIQUETA_LOG", "Mostrando la ubicación obtenida");
        LatLng ubicacion_actual =  new LatLng(location.getLatitude(),location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(ubicacion_actual).title("Estoy AQuí").snippet("JEREZ no es Cádiz"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(ubicacion_actual));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom (ubicacion_actual, 13));
        mostrarDireccionPostal (location);

    }

    private void accederAlaUbicacionGPS ()
    {
        //TODO obtener ubicación GPS
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//quiero precisión alta
        locationRequest.setInterval(5000);//cada 5 segundos, recibire una actualización

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                //super.onLocationResult(locationResult);
                if (locationResult!=null)
                {
                    Location location = locationResult.getLastLocation();//obtengo la última ubicación
                    mostrarUbicacionObtenida (location);
                    //una vez obtenida la ubicación, desactivo el bucle, la frecuncia de pedirla
                    MapsActivity.this.fusedLocationProviderClient.removeLocationUpdates(MapsActivity.this.locationCallback);
                }
            }
        };

        //android studio nos obliga antes de llamar a obtener la ubicación, comprobar que el acceso por gps (permiso peligroso, está concedido)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }

    }

    private void solicitarActivicacionGPS()
    {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, 77);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (gpsActivado())
        {
            accederAlaUbicacionGPS();
        } else {
            Log.d("ETIQUETA_LOG", "Permiso uso GPS sigue denegado");
            Toast.makeText(this, "Sin acceso a la ubicación", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("ETIQUETA_LOG", "Permiso ubicación concedido");
            if (gpsActivado())
            {
                accederAlaUbicacionGPS ();
            } else {
                solicitarActivicacionGPS();
            }
        } else {
            Log.d("ETIQUETA_LOG", "Permiso uso GPS denegado");
            Toast.makeText(this, "Sin acceso a la ubicación", Toast.LENGTH_LONG).show();
        }
    }
}