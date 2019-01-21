package com.example.maruf.tracko;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.example.maruf.tracko.GpsService.LocationService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Trackthis extends AppCompatActivity {
    private static final String TAG = "TrackThisActivity";


    public LocationService locationService;
    private MapView mapView;
    private GoogleMap map;

    private Marker userPositionMarker;
    private Circle locationAccuracyCircle;
    private BitmapDescriptor userPositionMarkerBitmapDescriptor;
    private Polyline runningPathPolyline;
    private PolylineOptions polylineOptions;
    private int polylineWidth = 30;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private String uid;

    //boolean isZooming;
    //boolean isBlockingAutoZoom;

    boolean zoomable = true;

    Timer zoomBlockingTimer;
    boolean didInitialZoom;
    private Handler handlerOnUIThread;


    private BroadcastReceiver locationUpdateReceiver;
    private BroadcastReceiver predictedLocationReceiver;

    private ImageButton startButton;
    private ImageButton stopButton;
    Location flocation;
    /* Filater */
    private Circle predictionRange;
    BitmapDescriptor oldLocationMarkerBitmapDescriptor;
    BitmapDescriptor noAccuracyLocationMarkerBitmapDescriptor;
    BitmapDescriptor inaccurateLocationMarkerBitmapDescriptor;
    BitmapDescriptor kalmanNGLocationMarkerBitmapDescriptor;
    ArrayList<Marker> malMarkers = new ArrayList<>();
    final Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_this_activity);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            uid = user.getUid();
        }
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("Users").child(uid);

        final Intent locationService = new Intent(this.getApplication(), LocationService.class);
        this.getApplication().startService(locationService);
        this.getApplication().bindService(locationService, serviceConnection, Context.BIND_AUTO_CREATE);


        mapView = (MapView) this.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                //map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                map.getUiSettings().setZoomControlsEnabled(false);
                if (ActivityCompat.checkSelfPermission(Trackthis.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Trackthis.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                }
                map.setMyLocationEnabled(false);
                map.getUiSettings().setCompassEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);

                map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                    @Override
                    public void onCameraMoveStarted(int reason) {
                        if(reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                            Log.d(TAG, "onCameraMoveStarted after user's zoom action");

                            zoomable = false;
                            if (zoomBlockingTimer != null) {
                                zoomBlockingTimer.cancel();
                            }

                            handlerOnUIThread = new Handler();

                            TimerTask task = new TimerTask() {
                                @Override
                                public void run() {
                                    handlerOnUIThread.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            zoomBlockingTimer = null;
                                            zoomable = true;

                                        }
                                    });
                                }
                            };
                            zoomBlockingTimer = new Timer();
                            zoomBlockingTimer.schedule(task, 10 * 1000);
                            Log.d(TAG, "start blocking auto zoom for 10 seconds");
                        }
                    }
                });


                drawLocationAccuracyCircle(flocation);
                drawUserPositionMarker(flocation);
                zoomMapTo(flocation);
            }
        });




        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Location newLocation = intent.getParcelableExtra("location");

                drawLocationAccuracyCircle(newLocation);
                drawUserPositionMarker(newLocation);

                if (Trackthis.this.locationService.isLogging) {
                    addPolyline();
                }
                zoomMapTo(newLocation);

                /* Filter Visualization */
                drawMalLocations();

            }
        };

        predictedLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Location predictedLocation = intent.getParcelableExtra("location");

                drawPredictionRange(predictedLocation);

            }
        };



        LocalBroadcastManager.getInstance(this).registerReceiver(
                locationUpdateReceiver,
                new IntentFilter("LocationUpdated"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                predictedLocationReceiver,
                new IntentFilter("PredictLocation"));



        startButton = (ImageButton) this.findViewById(R.id.start_button);
        stopButton = (ImageButton) this.findViewById(R.id.stop_button);
        stopButton.setVisibility(View.INVISIBLE);
        startButton.setVisibility(View.INVISIBLE);
       /* startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(View.INVISIBLE);
                stopButton.setVisibility(View.VISIBLE);

                clearPolyline();
                clearMalMarkers();

                Trackthis.this.locationService.startLogging();

            }
        });




        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.INVISIBLE);

                Trackthis.this.locationService.stopLogging();
            }
        });
*/
/*
        oldLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.old_location_marker);
        noAccuracyLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.no_accuracy_location_marker);
        inaccurateLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.inaccurate_location_marker);
        kalmanNGLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.kalman_ng_location_marker);
*/
        oldLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.transparent_image);
        noAccuracyLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.transparent_image);
        inaccurateLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.transparent_image);
        kalmanNGLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.transparent_image);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        flocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            String name = className.getClassName();

            if (name.endsWith("LocationService")) {
                locationService = ((LocationService.LocationServiceBinder) service).getService();

                locationService.startUpdatingLocation();


            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            if (className.getClassName().equals("LocationService")) {
                locationService.stopUpdatingLocation();
                locationService = null;
            }
        }
    };



    @Override
    public void onPause() {
        super.onPause();

        if (this.mapView != null) {
            this.mapView.onPause();
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        if (this.mapView != null) {
            this.mapView.onResume();
        }

    }


    @Override
    public void onDestroy() {

        if (this.mapView != null) {
            this.mapView.onDestroy();
        }
       /* try {
            if (locationUpdateReceiver != null) {
                unregisterReceiver(locationUpdateReceiver);
            }

            if (predictedLocationReceiver != null) {
                unregisterReceiver(predictedLocationReceiver);
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
*/
        super.onDestroy();

    }

    @Override
    public void onStart() {
        super.onStart();

        if (this.mapView != null) {
            this.mapView.onStart();
        }

    }

    @Override
    public void onStop() {
        super.onStop();

        if (this.mapView != null) {
            this.mapView.onStop();
        }


    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (this.mapView != null) {
            this.mapView.onLowMemory();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (this.mapView != null) {
            this.mapView.onSaveInstanceState(outState);
        }
    }



    private void zoomMapTo(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (this.didInitialZoom == false) {
            try {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                this.didInitialZoom = true;
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Toast.makeText(this.getActivity(), "Inital zoom in process", Toast.LENGTH_LONG).show();
        }

        if (zoomable) {
            try {
                zoomable = false;
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng),
                        new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                zoomable = true;
                            }

                            @Override
                            public void onCancel() {
                                zoomable = true;
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void drawUserPositionMarker(Location location){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if(this.userPositionMarkerBitmapDescriptor == null){
            userPositionMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.user_position_point);
        }

        if (userPositionMarker == null) {
            userPositionMarker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .icon(this.userPositionMarkerBitmapDescriptor));
        } else {
            userPositionMarker.setPosition(latLng);
        }

        databaseReference.child("location").setValue(location.getLatitude()+","+location.getLongitude());
    }


    private void drawLocationAccuracyCircle(Location location){
        if(location.getAccuracy() < 0){
            return;
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (this.locationAccuracyCircle == null) {
            this.locationAccuracyCircle = map.addCircle(new CircleOptions()
                    .center(latLng)
                    .fillColor(Color.argb(64, 0, 0, 0))
                    .strokeColor(Color.argb(64, 0, 0, 0))
                    .strokeWidth(0.0f)
                    .radius(location.getAccuracy())); //set readius to horizonal accuracy in meter.
        } else {
            this.locationAccuracyCircle.setCenter(latLng);
        }
    }


    private void addPolyline() {
        ArrayList<Location> locationList = locationService.locationList;

        if (runningPathPolyline == null) {
            if (locationList.size() > 1){
                Location fromLocation = locationList.get(locationList.size() - 2);
                Location toLocation = locationList.get(locationList.size() - 1);

                LatLng from = new LatLng(((fromLocation.getLatitude())),
                        ((fromLocation.getLongitude())));

                LatLng to = new LatLng(((toLocation.getLatitude())),
                        ((toLocation.getLongitude())));

                this.runningPathPolyline = map.addPolyline(new PolylineOptions()
                        .add(from, to)
                        .width(polylineWidth).color(Color.parseColor("#801B60FE")).geodesic(true));
            }
        } else {
            Location toLocation = locationList.get(locationList.size() - 1);
            LatLng to = new LatLng(((toLocation.getLatitude())),
                    ((toLocation.getLongitude())));

            List<LatLng> points = runningPathPolyline.getPoints();
            points.add(to);

            runningPathPolyline.setPoints(points);
        }
    }

    private void clearPolyline() {
        if (runningPathPolyline != null) {
            runningPathPolyline.remove();
            runningPathPolyline = null;
        }
    }


    /* Filter Visualization */
    private void drawMalLocations(){

        drawMalMarkers(locationService.oldLocationList, oldLocationMarkerBitmapDescriptor);
        drawMalMarkers(locationService.noAccuracyLocationList, noAccuracyLocationMarkerBitmapDescriptor);
        drawMalMarkers(locationService.inaccurateLocationList, inaccurateLocationMarkerBitmapDescriptor);
        drawMalMarkers(locationService.kalmanNGLocationList, kalmanNGLocationMarkerBitmapDescriptor);
    }

    private void drawMalMarkers(ArrayList<Location> locationList, BitmapDescriptor descriptor){
        for(Location location : locationList){
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            Marker marker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .icon(descriptor));

            malMarkers.add(marker);
        }
    }

    private void drawPredictionRange(Location location){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (this.predictionRange == null) {
            this.predictionRange = map.addCircle(new CircleOptions()
                    .center(latLng)
                    .fillColor(Color.argb(50, 30, 207, 0))
                    .strokeColor(Color.argb(128, 30, 207, 0))
                    .strokeWidth(1.0f)
                    .radius(30)); //30 meters of the prediction range
        } else {
            this.predictionRange.setCenter(latLng);
        }

        this.predictionRange.setVisible(true);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Trackthis.this.predictionRange.setVisible(false);
            }
        }, 2000);
    }

    public void clearMalMarkers(){
        for (Marker marker : malMarkers){
            marker.remove();
        }
    }



}


