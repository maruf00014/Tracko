package com.example.maruf.tracko;



import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.maruf.tracko.Fragments.APIService;
import com.example.maruf.tracko.Notifications.Client;
import com.example.maruf.tracko.Notifications.Data;
import com.example.maruf.tracko.Notifications.MyResponse;
import com.example.maruf.tracko.Notifications.Sender;
import com.example.maruf.tracko.Notifications.Token;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import android.Manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity
implements NavigationView.OnNavigationItemSelectedListener {

    private TextView navEmailTextView,navNameTextView;
    public String uid;
    String userName;
    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;
    ArrayList<ListItem> items;
    ListAdapter listAdapter;
    ListView listView;
    String name,location,TAG;
    CircleImageView profileImage;
    APIService apiService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TAG = "Speech";
        ImageView speechImage = toolbar.findViewById(R.id.speech_image);

        speechImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trackDeviceBySpeech();
            }
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        navEmailTextView =  headerView.findViewById(R.id.navEmailTextview);
        navNameTextView = headerView.findViewById(R.id.navNameTextView);
        profileImage = headerView.findViewById(R.id.profile_image);

       /* if(!isNetworkAvialable(this)) {
            View parentLayout = findViewById(android.R.id.content);
            Snackbar.make(parentLayout, "No Internet Connection!", Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                    .show();
        }*/


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
            databaseReference.child("Users").child(uid).addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userName = dataSnapshot.child("username").getValue(String.class);
                    navNameTextView.setText(userName);
                    String imageURL = dataSnapshot.child("imageURL").getValue(String.class);
                    if (imageURL.equals("default")){
                        profileImage.setImageResource(R.drawable.profile_sample_image);
                    } else {
                        Glide.with(getApplicationContext()).load(imageURL).into(profileImage);
                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    throw databaseError.toException();
                }
            });
        }
//requesting permission

        checkPermission();

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

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

                for (DataSnapshot deviceList: dataSnapshot.child(uid).child("deviceList").getChildren()) {

                    String id = deviceList.getValue(String.class);
                    //Log.e("ID",id);
                    //Toast.makeText(MainActivity.this, id, Toast.LENGTH_LONG).show();
                    name = dataSnapshot.child(id).child("username").getValue(String.class);
                    location = dataSnapshot.child(id).child("location").getValue(String.class);
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
            alertdialog.setMessage("Are you sure you want to quit?");
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
        if (id == R.id.shareDeviceId) {

            Intent intent = new Intent(); intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, uid );
            startActivity(Intent.createChooser(intent, "Share via"));

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
                editProfile();
        } else if (id == R.id.nav_add_device) {

            addDevice();

        } else if (id == R.id.nav_remove_device) {
            Intent intent = new Intent(MainActivity.this,RemoveDevice.class);
            intent.putExtra("items",items);
            intent.putExtra("uid",uid);
            startActivity(intent);

        } else if (id == R.id.nav_remove_all) {
            removeAllDevice();

        } else if (id == R.id.nav_message) {

            Intent intent = new Intent(MainActivity.this,MessageMainActivity.class);
            startActivity(intent);
        }  else if (id == R.id.nav_help_alert) {
                sendHelpAlert();
            Toast.makeText(MainActivity.this, "Send!", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_log_out) {

           logOut();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void sendHelpAlert() {
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(this);

        alertdialog.setTitle("Help Alert!");
        alertdialog.setMessage("Are you sure you want to send help alert?");
        alertdialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").
                        child(uid).child("deviceList");
                databaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            for (DataSnapshot deviceListSnapshot: dataSnapshot.getChildren()) {
                                String receiver = deviceListSnapshot.getValue(String.class);

                                sendNotifiaction(receiver,userName);

                            }


                        }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
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

    private void sendNotifiaction(final String receiver, final String username){
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(uid, R.mipmap.ic_launcher, username+" need help!", "Help Alert!",
                            receiver);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().success != 1){
                                            Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
        final EditText deviceIdEditText = adddialog.findViewById(R.id.dialog_deviceid);
        Button addButton = adddialog.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String deviceID = deviceIdEditText.getText().toString().trim();

                FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("deviceList").child(deviceID).setValue(deviceID);
                adddialog.dismiss();
                Toast.makeText(MainActivity.this, "Successfully device added!", Toast.LENGTH_LONG).show();

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

    private void logOut(){
        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .edit()
                .putBoolean("logedin", false)
                .putString("email","")
                .putString("pass","")
                .apply();
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(i);

    }

    private void removeAllDevice(){
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(this);

        alertdialog.setTitle("Remove All Devices?");
        alertdialog.setMessage("Are you sure you want to remove all devices?");
        alertdialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").
                        child(uid).child("deviceList");
                databaseReference.removeValue();

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

    private void editProfile(){

        final Dialog editdialog = new Dialog(MainActivity.this);
        editdialog.setContentView(R.layout.edit_profile_layout);
        final EditText nameEt = editdialog.findViewById(R.id.edp_dialog_nameEditText);
        Button editButton = editdialog.findViewById(R.id.edp_edit_button);
        nameEt.setText(userName);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (nameEt.getText().toString().matches("")) {
                    Toast.makeText(MainActivity.this, "Please enter Name!", Toast.LENGTH_SHORT).show();

                } else {

                    editdialog.dismiss();
                    final ProgressDialog progressDialog = ProgressDialog.show(MainActivity.this, "Please wait...", "Adding device...", true);

                    databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("username");
                    databaseReference.setValue(nameEt.getText().toString());
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Profile Edit successful!", Toast.LENGTH_LONG).show();


                }
            }

        });

        Button cancelButton = editdialog.findViewById(R.id.edp_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editdialog.dismiss();
            }
        });

        editdialog.show();
    }

    private void trackDeviceBySpeech(){

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                "com.example.maruf.tracko");

        SpeechRecognizer recognizer = SpeechRecognizer
                .createSpeechRecognizer(this.getApplicationContext());
        RecognitionListener listener = new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> voiceResults = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (voiceResults == null) {
                    Toast.makeText(MainActivity.this, "Please say again!", Toast.LENGTH_SHORT).show();
                } else {
                    String result = voiceResults.get(0);
                    String name[] = result.split(" ");
                    String finalResult ="";
                    for(int i=1;i<name.length;i++)
                    {
                        finalResult = finalResult + name[i]+" ";
                    }
                   finalResult = finalResult.trim();
                    boolean deviceFound = false;
                    for(ListItem item : items) {
                        if(item.getmName().equalsIgnoreCase(finalResult)) {
                            deviceFound = true;
                            Intent i = new Intent(MainActivity.this,TrackOtherActivity.class);
                            i.putExtra("id",item.getmId());
                            startActivity(i);
                            break;
                        }
                    }
                    if(!deviceFound)Toast.makeText(MainActivity.this, "Device not found!", Toast.LENGTH_SHORT).show();

                }
            }
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
            }

            @Override
            public void onError(int error) {
                Log.d(TAG,
                        "Error listening for speech: " + error);
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech starting");
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onEndOfSpeech() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // TODO Auto-generated method stub

            }
        };
        recognizer.setRecognitionListener(listener);
        recognizer.startListening(intent);


        }
    private void checkPermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);

        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    2);

        }

    }

    public boolean isNetworkAvialable (Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null)
        {
            NetworkInfo netInfos = connectivityManager.getActiveNetworkInfo();
            if(netInfos != null)
                if(netInfos.isConnected())
                    return true;
        }
        return false;
    }

}
