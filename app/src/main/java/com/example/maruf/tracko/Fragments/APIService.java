package com.example.maruf.tracko.Fragments;

import com.example.maruf.tracko.Notifications.MyResponse;
import com.example.maruf.tracko.Notifications.Sender;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAx2UQsqQ:APA91bHPqn6tyszrpyoiw2wdnmDSwkGFcoO63ZhAe5_TC73b2iKYjIZeu4hb1VOiW32eVm6mB-qTlycbSNXFXqas9uS2TItrFq7QuN5bMA-EXoAwj_glxawb9j_miOgwwi0U02FJS369"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
