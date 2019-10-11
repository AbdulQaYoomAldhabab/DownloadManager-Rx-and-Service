package com.asadeq.rxdownloadmanager;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;

public class DownloadManagerService extends IntentService {

    private static final String DOWNLOAD_TITLE   = "Download_title";
    private static final String DOWNLOAD_PATH    = "Download_path";
    private static final String DESTINATION_PATH = "Destination_path";
    private String downloadTitle    = null;
    private String downloadPath     = null;
    private String destinationPath  = null;
    private String fileName  = null;
    private String fileType  = null;
    private long downloadID  = 0;
    private DownloadManager downloadManager;
    private DirectoryHelper directoryHelper;

    public DownloadManagerService() {
        super("DownloadSongService");
        directoryHelper = DirectoryHelper.getInstance(this);
        directoryHelper.createFolderDirectories();
    }

    private static Activity mActivity = null;
    public static Intent getDownloadService(@NonNull Activity mContext, @NonNull String downloadPath
            , @NonNull String destinationPath, @NonNull String title) {
        mActivity = mContext;
        return new Intent(mContext, DownloadManagerService.class)
                .putExtra(DOWNLOAD_PATH, downloadPath)
                .putExtra(DESTINATION_PATH, destinationPath)
                .putExtra(DOWNLOAD_TITLE, title);
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        downloadTitle   = intent.getStringExtra(DOWNLOAD_TITLE);
        downloadPath    = intent.getStringExtra(DOWNLOAD_PATH);
        destinationPath = intent.getStringExtra(DESTINATION_PATH);
        fileName        = Uri.parse(downloadPath).getLastPathSegment();
        fileType        = fileName.substring(fileName.lastIndexOf("."));

        directoryHelper.removeDuplicateFileIfExist(fileName);
        startDownload(downloadPath, destinationPath, downloadTitle);
    }

    private void startDownload(String downloadPath, String destinationPath, String title) {
        Uri uri = Uri.parse(downloadPath);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        // Tell on which network you want to download file.
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                | DownloadManager.Request.NETWORK_WIFI);
        // This will show notification on top when downloading the file.
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(true);
        // Title for notification.
        request.setTitle(title);
        // Storage directory path
        request.setDestinationInExternalPublicDir(destinationPath, uri.getLastPathSegment());
        // This will start downloading
        downloadManager = getDownloadManager();
        downloadID = getDownloadManager().enqueue(request);

        //register download receiver
        //IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        //registerReceiver(new DownloadReceiver(), new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
    @NonNull
    private DownloadManager getDownloadManager() {
        if (downloadManager == null) {
            downloadManager = (DownloadManager) getApplicationContext()
                    .getSystemService(Context.DOWNLOAD_SERVICE);
        }
        if (downloadManager == null) {
            throw new RuntimeException("Can't get DownloadManager from system service");
        }
        return downloadManager;
    }
    public class DownloadReceiverListener{

    }
    public class DownloadReceiver extends BroadcastReceiver {
        int status = -1;
        int reason = -1;
        @Override
        public void onReceive(Context mContext, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                Uri uri = downloadManager.getUriForDownloadedFile(downloadID);

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);
                Uri fileUri;
                if (cursor.moveToFirst()) {
                    status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    //reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                    switch (status){
                        case DownloadManager.STATUS_SUCCESSFUL:
                            fileUri = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
                            installGameAfterDownloading2(new File(fileUri.getPath()));
                            break;
                        case DownloadManager.STATUS_PAUSED:
                        case DownloadManager.STATUS_PENDING:
                            break;
                        case DownloadManager.STATUS_FAILED:
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            break;
                        case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                            fileUri = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
                            installGameAfterDownloading2(new File(fileUri.getPath()));
                            break;
                        case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                            break;
                    }
                }
                cursor.close();

                //mContext.unregisterReceiver(this);
            }
        }
    }
    private void installGameAfterDownloading2(File file1) {
        try {
            // When Api> 25 Use File Manager To Install The games
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                File directory = new File(DirectoryHelper.getInstance(this).getDownloadDirectory());
                File file = new File(directory, file1.getName());
                Uri fileUri = FileProvider.getUriForFile(mActivity,
                        BuildConfig.APPLICATION_ID.concat(".fileProvider") ,file);

                Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE, fileUri);
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mActivity.startActivity(intent);
            } else {
                Uri fileUri = Uri.fromFile(file1);
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                install.setDataAndType(fileUri, "application/vnd.android.package-archive");
                mActivity.startActivity(install);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void installPackage(@NonNull File file){
//        if (uri != null /*&& uri.getPath().endsWith(".apk")*/) {
//            Intent install = new Intent(Intent.ACTION_VIEW);
//            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            install.setDataAndType(uri, "application/vnd.android.package-archive");
//            startActivity(install);
//        }
       /* Intent downloadIntent;
        File fileLocation = new File(DirectoryHelper.getInstance(this).getDownloadDirectory(), fileName);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            downloadIntent = new Intent(Intent.ACTION_VIEW);
            downloadIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            downloadIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            downloadIntent.setDataAndType(Uri.fromFile(fileLocation)
                    , "application/vnd.android.package-archive");
        } else {
            downloadIntent = new Intent(Intent.ACTION_VIEW);
            downloadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            downloadIntent.setDataAndType(Uri.fromFile(fileLocation)
                    , "application/vnd.android.package-archive");
        }
        //startActivity(downloadIntent);*/
    }
//    private BroadcastReceiver onComplete = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context mContext, Intent intent) {
//            if (intent.getAction().equalsIgnoreCase(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
//                Uri uri = downloadManager.getUriForDownloadedFile(downloadID);
//                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
//
//                DownloadManager.Query query = new DownloadManager.Query();
//                query.setFilterById(downloadId);
//                Cursor cursor = downloadManager.query(query);
//                if (cursor.moveToFirst()) {
//                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//                    int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
//                }
//                cursor.close();
//
//                if (uri != null && uri.getPath().endsWith(".apk")) {
//                    Intent install = new Intent(Intent.ACTION_VIEW);
//                    install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    install.setDataAndType(uri, "application/vnd.android.package-archive");
//                    mContext.startActivity(install);
//                }
//                mContext.unregisterReceiver(this);
//            }
//        }
//    };


//    private String statusMessage() {
//        String msg = null;
//        try {
//            DownloadManager.Query query = new DownloadManager.Query();
//            Cursor c = downloadManager.query(query);
//            int status = c.getInt(c.getColumnIndex(downloadManager.COLUMN_STATUS));
//            //int status = c.getColumnIndex(downloadManager.COLUMN_STATUS);
//            switch (status) {
//                case DownloadManager.STATUS_SUCCESSFUL:
//                    // Install Game After Download
//                    msg = mContext.getString(R.string.download_complete_successfully);
//                    break;
//                case DownloadManager.STATUS_FAILED:
//                    msg = mContext.getString(R.string.download_faild);
//                    break;
//                case DownloadManager.STATUS_PAUSED:
//                    msg = mContext.getString(R.string.download_paused);
//                    break;
//                case DownloadManager.STATUS_PENDING:
//                    msg = mContext.getString(R.string.download_pending);
//                    break;
//                case DownloadManager.STATUS_RUNNING:
//                    msg = mContext.getString(R.string.download_in_progress);
//                    break;
//                /*default:
//                    msg = "Download is nowhere in sight";
//                    break;*/
//            }
//            return (msg);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return (msg);
//    }
}
