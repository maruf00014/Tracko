package com.example.maruf.tracko;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.maruf.tracko.Fragments.APIService;
import com.example.maruf.tracko.Notifications.Client;
import com.example.maruf.tracko.Notifications.Data;
import com.example.maruf.tracko.Notifications.MyResponse;
import com.example.maruf.tracko.Notifications.Sender;
import com.example.maruf.tracko.Notifications.Token;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HelpAlert extends AppCompatActivity {

    ArrayList<ListItem> items;
    RemoveListAdapter removeListAdapter;
    ListView listView;
    String uid,userName;
    APIService apiService;
    private DatabaseReference databaseReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_device);

        Toolbar toolbar =  findViewById(R.id.removetoolbar);
        setSupportActionBar(toolbar);

        TextView text=new TextView(this);
        text.setText("Send");
        text.setTextSize(20);
        text.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        Toolbar.LayoutParams l2=new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        l2.gravity=Gravity.END;
        l2.setMargins(0, 0, 32, 0);
        text.setLayoutParams(l2);
        toolbar.addView(text);

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user!=null) {

            uid = user.getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("Users").child(uid).addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userName = dataSnapshot.child("username").getValue(String.class);

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    throw databaseError.toException();
                }
            });
        }


        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                for (int k=0; k < items.size(); k++) {
                    ListItem i = items.get(k);
                    sendNotifiaction(i.getmId(),userName);

                }
                Toast.makeText(HelpAlert.this, "Send!", Toast.LENGTH_SHORT).show();

            }
        });




        items = (ArrayList<ListItem>) getIntent().getSerializableExtra("items");
        uid = getIntent().getStringExtra("uid");
        removeListAdapter = new RemoveListAdapter(this,items);
        listView = findViewById(R.id.removelistView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long ld) {
                ListItem selectedItem = (ListItem) listView.getItemAtPosition(position);
                items.remove(selectedItem);
                removeListAdapter.notifyDataSetChanged();

            }
        });

        listView.setAdapter(removeListAdapter);

    }

    public void sendNotifiaction(final String receiver, final String username){
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

}
