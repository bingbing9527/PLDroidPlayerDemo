package me.qbb.player.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


/**
 * Created by 太极 on 2016/12/30.
 */

public class NetUtilss {

    public static int isNetConnected(Context context) {
        int downCode = 0;//0：不下载。1：下载。2：提示窗
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    downCode = 1;
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    downCode = 2;
                }
            } else {
                downCode = 0;
            }
        }
        return downCode;
    }

}
