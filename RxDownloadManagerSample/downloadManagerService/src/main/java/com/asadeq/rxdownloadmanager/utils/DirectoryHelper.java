package com.asadeq.rxdownloadmanager.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import com.asadeq.rxdownloadmanager.BuildConfig;
import java.io.File;
import io.reactivex.annotations.NonNull;


public class DirectoryHelper extends ContextWrapper {

    public static final String ROOT_DIRECTORY_NAME = getRootDirectoryName();
    public static final String BACK_SLASH_DELIMITER = "/";

    private DirectoryHelper(Context context) {
        super(context);
        createFolderDirectories();
    }

    public static DirectoryHelper directoryHelper;
    public static DirectoryHelper getInstance(Context context) {
        if (directoryHelper == null){
            directoryHelper = new DirectoryHelper(context);
        }
        return directoryHelper;
    }

    private boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(extStorageState);
    }
    private static String getRootDirectoryName() {
        return BuildConfig.APPLICATION_ID.substring(
                BuildConfig.APPLICATION_ID.lastIndexOf(".")+1);
    }

    public void createFolderDirectories() {
        if (isExternalStorageAvailable()) {
            createExternalDirectory(ROOT_DIRECTORY_NAME);
        } else {
            createInternalDirectory(ROOT_DIRECTORY_NAME);
        }
    }

    private void createInternalDirectory(String directoryName) {
        File file = new File(Environment.DIRECTORY_DOWNLOADS, directoryName);
        if (!file.exists())
            file.mkdir();
    }

    private void createExternalDirectory(String directoryName) {
        if (!isExternalDirectoryExist(directoryName)) {
            File file = new File(getDirectory(), directoryName);
            file.mkdir();
        }
    }
    private boolean isExternalDirectoryExist(String directoryName) {
        File file = new File(getDirectory().concat(directoryName));
        return file.isDirectory() && file.exists();
    }

    public void removeDuplicateFileIfExist(@NonNull String fileName) {
        File file = new File(getDirectory(), fileName);
        file.deleteOnExit();
    }

    private String getDirectory(){
        if (isExternalStorageAvailable()) {
            return Environment.getExternalStorageDirectory().getPath().concat(BACK_SLASH_DELIMITER);
        }
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath().concat(BACK_SLASH_DELIMITER);
    }

    public String getDownloadDirectory(){
        String mDir = null;
        if (isExternalStorageAvailable()) {
            mDir = getDirectory().concat(ROOT_DIRECTORY_NAME);
        } else {
            mDir = getDirectory().concat(ROOT_DIRECTORY_NAME);
        }
        return mDir;
    }

}
