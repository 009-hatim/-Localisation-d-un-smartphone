package com.example.localisationapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    private RequestQueue requestQueue;
    private static final String INSERT_URL = "http://10.0.2.2/PfeB/php/ws/CreatePosition.php";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final int REQUEST_PERMISSIONS_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonMap = findViewById(R.id.buttonMap);
        buttonMap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });


        requestQueue = Volley.newRequestQueue(this);
        checkAndRequestPermissions();

    }

    private void checkAndRequestPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        };

        boolean allPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permissions denied. Some features may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationUpdates() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    60000,
                    150,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            altitude = location.getAltitude();
                            accuracy = location.getAccuracy();

                            String msg = String.format(
                                    getResources().getString(R.string.new_location),
                                    latitude, longitude, altitude, accuracy
                            );
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                            addPosition(latitude, longitude);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                            String newStatus = "";
                            switch (status) {
                                case LocationProvider.OUT_OF_SERVICE:
                                    newStatus = "OUT_OF_SERVICE";
                                    break;
                                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                                    newStatus = "TEMPORARILY_UNAVAILABLE";
                                    break;
                                case LocationProvider.AVAILABLE:
                                    newStatus = "AVAILABLE";
                                    break;
                            }
                            String msg = String.format(
                                    getResources().getString(R.string.provider_new_status),
                                    provider, newStatus
                            );
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                            String msg = String.format(
                                    getResources().getString(R.string.provider_enabled),
                                    provider
                            );
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            String msg = String.format(
                                    getResources().getString(R.string.provider_disabled),
                                    provider
                            );
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }

    private void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                INSERT_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("LOCATION_SAVE", "Server response: " + response);
                        Toast.makeText(MainActivity.this, "Position saved!", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMsg = "Error saving position: " + (error.getMessage() != null ? error.getMessage() : "Unknown error");
                        Log.e("LOCATION_SAVE", errorMsg);
                        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date", SDF.format(new Date()));

                String androidId = "unknown";
                try {
                    androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    if (androidId == null) {
                        androidId = "null_android_id";
                    }
                } catch (Exception e) {
                    androidId = "error_android_id";
                }
                params.put("imei", androidId);

                return params;
            }
        };

        Log.d("MY_APP", "Sending to server - lat: " + lat + ", lon: " + lon);
        Log.d("MY_APP", "Full request: " + new HashMap<String, String>() {{
            put("latitude", String.valueOf(lat));
            put("longitude", String.valueOf(lon));
            put("date", SDF.format(new Date()));
            put("imei", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        }}.toString());
        requestQueue.add(request);
    }
}