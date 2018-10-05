package com.example.maruf.tracko;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

    GPSTracker gps;
    private Timer timerExecutor = new Timer();
    private TimerTask doAsynchronousTaskExecutor;

    GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user!=null) {
            uid = user.getUid();
        }


        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("Users").child(uid);


        startTracking();
        timerExecutor = new Timer();
        startBackgroundPerformExecutor();




        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapfragment);
        mapFragment.getMapAsync(this);

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
                location = dataSnapshot.child("Location").getValue(String.class);
                name = dataSnapshot.child("Name").getValue(String.class);
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
    protected void onDestroy() {
        doAsynchronousTaskExecutor.cancel();
        timerExecutor.cancel();
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

            databaseReference.child("Location").setValue(latitude+","+longitude);
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
        timerExecutor.schedule(doAsynchronousTaskExecutor, 0, 15000);
    }
}
