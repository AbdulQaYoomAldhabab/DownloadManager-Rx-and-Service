package com.asadeq.rxdownloadmanagersample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.asadeq.rxdownloadmanager.DownloadManagerService;
import com.asadeq.rxdownloadmanager.DirectoryHelper;
import com.asadeq.rxdownloader.RxDownloader;
import com.asadeq.rxdownloadmanager.DownloadReceiverListener;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DownloadReceiverListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String IMAGE_DOWNLOAD_URL = "http://globalmedicalco.com/photos/globalmedicalco/9/41427.jpg";
    private static final String SONG_DOWNLOAD_URL = "https://cloudup.com/files/inYVmLryD4p/download";
    private static final String APK_DOWNLOAD_URL = "http://play.mtnsyr.com/sygames/android/gf_petvet_v1_0_11_en_fr_de_es_it_pt_android.apk"; // 3 MB
//    private static final String APK_DOWNLOAD_URL = "http://play.mtnsyr.com/sygames/android/globalfun_robinhoodprince_1_1_2_en_es_de_fr_it_pt_android.apk"; // 4 MB
//    private static final String APK_DOWNLOAD_URL = "http://play.mtnsyr.com/sygames/android/iec_SnowJewel_en_1_0_0.apk"; // 9 MB
//    private static final String APK_DOWNLOAD_URL = "http://play.mtnsyr.com/sygames/android/duddu.apk"; // 21 MB
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 54654;
    private static final int INSTALL_PACKAGES_REQUEST_CODE = 54655;
    private DownloadReceiverListener downloadReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.downloadImageButton).setOnClickListener(this);
        findViewById(R.id.downloadSongButton).setOnClickListener(this);
        findViewById(R.id.downloadApkButton).setOnClickListener(this);
        findViewById(R.id.downloadApkButtonRx).setOnClickListener(this);
        downloadReceiver = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUEST_CODE);
            return;
        }
        startActivity(enableUnknownAppSourcesIntent(this));


    }

    public Intent enableUnknownAppSourcesIntent(Context mContext) {
        return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                , Uri.parse("package:" + mContext.getPackageName()));
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.downloadImageButton: {
                install(Uri.parse("file:///storage/emulated/0/storage/emulated/0/rxdownloadmanager/gf_petvet_v1_0_11_en_fr_de_es_it_pt_android-1.apk"));
//                startService(DownloadManagerService.getInstance().getDownloadService(this
//                        , IMAGE_DOWNLOAD_URL, DirectoryHelper.ROOT_DIRECTORY_NAME, "IMAGE Name", downloadReceiver));
                break;
            }
            case R.id.downloadSongButton: {
                startService(DownloadManagerService.getInstance().getDownloadService(this
                        , SONG_DOWNLOAD_URL, DirectoryHelper.ROOT_DIRECTORY_NAME,"SONG Name", downloadReceiver));
                break;
            }
            case R.id.downloadApkButton: {
                startService(DownloadManagerService.getInstance().getDownloadService(this
                        , APK_DOWNLOAD_URL, DirectoryHelper.ROOT_DIRECTORY_NAME,"APK Name", downloadReceiver));
                break;
            }
            case R.id.downloadApkButtonRx: {
                //Uri uri = Uri.parse("file:///storage/emulated/0/storage/emulated/0/rxdownloadmanager/gf_petvet_v1_0_11_en_fr_de_es_it_pt_android-1.apk");
                //install("file:///storage/emulated/0/storage/emulated/0/rxdownloadmanager/gf_petvet_v1_0_11_en_fr_de_es_it_pt_android-1.apk");
                startDownloadingRx(APK_DOWNLOAD_URL);
                break;
            }
        }
    }

    private void startDownloadingRx(String url) {
        //RxDownloader rxDownloader = new RxDownloader(mContext);
        RxDownloader.getInstance(this).download(url
                , Uri.parse(url).getLastPathSegment()
                , DirectoryHelper.getInstance(this).getDownloadDirectory()
                , RxDownloader.DEFAULT_MIME_TYPE,true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pathUri ->{
                    // Do what you want with downloaded path
                    Log.i(TAG, pathUri.toString());
                    install(pathUri);
                }, throwable -> {
                    // Handle download failed here
                    Log.e(TAG, throwable.getMessage());
                });
    }

    @Override
    public void onSuccessDownload(Uri pathUri) {
        install(pathUri);
    }

    @Override
    public void onErrorDownload(Throwable e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
    public void install(Uri fileUri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24 and above
                Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(fileUri);
                startActivity(intent);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                DirectoryHelper.getInstance(this).createFolderDirectories();
        }
    }
    public void permissionsObserver(final AppCompatActivity mAppCompatActivity) {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        new RxPermissions(mAppCompatActivity)
                .request(permissions)
                .subscribe(new DisposableObserver<Boolean>() {
                    @Override
                    public void onNext(Boolean granted) {
                        if (!granted) {
                            // At least one permission is denied
                            Log.i("TAG", "PERMISSIONS ARE DENIED");
                            return;
                        }
                        // All requested permissions are granted
                        Log.i("TAG", "PERMISSIONS ARE GRANTED");
                    }
                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(mAppCompatActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onComplete() {
                        Log.i("TAG", "CHECK PERMISSIONS COMPLETE");
                    }
                });
    }

}
