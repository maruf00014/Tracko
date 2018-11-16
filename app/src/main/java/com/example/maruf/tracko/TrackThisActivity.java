package com.example.maruf.tracko;

import android.content.Intent;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;

public class TrackThisActivity extends FragmentActivity implements OnMapReadyCallback {


    Marker marker;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private String location,name ;
    private Double latitude;
    private Double logitude;
    private String uid;
    private LatLng latLng;
    private ImageButton startButton;
    private ImageButton stopButton;


    GPSTracker gps;
    private Timer timerExecutor = new Timer();
    private TimerTask doAsynchronousTaskExecutor;

    GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_this_layout);

//Check whether GPS tracking is enabled//

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            finish();
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user!=null) {
            uid = user.getUid();
        }


        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("Users").child(uid);

        SupportMapFragment trackThisFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.trackthisfragment);

        trackThisFragment.getMapAsync(this);


        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);

        if(!getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isTracking",false))
        {
            stopButton.setVisibility(View.INVISIBLE);
        }else {
            startButton.setVisibility(View.INVISIBLE);
        }


        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                        .edit()
                        .putBoolean("isTracking", true)
                        .apply();
                startButton.setVisibility(View.INVISIBLE);
                stopButton.setVisibility(View.VISIBLE);
                startTracking();
                timerExecutor = new Timer();
                startBackgroundPerformExecutor();

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doAsynchronousTaskExecutor.cancel();
                timerExecutor.cancel();
                getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                        .edit()
                        .putBoolean("isTracking", false)
                        .apply();
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.INVISIBLE);


            }
        });








    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getData();
    }


    private void getData(){

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                location = dataSnapshot.child("location").getValue(String.class);
                name = dataSnapshot.child("username").getValue(String.class);
                if(location != null ) {
                    String[] seperator = location.split(",");
                    latitude = Double.parseDouble(seperator[0].trim());
                    logitude = Double.parseDouble(seperator[1].trim());
                    latLng = new LatLng(latitude, logitude);
                    if(marker != null) marker.remove();
                    updateMarker();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void updateMarker(){

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
        marker = mMap.addMarker(new MarkerOptions().position(latLng).title(name));
        // .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon)));
        marker.showInfoWindow();



    }


    @Override
    public void onBackPressed() {

        Intent intent = new Intent(TrackThisActivity.this,MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {

        if(getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isTracking",false))
        {
            doAsynchronousTaskExecutor.cancel();
            timerExecutor.cancel();
            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isTracking", false)
                    .apply();
        }
        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .edit()
                .putBoolean("dialog", true)
                .apply();

        super.onDestroy();

    }

    private void startTracking(){
        gps = new GPSTracker(TrackThisActivity.this);
        if(gps.canGetLocation()){
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            databaseReference.child("location").setValue(latitude+","+longitude);
        }else {
            gps.showSettingsAlert();
        }
    }

    public void startBackgroundPerformExecutor() {
        final Handler handler = new Handler();
        doAsynchronousTaskExecutor = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            BackgroundPerformExecutor performBackgroundTask =
                                    new BackgroundPerformExecutor(
                                            getApplicationContext());
                            performBackgroundTask.execute(new Runnable() {
                                @Override public void run() {

                                    startTracking();

                                    Toast.makeText(getApplicationContext(),
                                            "Location Updated",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
        timerExecutor.schedule(doAsynchronousTaskExecutor, 0, 10000);
    }
}
