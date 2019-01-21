package com.example.maruf.tracko;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListAdapter extends ArrayAdapter<ListItem> {

    public ListAdapter( Context context, ArrayList<ListItem> items) {
        super(context,0,items);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item, parent, false);
        }

        ListItem currentItem = getItem(position);

        CircleImageView circleImageView = listItemView.findViewById(R.id.list_item_profile_image);

        if (currentItem.getmImageURL().equals("default")){
            circleImageView.setImageResource(R.drawable.profile_sample_image);
        } else {
            Glide.with(getContext()).load(currentItem.getmImageURL()).into(circleImageView);
        }

        TextView name = listItemView.findViewById(R.id.listItemNameTextView);
        name.setText(currentItem.getmName());

        TextView location = listItemView.findViewById(R.id.listItemLocationTextView);
        location.setText(currentItem.getmLocation());

        TextView email = listItemView.findViewById(R.id.listItemEmailTextView);
        email.setText(currentItem.getMemail());

        CircleImageView sharingImage = listItemView.findViewById(R.id.list_item_sharing_image);

        if (currentItem.getmsharing().equals("yes")){
            sharingImage.setBorderColor(getContext().getResources().getColor(R.color.green));
        } else {
            sharingImage.setBorderColor(getContext().getResources().getColor(R.color.red));
        }
        return listItemView;


    }


}
