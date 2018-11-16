package com.example.maruf.tracko;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class RemoveDevice extends AppCompatActivity {

    ArrayList<ListItem> items;
    RemoveListAdapter removeListAdapter;
    ListView listView;
    private DatabaseReference databaseReference;
    String uid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_device);
        Toolbar toolbar =  findViewById(R.id.removetoolbar);
        setSupportActionBar(toolbar);



        items = (ArrayList<ListItem>) getIntent().getSerializableExtra("items");
        uid = getIntent().getStringExtra("uid");
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("Device List");
        removeListAdapter = new RemoveListAdapter(this,items);
        listView = findViewById(R.id.removelistView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long ld) {
                ListItem selectedItem = (ListItem) listView.getItemAtPosition(position);
                items.remove(selectedItem);
                removeListAdapter.notifyDataSetChanged();
                databaseReference.child(selectedItem.getmId()).removeValue();

            }
        });

        listView.setAdapter(removeListAdapter);

    }
}
