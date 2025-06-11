package com.frank.ffmpeg.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import com.frank.ffmpeg.listener.OnNetworkChangeListener;

public class ConnectionReceiver extends BroadcastReceiver {
    private OnNetworkChangeListener networkChangeListener;

    public ConnectionReceiver(OnNetworkChangeListener networkChangeListener) {
        this.networkChangeListener = networkChangeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable()) {
                    if (networkChangeListener != null) {
                        networkChangeListener.onNetworkChange();
                    }
                }
            }
        }
    }
}