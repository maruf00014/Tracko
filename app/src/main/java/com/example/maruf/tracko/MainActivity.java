package com.example.maruf.tracko;



import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import android.webkit.MimeTypeMap;
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
import com.google.android.gms.tasks.Continuation;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import android.Manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity
implements NavigationView.OnNavigationItemSelectedListener {

    private TextView navEmailTextView,navNameTextView,navIDTextView;
    public String uid;
    String userName;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    ArrayList<ListItem> items;
    ListAdapter listAdapter;
    ListView listView;
    String name,location,imageURL,userImageURL,editImageURL,TAG;
    CircleImageView profileImage,editProfileImage;
    APIService apiService;
    ProgressDialog progressDialog;
    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        TAG = "Speech";
        ImageView speechImage = toolbar.findViewById(R.id.speech_image);

        speechImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Toast.makeText(MainActivity.this, FirebaseAuth.getInstance().getCurrentUser().getUid(), Toast.LENGTH_LONG).show();

                trackDeviceBySpeech();
            }
        });
       progressDialog = ProgressDialog.show(MainActivity.this, "", "Loading...", true);

        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        navEmailTextView =  headerView.findViewById(R.id.navEmailTextview);
        navNameTextView = headerView.findViewById(R.id.navNameTextView);
        navIDTextView = headerView.findViewById(R.id.navIDTextview);
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
            navIDTextView.setText(uid);
            databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("Users").child(uid).addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userName = dataSnapshot.child("username").getValue(String.class);
                    navNameTextView.setText(userName);
                    userImageURL = dataSnapshot.child("imageURL").getValue(String.class);
                    if (userImageURL.equals("default")){
                        profileImage.setImageResource(R.drawable.profile_sample_image);
                    } else {
                        Glide.with(getApplicationContext()).load(userImageURL).into(profileImage);
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
                    imageURL = dataSnapshot.child(id).child("imageURL").getValue(String.class);
                   String sharing = dataSnapshot.child(id).child("sharing").getValue(String.class);
                   String email = dataSnapshot.child(id).child("email").getValue(String.class);
                    items.add(new ListItem(name,location,id,imageURL,sharing,email));

                }
                listView.setAdapter(listAdapter);
                progressDialog.dismiss();
               // Toast.makeText(MainActivity.this, "Finish", Toast.LENGTH_LONG).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                throw databaseError.toException();
            }
        });
        /*if(getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("dialog",true)) {
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


        }*/












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
            Intent i = new Intent(MainActivity.this, Trackthis.class);
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
            Intent intent = new Intent(MainActivity.this,HelpAlert.class);
            intent.putExtra("items",items);
            intent.putExtra("uid",uid);
            startActivity(intent);
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

                                sendNotifiaction(receiver,userName,"help");

                            }


                        }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                Toast.makeText(MainActivity.this, "Send!", Toast.LENGTH_SHORT).show();


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

    public void sendNotifiaction(final String receiver, final String username, final String type){
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Token token = snapshot.getValue(Token.class);
                    Data data;
                    if(type.equals("help")){
                        data = new Data(uid, R.mipmap.ic_launcher, username+" need help!", "Help Alert!",
                                receiver);
                    } else{
                        data = new Data(uid, R.mipmap.ic_launcher, username+" added you!", "Adding Alert!",
                                receiver);
                    }


                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().success != 1){
                                           // Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
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
    protected void onPause(){
        sharing("no");
        super.onPause();



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
                if(!deviceID.equals("") && deviceID.length() == 28) {
                    FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("deviceList").child(deviceID).setValue(deviceID);
                    adddialog.dismiss();
                    sendNotifiaction(deviceID,userName,"add");
                    Toast.makeText(MainActivity.this, "Successfully send!", Toast.LENGTH_LONG).show();
                }
                else{
                    adddialog.dismiss();
                    Toast.makeText(MainActivity.this, "Please enter a valid ID!", Toast.LENGTH_LONG).show();

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

    private void logOut(){
        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .edit()
                .putBoolean("logedin", false)
                .putString("email","")
                .putString("pass","")
                .apply();
         sharing("no");
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
        editProfileImage = editdialog.findViewById(R.id.edit_profile_image);
        if (userImageURL.equals("default")){
            editProfileImage.setImageResource(R.drawable.profile_sample_image);
        } else {
            Glide.with(getApplicationContext()).load(userImageURL).into(editProfileImage);
        }

        editProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (nameEt.getText().toString().matches("")) {
                    Toast.makeText(MainActivity.this, "Please enter Name!", Toast.LENGTH_SHORT).show();

                } else {

                    editdialog.dismiss();
                    storeData(nameEt.getText().toString());


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
    public void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }
    public String getFileExtension(Uri uri){
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void storeData(final String editedName){
      final ProgressDialog  dataprogressDialog = ProgressDialog.show(MainActivity.this, "Please wait...", "Profile updating...", true);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

        editImageURL = "default";
        if (imageUri != null){
            storageReference = FirebaseStorage.getInstance().getReference("uploads");

            final  StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    +"."+getFileExtension(imageUri));

            uploadTask = fileReference.putFile(compressImage(imageUri));
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw  task.getException();
                    }

                    return  fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()){
                        Uri downloadUri = task.getResult();
                        editImageURL = downloadUri.toString();

                        databaseReference.child("username").setValue(editedName);
                        databaseReference.child("imageURL").setValue(editImageURL);
                        dataprogressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Profile Edit successful!", Toast.LENGTH_LONG).show();


                    } else {
                        Toast.makeText(MainActivity.this, "Failed to upload profile photo!", Toast.LENGTH_LONG).show();
                        dataprogressDialog.dismiss();

                    }

                }
            });
        }

        else {

            databaseReference.child("username").setValue(editedName);
            dataprogressDialog.dismiss();
            Toast.makeText(MainActivity.this, "Profile Edit successful!", Toast.LENGTH_LONG).show();



        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null){
            imageUri = data.getData();
            editProfileImage.setImageURI(null);
            editProfileImage.setImageURI(imageUri);

        }
    }


    public Uri compressImage(Uri imageUri) {

        String filePath = getRealPathFromURI(imageUri);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

        //      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
        //      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        //      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 200.0f;
        float maxWidth = 200.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        //      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

        //      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

        //      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

        //      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
            //          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        //      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        String filename = getFilename();
        try {
            out = new FileOutputStream(filename);

            //          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(new File(filename));

    }

    public String getFilename() {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "MyFolder/Images");
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        return uriSting;

    }

    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
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

    private void sharing(String status){
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        databaseReference.child("sharing").setValue(status);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharing("yes");
    }


}

