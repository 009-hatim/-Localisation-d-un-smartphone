package com.example.localisationapp; // adapte avec ton vrai nom de package

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.os.Handler;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    String showUrl = "http://10.0.2.2/PfeB/php/localisation/showPositions.php";
    RequestQueue requestQueue;
    private Handler handler = new Handler();
    private final int REFRESH_INTERVAL = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize the request queue
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Optional: set initial camera position
        LatLng start = new LatLng(20, 0); // some zoomed-out location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 3));

        refreshMap();       // initial load
        startAutoRefresh(); // start periodic updates
    }


    private void refreshMap() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                showUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mMap.clear(); // 🧹 Clear all existing markers on the map

                        try {
                            JSONArray positions = response.getJSONArray("positions");

                            for (int i = 0; i < positions.length(); i++) {
                                JSONObject position = positions.getJSONObject(i);

                                double lat = position.getDouble("latitude");
                                double lng = position.getDouble("longitude");
                                String date = position.getString("date");
                                String imei = position.getString("imei");

                                LatLng location = new LatLng(lat, lng);

                                // 📍 Add marker with some info
                                mMap.addMarker(new MarkerOptions()
                                        .position(location)
                                        .title("IMEI: " + imei)
                                        .snippet("Date: " + date));
                            }

                            // Optionally, move camera to the latest position
                            if (positions.length() > 0) {
                                JSONObject latest = positions.getJSONObject(positions.length() - 1);
                                double lat = latest.getDouble("latitude");
                                double lng = latest.getDouble("longitude");

                                LatLng latestPosition = new LatLng(lat, lng);
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestPosition, 5f)); // Adjust zoom as needed
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void startAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshMap(); // call your map update method
                handler.postDelayed(this, REFRESH_INTERVAL); // repeat every 5 sec
            }
        }, REFRESH_INTERVAL);
    }



}
