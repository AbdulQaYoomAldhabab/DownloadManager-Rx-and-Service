package com.asadeq.rxdownloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.asadeq.rxdownloader.utils.DirectoryHelper;
import com.asadeq.rxdownloader.utils.LongSparseArray;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by esa on 10/11/15, with awesomeness
 */
public class RxDownloader {

    public static final String DEFAULT_MIME_TYPE = "*/*";
    private Context context;
    private LongSparseArray<PublishSubject<Uri>> subjectMap = new LongSparseArray<>();
    private DownloadManager downloadManager;

    public static RxDownloader rxDownloader = null;
    public static RxDownloader getInstance(@NonNull Context context) {
        if (rxDownloader == null)
            rxDownloader = new RxDownloader(context);
        return rxDownloader;
    }
    private RxDownloader(@NonNull Context context) {
        this.context = context.getApplicationContext();
        DownloadStatusReceiver downloadStatusReceiver = new DownloadStatusReceiver();
        context.registerReceiver(downloadStatusReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @NonNull
    private DownloadManager getDownloadManager() {
        if (downloadManager == null && context != null) {
            downloadManager = (DownloadManager) context.getApplicationContext()
                    .getSystemService(Context.DOWNLOAD_SERVICE);
        }
        if (downloadManager == null) {
            throw new RuntimeException("Can't get DownloadManager from system service");
        }
        return downloadManager;
    }

    public  Observable<Uri> download(@NonNull String url,
                                       @NonNull String filename,
                                       boolean showCompletedNotification) {
        return download(url, filename, DEFAULT_MIME_TYPE, showCompletedNotification);
    }

    public Observable<Uri> download(@NonNull String url,
                                       @NonNull String filename,
                                       @NonNull String mimeType,
                                       boolean showCompletedNotification) {
        return download(createRequest(url, filename, null,
                mimeType, true, showCompletedNotification));
    }

    public Observable<Uri> download(@NonNull String url,
                                    @NonNull String filename,
                                    @NonNull String destinationPath,
                                    @NonNull String mimeType,
                                    boolean showCompletedNotification) {
        return download(createRequest(url, filename, destinationPath,
                mimeType, true, showCompletedNotification));
    }

    public Observable<Uri> downloadInFilesDir(@NonNull String url,
                                                 @NonNull String filename,
                                                 @NonNull String destinationPath,
                                                 @NonNull String mimeType,
                                                 boolean showCompletedNotification) {
        return download(createRequest(url, filename, destinationPath,
                mimeType, false, showCompletedNotification));
    }

    public Observable<Uri> download(DownloadManager.Request request) {
        long downloadId = getDownloadManager().enqueue(request);

        PublishSubject<Uri> publishSubject = PublishSubject.create();
        subjectMap.put(downloadId, publishSubject);

        return publishSubject;
    }

    private DownloadManager.Request createRequest(@NonNull String url,
                                                  @NonNull String filename,
                                                  @Nullable String destinationPath,
                                                  @NonNull String mimeType,
                                                  boolean inPublicDir,
                                                  boolean showCompletedNotification) {

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(filename);
        request.setMimeType(mimeType);

        if (destinationPath == null) {
//            destinationPath = Environment.DIRECTORY_DOWNLOADS;
            destinationPath = DirectoryHelper.ROOT_DIRECTORY_NAME;
        }

        File destinationFolder = new File(DirectoryHelper.getInstance(context)
                .getDownloadDirectory());
//        File destinationFolder = inPublicDir
//                ? Environment.getExternalStoragePublicDirectory(destinationPath)
//                : new File(context.getFilesDir(), destinationPath);

        createFolderIfNeeded(destinationFolder);
        //removeDuplicateFileIfExist(destinationFolder, filename);
        DirectoryHelper.getInstance(context).removeDuplicateFileIfExist(filename);
        if (inPublicDir) {
            request.setDestinationInExternalPublicDir(destinationPath, filename);
        } else {
            request.setDestinationInExternalFilesDir(context, destinationPath, filename);
        }

        request.setNotificationVisibility(showCompletedNotification
                ? DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                : DownloadManager.Request.VISIBILITY_VISIBLE);

        return request;
    }

    private void createFolderIfNeeded(@NonNull File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("Can't create directory");
        }
    }

    private class DownloadStatusReceiver  extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equalsIgnoreCase(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                PublishSubject<Uri> publishSubject = subjectMap.get(downloadId);

                if (publishSubject == null)
                    return;

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                DownloadManager downloadManager = getDownloadManager();
                Cursor cursor = downloadManager.query(query);

                if (!cursor.moveToFirst()) {
                    cursor.close();
                    downloadManager.remove(downloadId);
                    publishSubject.onError(new IllegalStateException("Cursor empty, this shouldn't happened"));
                    subjectMap.remove(downloadId);
                    return;
                }
                String MimeType = downloadManager.getMimeTypeForDownloadedFile(downloadId);
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status){
                    case DownloadManager.STATUS_SUCCESSFUL:
                        Log.i("DOWNLOAD LISTENER", "STATUS_SUCCESSFUL");
                        int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        String downloadedUriString = cursor.getString(uriIndex);
                        Uri downloadedUri = Uri.parse(downloadedUriString);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24 and above
                            File mFile = new File(DirectoryHelper.getInstance(context).getDownloadDirectory()
                                    , downloadedUri.getLastPathSegment());
                            Uri uriForFile = FileProvider.getUriForFile(context
                                    , context.getPackageName().concat(".provider"), mFile);
                            publishSubject.onNext(uriForFile);
                            //publishSubject.onNext(Uri.parse(uriForFile.getScheme()+":/"+uriForFile.getSchemeSpecificPart()));
                            publishSubject.onComplete();
                        } else {
                            publishSubject.onNext(downloadedUri);
                            publishSubject.onComplete();
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
                        publishSubject.onError(new IllegalStateException("Download Failed"));
                        break;
                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                        Log.i("DOWNLOAD LISTENER", "ERROR_INSUFFICIENT_SPACE");
                        publishSubject.onError(new IllegalStateException("ERROR_INSUFFICIENT_SPACE"));
                        break;
                    case DownloadManager.ERROR_UNKNOWN:
                        Log.i("DOWNLOAD LISTENER", "ERROR_UNKNOWN");
                        publishSubject.onError(new IllegalStateException("ERROR_UNKNOWN"));
                        break;
                }
                downloadManager.remove(downloadId);
                subjectMap.remove(downloadId);
                cursor.close();
                //context.unregisterReceiver(this);
            }
        }
    }
}
