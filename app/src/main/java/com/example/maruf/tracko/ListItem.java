package com.example.maruf.tracko;

import java.io.Serializable;

public class ListItem implements Serializable {
    private String mName;
    private String mLocation;
    private String mId;


    public ListItem(String name,String location,String id){

        mName = name;
        mLocation = location;
        mId = id;
    }

    public String getmName(){return mName;}
    public String getmLocation(){return mLocation;}
    public String getmId(){return mId;}


}
