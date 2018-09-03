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
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class PassResetActivity extends AppCompatActivity {

    private EditText rpEditText;
    private Button passResetButton;
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pass_reset);

        rpEditText = findViewById(R.id.rpEmailEditText);
        passResetButton = findViewById(R.id.passResetButton);
        firebaseAuth = FirebaseAuth.getInstance();

        passResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(rpEditText.getText().toString().matches("")){
                    Toast.makeText(PassResetActivity.this, "Email is required!", Toast.LENGTH_SHORT).show();
                }else{
                    final ProgressDialog progressDialog = ProgressDialog.show(PassResetActivity.this, "Please wait...", "Proccessing...", true);
                    firebaseAuth.sendPasswordResetEmail(rpEditText.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    progressDialog.dismiss();

                                    if (task.isSuccessful()) {
                                        Toast.makeText(PassResetActivity.this, "A reset link is send to your email!", Toast.LENGTH_LONG).show();
                                        Intent i = new Intent(PassResetActivity.this, LoginActivity.class);
                                        startActivity(i);
                                    } else {
                                        Log.e("ERROR", task.getException().toString());
                                        Toast.makeText(getApplicationContext(), "Invalid Email !", Toast.LENGTH_LONG).show();

                                    }



                                }
                            });



                }

            }

        });

    }

}













