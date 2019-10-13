package com.asadeq.downloader;

import android.net.Uri;

public interface DownloadReceiverListener {
     void onSuccessDownload(Uri pathUri);
     void onErrorDownload(Throwable e);
}
