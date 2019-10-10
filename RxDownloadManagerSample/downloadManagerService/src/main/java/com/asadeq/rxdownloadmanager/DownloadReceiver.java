package com.asadeq.rxdownloadmanager;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;

import static android.content.Context.DOWNLOAD_SERVICE;

public class DownloadReceiver extends BroadcastReceiver {
    int status = -1;
    int reason = -1;
    @Override
    public void onReceive(Context mContext, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            String filePath = null;
            Cursor cursor = null;
            try {
                DownloadManager manager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                cursor = manager.query(query);
                filePath = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                if (cursor.moveToFirst()) {
                    status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                }
                Log.i("DOWNLOAD LISTENER", filePath);
            } catch(Exception e) {
                Log.i("DOWNLOAD Exception ", e.getMessage());
            } finally {
                cursor.close();
            }

            if (filePath != null) {
                Uri uri = Uri.fromFile(new File(filePath));
                if (uri != null && uri.getPath().endsWith(".apk")) {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    install.setDataAndType(uri, "application/vnd.android.package-archive");
                    mContext.startActivity(install);
                }
            }
        }
    }
}
