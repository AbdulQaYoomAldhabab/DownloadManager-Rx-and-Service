package com.asadeq.rxdownloadmanager.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import com.asadeq.rxdownloadmanager.R;
import com.muddzdev.styleabletoast.StyleableToast;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;


public class Utils {
    public static String TAG = "Utils : ";

    public static Utils utils;
    public static Utils getInstance(){
        if (utils == null ) {
            utils = new Utils();
            permitThreadPolicy();
        }
        return utils;
    }

    public final static boolean isEmpty(String string) {
        if (TextUtils.isEmpty(string) || string.equalsIgnoreCase(null)
                || string.equalsIgnoreCase("")) {
            return true;
        }
        return false;
    }
    public Intent enableUnknownAppSourcesIntent(Context mContext) {
        return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                , Uri.parse("package:" + mContext.getPackageName()));
    }

    public boolean isOnline() {
        try {
            int port = 80;
            int timeOut = 2000;
            String host =  "www.google.com";
            new Socket().connect(new InetSocketAddress(host, port), timeOut);
            return true;
        } catch (Exception e) {
            // Either we have a timeout or unreachable host or failed DNS lookup
            System.out.println(e);
            return false;
        }
    }
    private static void permitThreadPolicy() {
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        } catch (Exception e) {}
    }

    public void log(String tag, String msg) {
        //if (AppConstants.ENABLE_DEBUG_MODE == true) {
            Log.d(tag, msg);
        //}
    }
    public void showToast(Context mContext, String msg) {
            new StyleableToast.Builder(mContext)
                    .backgroundColor(Color.parseColor("#E68E24AA"))
                    .textColor(Color.WHITE)
                    .text(msg)
                    .show();
    }

    public void showAlertDialog(final Context mContext,String title, String Confirm, String cancel
            , DialogInterface.OnClickListener confirmListener , DialogInterface.OnClickListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.Theme_AppCompat_Light_Dialog_Alert);
        builder.setMessage(title)
                .setCancelable(false)
                .setNegativeButton(cancel, cancelListener)
                .setPositiveButton(Confirm, confirmListener);
        AlertDialog alert = builder.create();
        alert.show();
    }
}
