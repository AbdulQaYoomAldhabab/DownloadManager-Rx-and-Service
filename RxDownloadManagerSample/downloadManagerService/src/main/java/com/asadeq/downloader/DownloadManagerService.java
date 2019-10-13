package com.asadeq.downloader;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

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
    public static DownloadManagerService downloadManagerService;
    public static DownloadManagerService getInstance(){
        if (downloadManagerService == null){
            downloadManagerService = new DownloadManagerService();
        }
        return downloadManagerService;
    }

    public Intent getDownloadService(@NonNull Context mContext, @NonNull String downloadPath
            , @NonNull String destinationPath, @NonNull String title,@NonNull DownloadReceiverListener downloadReceiverListener) {
        this.mDownloadReceiverListener = downloadReceiverListener;
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

        getApplicationContext().registerReceiver(new DownloadReceiver(),
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

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
    public static DownloadReceiverListener mDownloadReceiverListener;
    public class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context mContext, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                Cursor cursor = null;
                try {
                    DownloadManager manager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    cursor = manager.query(query);
                    if (!cursor.moveToFirst()) { // cursor is empty
                        if (mDownloadReceiverListener != null)
                            mDownloadReceiverListener.onErrorDownload(new IllegalStateException("Cursor empty, this shouldn't happened"));
                        return;
                    }
                    String MimeType = downloadManager.getMimeTypeForDownloadedFile(downloadId);
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    switch (status){
                        case DownloadManager.STATUS_SUCCESSFUL:
                            Log.i("DOWNLOAD LISTENER", "STATUS_SUCCESSFUL");
                            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                            String downloadedUriString = cursor.getString(uriIndex);
                            if (mDownloadReceiverListener != null && downloadedUriString != null) {
                                Uri downloadedUri = Uri.parse(downloadedUriString);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24 and above
                                    File mFile = new File(DirectoryHelper.getInstance(getApplicationContext())
                                            .getDownloadDirectory(), downloadedUri.getLastPathSegment());
                                    Uri uriForFile = FileProvider.getUriForFile(getApplicationContext()
                                            , getPackageName().concat(".provider"), mFile);
                                    mDownloadReceiverListener.onSuccessDownload(uriForFile);
                                } else {
                                    mDownloadReceiverListener.onSuccessDownload(downloadedUri);
                                }
                            }
                            Log.i("DOWNLOAD LISTENER", downloadedUriString);
                            break;
                        case DownloadManager.STATUS_PAUSED:
                        case DownloadManager.STATUS_PENDING:
                            Log.i("DOWNLOAD LISTENER", "STATUS_PENDING");
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            Log.i("DOWNLOAD LISTENER", "STATUS_RUNNING");
                            break;
                        case DownloadManager.STATUS_FAILED:
                            Log.i("DOWNLOAD LISTENER", "STATUS_FAILED");
                            if (mDownloadReceiverListener != null)
                                mDownloadReceiverListener.onErrorDownload(new IllegalStateException("Download Failed"));
                            break;
                        case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                            Log.i("DOWNLOAD LISTENER", "ERROR_INSUFFICIENT_SPACE");
                            if (mDownloadReceiverListener != null)
                                mDownloadReceiverListener.onErrorDownload(new IllegalStateException("INSUFFICIENT SPACE"));
                            break;
                        case DownloadManager.ERROR_UNKNOWN:
                            Log.i("DOWNLOAD LISTENER", "ERROR_UNKNOWN");
                            if (mDownloadReceiverListener != null)
                                mDownloadReceiverListener.onErrorDownload(new IllegalStateException("UNKNOWN ERROR"));
                            break;
                    }
                    //downloadManager.remove(id);
                    //subjectMap.remove(id);
                } catch(Exception e) {
                    Log.i("DOWNLOAD Exception ", e.getMessage());
                } finally {
                    cursor.close();
//                    unregisterReceiver(this);
                }
            }
        }
    }
}
