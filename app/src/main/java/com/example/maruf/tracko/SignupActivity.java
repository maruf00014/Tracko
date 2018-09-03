package com.example.maruf.tracko;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    private EditText emailEditText2;
    private EditText passEditText2;
    private EditText repassEditText;
    private FirebaseAuth firebaseAuth;
    private Button signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        emailEditText2 =  findViewById(R.id.emailEditText2);
        passEditText2 =  findViewById(R.id.PassEditText2);
        repassEditText =  findViewById(R.id.rePassEditText);
        signUpButton = findViewById(R.id.SignupButton);
        firebaseAuth = FirebaseAuth.getInstance();


        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (TextUtils.isEmpty(emailEditText2.getText().toString()) ||
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

                    final ProgressDialog progressDialog = ProgressDialog.show(SignupActivity.this, "Please wait...", "Processing...", true);
                    (firebaseAuth.createUserWithEmailAndPassword(emailEditText2.getText().toString(), passEditText2.getText().toString()))
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressDialog.dismiss();

                                    if (task.isSuccessful()) {
                                        Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_LONG).show();
                                        Intent i = new Intent(SignupActivity.this, LoginActivity.class);
                                        startActivity(i);
                                    } else {
                                        Log.e("ERROR", task.getException().toString());
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
}
