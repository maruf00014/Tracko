package com.example.maruf.tracko;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private Button signUpButton;
    private Button logInButton;
    private EditText emailEditText;
    private EditText passEditText;
    private TextView forgetPassTextView;
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        logInButton = findViewById(R.id.loginButton);
        emailEditText = findViewById(R.id.emailEditText);
        passEditText = findViewById(R.id.PassEditText);
        firebaseAuth = FirebaseAuth.getInstance();

        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (emailEditText.getText().toString().matches("") || passEditText.getText().toString().matches("")) {
                    Toast.makeText(LoginActivity.this, "Please enter Email and Password!", Toast.LENGTH_SHORT).show();

                } else{
                    final ProgressDialog progressDialog = ProgressDialog.show(LoginActivity.this, "Please wait...", "Proccessing...", true);
                    (firebaseAuth.signInWithEmailAndPassword(emailEditText.getText().toString(), passEditText.getText().toString()))
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressDialog.dismiss();

                                    if (task.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_LONG).show();
                                        Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                        i.putExtra("Email", firebaseAuth.getCurrentUser().getEmail());
                                        startActivity(i);
                                    } else {
                                        Log.e("ERROR", task.getException().toString());
                                        Toast.makeText(LoginActivity.this, "Invalid Email or Password!", Toast.LENGTH_LONG).show();

                                    }
                                }
                            });
                    }
            }
        });








        signUpButton = findViewById(R.id.SignupButton);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent gotoSignUp = new Intent(LoginActivity.this,SignupActivity.class);
                startActivity(gotoSignUp);
                finish();
            }
        });

        forgetPassTextView = findViewById(R.id.forgetPassTextView);
        forgetPassTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this,PassResetActivity.class));
            }
        });



    }
}
