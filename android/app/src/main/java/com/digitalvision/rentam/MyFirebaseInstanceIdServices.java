package com.digitalvision.rentam;


import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseInstanceIdServices extends FirebaseMessagingService {


    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("NEW_TOKEN",token);
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

    }
}
