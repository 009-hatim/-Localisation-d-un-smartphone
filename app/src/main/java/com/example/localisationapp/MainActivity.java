package com.example.localisationapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.view.View;  // Doit être utilisé quelque part
import android.widget.AdapterView;  // Doit être utilisé

public class MainActivity extends AppCompatActivity {


    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    private RequestQueue requestQueue;
    private static final String INSERT_URL = "http://10.0.2.2/php/ws/CreatePosition.php";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final int REQUEST_PERMISSIONS_CODE = 1;

    private int selectedBusId = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonMap = findViewById(R.id.buttonMap);
        Spinner spinnerBus = findViewById(R.id.spinnerBus);
        requestQueue = Volley.newRequestQueue(this);

        // Initialize with empty adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBus.setAdapter(adapter);

        // Load buses from server
        loadBusesFromServer(spinnerBus);

        buttonMap.setOnClickListener(v -> {
            // Pass the selected bus_id to MapsActivity
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            intent.putExtra("bus_id", selectedBusId); // Pass bus_id as an extra
            startActivity(intent);
        });


        checkAndRequestPermissions();
    }

    private void loadBusesFromServer(Spinner spinnerBus) {
        String busesUrl = "http://10.0.2.2/php/ws/loadbus.php";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                busesUrl,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray busesArray) {
                        try {
                            List<String> busList = new ArrayList<>();
                            List<Integer> busIds = new ArrayList<>();

                            for (int i = 0; i < busesArray.length(); i++) {
                                JSONObject bus = busesArray.getJSONObject(i);
                                int id = bus.getInt("id");
                                String matricule = bus.getString("matricule");

                                if (!busIds.contains(id)) {
                                    busIds.add(id);
                                    busList.add(id + " - " + matricule);
                                }
                            }

                            updateSpinnerAdapter(spinnerBus, busList);

                            // Ajouter le listener APRÈS avoir peuplé le Spinner
                            spinnerBus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    String selectedItem = (String) parent.getItemAtPosition(position);
                                    selectedBusId = Integer.parseInt(selectedItem.split(" - ")[0]);
                                    Log.d("BUS_SELECTION", "Bus sélectionné: " + selectedBusId);

                                    // Enregistrer immédiatement si on a une position
                                    if (latitude != 0 && longitude != 0) {
                                        addPosition(latitude, longitude);
                                        Toast.makeText(MainActivity.this,
                                                "Position enregistrée pour le bus " + selectedBusId,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                    selectedBusId = 1; // Valeur par défaut
                                }
                            });

                        } catch (JSONException e) {
                            Log.e("JSON_ERROR", "Erreur parsing données bus", e);
                            loadDefaultBuses(spinnerBus);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("NETWORK_ERROR", "Erreur chargement bus", error);
                        loadDefaultBuses(spinnerBus);
                    }
                }
        );

        requestQueue.add(request);
    }
    private void updateSpinnerAdapter(Spinner spinner, List<String> busList) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                busList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set default selection
        if (!busList.isEmpty()) {
            selectedBusId = Integer.parseInt(busList.get(0).split(" - ")[0]);
        }
    }

    private void loadDefaultBuses(Spinner spinnerBus) {
        List<String> busList = Arrays.asList(
                "1 - Bus A",
                "2 - Bus B",
                "3 - Bus C"
        );
        updateSpinnerAdapter(spinnerBus, busList);
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

                            // Enregistrer automatiquement avec le bus actuel
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
        // Vérification basique
        if (selectedBusId == 0) {
            selectedBusId = 1; // Fallback au bus par défaut
        }

        StringRequest request = new StringRequest(
                Request.Method.POST,
                INSERT_URL,
                response -> {
                    Log.d("LOCATION_SAVE", "Réponse: " + response);
                    Toast.makeText(MainActivity.this, "Position enregistrée", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String errorMsg = "Erreur: " + (error.getMessage() != null ? error.getMessage() : "Inconnue");
                    Log.e("LOCATION_SAVE", errorMsg);
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date", SDF.format(new Date()));
                params.put("bus_id", String.valueOf(selectedBusId)); // Utilise toujours le bus sélectionné

                // Gestion IMEI inchangée
                String androidId = "unknown";
                try {
                    androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    androidId = androidId != null ? androidId : "null_android_id";
                } catch (Exception e) {
                    androidId = "error_android_id";
                }
                params.put("imei", androidId);

                return params;
            }
        };

        requestQueue.add(request);
    }
}