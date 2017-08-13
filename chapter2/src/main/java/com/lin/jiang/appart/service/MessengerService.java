package com.lin.jiang.appart.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.lin.jiang.appart.utils.Constants;

public class MessengerService extends Service {

    private static final String TAG = "TEST:MessengerService";
    private final Messenger mMessenger = new Messenger(new MessengerHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    public static class MessengerHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_FROM_CLIENT:
                    Log.d(TAG, "handleMessage: receive msg from Client: " + msg.getData().getString("msg"));
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
}
