package com.example.localisationapp;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Color;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RequestQueue requestQueue;
    private final Handler handler = new Handler();
    private final int REFRESH_INTERVAL = 5000; // 5 secondes maintenant
    private int busId = 1; // Default bus ID
    private Runnable refreshRunnable; // Pour gérer le rafraîchissement

    // Zoom level for country view (comme dans votre version originale)
    private static final float COUNTRY_ZOOM = 5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Get selected bus_id from intent
        busId = getIntent().getIntExtra("bus_id", 1); // Changé pour correspondre à MainActivity

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set initial wide view showing countries (comme dans votre version originale)
        LatLng defaultLocation = new LatLng(20, 0); // Centered view
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, COUNTRY_ZOOM));

        // Load initial positions
        refreshMap();

        // Start periodic updates
        startAutoRefresh();
    }

    private void refreshMap() {
        String showUrl = "http://10.0.2.2/php/ws/showPositionsByBus.php?bus_id=" + busId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                showUrl,
                null,
                response -> {
                    try {
                        mMap.clear();

                        if (!response.getBoolean("success")) {
                            Toast.makeText(this, "Server error: " + response.optString("message"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray positions = response.getJSONArray("positions");

                        // Add markers and optionally draw a path
                        List<LatLng> points = new ArrayList<>();
                        for (int i = 0; i < positions.length(); i++) {
                            JSONObject pos = positions.getJSONObject(i);
                            LatLng location = new LatLng(
                                    pos.getDouble("latitude"),
                                    pos.getDouble("longitude")
                            );
                            points.add(location);

                            mMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title("Bus " + busId)
                                    .snippet(pos.getString("date"))
                            );
                        }

                        // Draw polyline if we have multiple points
                        if (points.size() > 1) {
                            mMap.addPolyline(new PolylineOptions()
                                    .addAll(points)
                                    .color(Color.BLUE)
                                    .width(5));
                        }

                        if (positions.length() == 0) {
                            Toast.makeText(this, "No positions for this bus", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing positions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("MAP_ERROR", "Error parsing positions", e);
                    }
                },
                error -> {
                    Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MAP_ERROR", "Network error", error);
                }
        );

        requestQueue.add(request);
    }

    private void startAutoRefresh() {
        // Créer le Runnable pour le rafraîchissement
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshMap();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };

        // Démarrer le cycle de rafraîchissement
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Arrêter les rafraîchissements quand l'activité est en pause
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Redémarrer les rafraîchissements quand l'activité reprend
        if (refreshRunnable != null) {
            handler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer le Handler
        handler.removeCallbacksAndMessages(null);
    }
}