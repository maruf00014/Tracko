package com.example.maruf.tracko;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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

public class TrackOtherActivity extends FragmentActivity implements OnMapReadyCallback {


    Marker marker;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private String location,name;
    private Double latitude;
    private Double logitude;
    private String uid;
    private LatLng latLng;
    GoogleMap map;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);

        Intent intent = getIntent();
        uid = intent.getStringExtra("id");


        firebaseDatabase = FirebaseDatabase.getInstance();


        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.getMapAsync(this);


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        getData();


    }


    private void getData(){
        databaseReference = firebaseDatabase.getReference().child("Users").child(uid);
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
                    //Toast.makeText(TrackOtherActivity.this, location, Toast.LENGTH_LONG).show();

                    updateMarker();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void updateMarker(){

        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        map.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
        marker = map.addMarker(new MarkerOptions().position(latLng).title(name));
        // .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon)));
        marker.showInfoWindow();




    }

}
