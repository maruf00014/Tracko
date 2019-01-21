package com.example.maruf.tracko;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignupActivity extends AppCompatActivity {

    private EditText emailEditText2;
    private EditText passEditText2;
    private EditText repassEditText, nameEditText;
    private FirebaseAuth firebaseAuth;
    private Button signUpButton,addPhotoButton;
    private CircleImageView circleImageView;

    StorageReference storageReference;
    DatabaseReference databaseReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;
    String imageURL;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        nameEditText =  findViewById(R.id.nameEditText);
        emailEditText2 =  findViewById(R.id.emailEditText2);
        passEditText2 =  findViewById(R.id.PassEditText2);
        repassEditText =  findViewById(R.id.rePassEditText);
        signUpButton = findViewById(R.id.SignupButton);
        addPhotoButton = findViewById(R.id.add_photo_button);
        circleImageView = findViewById(R.id.signup_profile_image);
        firebaseAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("uploads");



        addPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (TextUtils.isEmpty(nameEditText.getText().toString()) || TextUtils.isEmpty(emailEditText2.getText().toString()) ||
                        TextUtils.isEmpty(passEditText2.getText().toString()) ||
                        TextUtils.isEmpty(repassEditText.getText().toString())) {
                    Toast.makeText(getApplicationContext(), "All field are required!", Toast.LENGTH_SHORT).show();
                    return;
                }else if(!passEditText2.getText().toString().matches(repassEditText.getText().toString())){
                    Toast.makeText(getApplicationContext(), "Password not match!", Toast.LENGTH_SHORT).show();
                    return;
                }else if(repassEditText.getText().toString().length()<6){
                    Toast.makeText(getApplicationContext(), "Password too short!", Toast.LENGTH_SHORT).show();
                    return;
                }else {


                    progressDialog = ProgressDialog.show(SignupActivity.this, "Please wait...", "Processing...", true);
                    (firebaseAuth.createUserWithEmailAndPassword(emailEditText2.getText().toString(), passEditText2.getText().toString()))
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {


                                    if (task.isSuccessful()) {
                                        storeData();

                                    } else {
                                        Log.e("ERROR", task.getException().toString());
                                        progressDialog.dismiss();
                                        Toast.makeText(SignupActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }
            }
        });


    }

    @Override
    public void onBackPressed() {

        Intent intent = new Intent(SignupActivity.this,LoginActivity.class);
        startActivity(intent);
        finish();
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

    private void storeData(){
        final String userID = firebaseAuth.getCurrentUser().getUid();
      final  DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userID);
        imageURL = "default";
        if (imageUri != null){
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
                        imageURL = downloadUri.toString();
                        Map addUser = new HashMap();
                        addUser.put("id", userID);
                        addUser.put("username",nameEditText.getText().toString());
                        addUser.put("imageURL", imageURL);
                        addUser.put("status", "offline");
                        addUser.put("search", nameEditText.getText().toString().toLowerCase());
                        addUser.put("location", "0,0");
                        addUser.put("sharing", "no");
                        addUser.put("email", emailEditText2.getText());
                        databaseReference.setValue(addUser);
                        progressDialog.dismiss();
                        Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_LONG).show();
                        Intent i = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(i);

                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to upload profile photo!", Toast.LENGTH_LONG).show();
                        Map addUser = new HashMap();
                        addUser.put("id", userID);
                        addUser.put("username",nameEditText.getText().toString());
                        addUser.put("imageURL", "default");
                        addUser.put("status", "offline");
                        addUser.put("search", nameEditText.getText().toString().toLowerCase());
                        addUser.put("location", "0,0");
                        addUser.put("sharing", "no");
                        addUser.put("email", emailEditText2.getText());
                        databaseReference.setValue(addUser);
                        progressDialog.dismiss();
                        Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_LONG).show();
                        Intent i = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(i);
                    }

                }
            });
        }

        else {
            Map addUser = new HashMap();
            addUser.put("Name",nameEditText.getText().toString());
            addUser.put("imageURL", "Default");
            addUser.put("location", "0,0");
            addUser.put("sharing", "no");
            addUser.put("email", emailEditText2.getText());
            databaseReference.setValue(addUser);
            progressDialog.dismiss();
            Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_LONG).show();
            Intent i = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(i);

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null){
            imageUri = data.getData();
            circleImageView.setImageURI(null);
            circleImageView.setImageURI(imageUri);

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
}
