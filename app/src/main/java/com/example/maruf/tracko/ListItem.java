package com.example.maruf.tracko;

import java.io.Serializable;

public class ListItem implements Serializable {
    private String mName;
    private String mLocation;
    private String mId;
    private String mImageURL;
    private String msharing,memail;


    public ListItem(String name,String location,String id,String imageURL,String sharing,String email){

        mName = name;
        mLocation = location;
        mId = id;
        mImageURL = imageURL;
        msharing = sharing;
        memail =email;
    }

    public String getmName(){return mName;}
    public String getmLocation(){return mLocation;}
    public String getmId(){return mId;}
    public String getmImageURL(){return mImageURL;}
    public String getmsharing(){return msharing;}
    public String getMemail(){return memail;}

}
