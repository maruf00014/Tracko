package com.example.maruf.tracko;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class RemoveListAdapter extends ArrayAdapter<ListItem> {

    public RemoveListAdapter(Context context, ArrayList<ListItem> items) {
        super(context,0,items);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.remove_list_item, parent, false);
        }

        ListItem currentItem = getItem(position);

        TextView name = listItemView.findViewById(R.id.removelistItemNameTextView);
        name.setText(currentItem.getmName());

        TextView location = listItemView.findViewById(R.id.removelistItemLocationTextView);
        location.setText(currentItem.getmLocation());

        return listItemView;


    }


}
