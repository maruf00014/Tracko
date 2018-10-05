package com.example.maruf.tracko;



import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.Manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
implements NavigationView.OnNavigationItemSelectedListener {

    private TextView navEmailTextView,navNameTextView;
    private String uid;
    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;
    ArrayList<ListItem> items;
    ListAdapter listAdapter;
    ListView listView;
    String name,location;
    private static final int REQUEST_CODE_PERMISSION =2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        navEmailTextView =  headerView.findViewById(R.id.navEmailTextview);
        navNameTextView = headerView.findViewById(R.id.navNameTextView);



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              addDevice();

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);



        firebaseAuth = FirebaseAuth.getInstance();
        if(getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("logedin",false) && getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("dialog",true)) {

            (firebaseAuth.signInWithEmailAndPassword(getSharedPreferences("PREFERENCE",
            MODE_PRIVATE).getString("email", ""), getSharedPreferences("PREFERENCE",
            MODE_PRIVATE).getString("pass", "")))
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    if (task.isSuccessful()) {

                        // Toast.makeText(MainActivity.this, FirebaseAuth.getInstance().getCurrentUser().getUid(), Toast.LENGTH_LONG).show();

                    } else {
                        Log.e("ERROR", task.getException().toString());
                        Toast.makeText(MainActivity.this, "Failed to login!", Toast.LENGTH_LONG).show();

                    }
                }
            });
        }



        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user!=null) {

            String email = user.getEmail();
            uid = user.getUid();
            navEmailTextView.setText(email);

            databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("Users").child(uid).child("Name").addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String name = dataSnapshot.getValue(String.class);
                    navNameTextView.setText(name);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    throw databaseError.toException();
                }
            });
        }
//requesting permission

        try{
            if(ActivityCompat.checkSelfPermission(this,mPermission)!= MockPackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{mPermission},REQUEST_CODE_PERMISSION);
            }
        }catch (Exception e){
            e.printStackTrace();
        }



         //Main
        items = new ArrayList<>();

        listAdapter = new ListAdapter(this,items);
        listView = findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long ld) {
                Intent i = new Intent(MainActivity.this,TrackOtherActivity.class);
                ListItem selectedItem = (ListItem) listView.getItemAtPosition(position);
                i.putExtra("id",selectedItem.getmId());
                startActivity(i);
            }
        });



        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        databaseReference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                items.clear();

                for (DataSnapshot deviceList: dataSnapshot.child(uid).child("Device List").getChildren()) {

                    String id = deviceList.getValue(String.class);
                    //Log.e("ID",id);
                    //Toast.makeText(MainActivity.this, id, Toast.LENGTH_LONG).show();
                    name = dataSnapshot.child(id).child("Name").getValue(String.class);
                    location = dataSnapshot.child(id).child("Location").getValue(String.class);
                    items.add(new ListItem(name,location,id));

                }
                listView.setAdapter(listAdapter);

               // Toast.makeText(MainActivity.this, "Finish", Toast.LENGTH_LONG).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                throw databaseError.toException();
            }
        });
        if(getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("dialog",true)) {
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dialog_layout);

            LinearLayout trackThis = dialog.findViewById(R.id.trackThis);
            trackThis.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                            .edit()
                            .putBoolean("dialog", false)
                            .apply();
                    dialog.dismiss();

                    Intent i = new Intent(MainActivity.this, TrackThisActivity.class);
                    startActivity(i);


                }
            });

            LinearLayout trackOther = dialog.findViewById(R.id.trackOther);
            trackOther.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();


        }












    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
            AlertDialog.Builder alertdialog = new AlertDialog.Builder(this);

            alertdialog.setTitle("Quit?");
            alertdialog.setMessage("Are you sure you Want to quit?");
            alertdialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    moveTaskToBack(true);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);

                }
            });

            alertdialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });


            AlertDialog alert = alertdialog.create();
            alertdialog.show();

        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_track_this) {
            Intent i = new Intent(MainActivity.this, TrackThisActivity.class);
            startActivity(i);

        } else if (id == R.id.nav_edit_profile) {

        } else if (id == R.id.nav_add_device) {

        } else if (id == R.id.nav_remove_device) {

        } else if (id == R.id.nav_remove_all) {

        } else if (id == R.id.nav_log_out) {

            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("logedin", false)
                    .putString("email","")
                    .putString("pass","")
                    .apply();
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(i);


        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .edit()
                .putBoolean("dialog", true)
                .apply();


    }

    private void addDevice(){

        final Dialog adddialog = new Dialog(MainActivity.this);
        adddialog.setContentView(R.layout.add_device_dialog_layout);
        final EditText emailEt = adddialog.findViewById(R.id.dialog_emailEditText);
        final EditText passEt = adddialog.findViewById(R.id.dialog_PassEditText);
        Button addButton = adddialog.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (emailEt.getText().toString().matches("") || passEt.getText().toString().matches("")) {
                    Toast.makeText(MainActivity.this, "Please enter Email and Password!", Toast.LENGTH_SHORT).show();

                } else {

                    adddialog.dismiss();
                    final ProgressDialog progressDialog = ProgressDialog.show(MainActivity.this, "Please wait...", "Adding device...", true);
                    FirebaseAuth dialogfirebaseAuth = FirebaseAuth.getInstance();
                    (dialogfirebaseAuth.signInWithEmailAndPassword(emailEt.getText().toString(), passEt.getText().toString()))
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {


                                    if (task.isSuccessful()) {

                                        FirebaseUser dialoguser = FirebaseAuth.getInstance().getCurrentUser();
                                        String dialoguid = dialoguser.getUid();

                                        firebaseAuth.signInWithEmailAndPassword(getSharedPreferences("PREFERENCE",
                                                MODE_PRIVATE).getString("email", ""), getSharedPreferences("PREFERENCE",
                                                MODE_PRIVATE).getString("pass", ""));

                                        FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("Device List").child(dialoguid).setValue(dialoguid);

                                        progressDialog.dismiss();
                                        Toast.makeText(MainActivity.this, "Successfully device added!", Toast.LENGTH_LONG).show();



                                    } else {
                                        Log.e("ERROR", task.getException().toString());
                                        progressDialog.dismiss();
                                        Toast.makeText(MainActivity.this, "Invalid email or password!", Toast.LENGTH_LONG).show();

                                    }
                                }
                            });
                }

            }

        });

        Button canelButton = adddialog.findViewById(R.id.cancel_button);
        canelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adddialog.dismiss();
            }
        });

        adddialog.show();

    }

}
