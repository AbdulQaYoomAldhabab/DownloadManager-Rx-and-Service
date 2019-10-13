# DownloadManager-Rx-and-Service
Download Manager using Rxjava or Service Download Manager

[![](https://jitpack.io/v/AbdulQaYoomAldhabab/DownloadManager-Rx-and-Service.svg)](https://jitpack.io/#AbdulQaYoomAldhabab/DownloadManager-Rx-and-Service)


# SetUp dependencies
Add it in your root build.gradle at the end of repositories:

```maven
allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
	
```

  # dependencies Library
```gradle
  dependencies {
	        implementation 'com.github.AbdulQaYoomAldhabab:DownloadManager-Rx-and-Service:1.0.1'

	}
```
  
  # Permission 
  Before you Call any of Those libraries you need to gran User Storage permission and Internet Permission.

```permissions
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```		

# Usage

# A 
For Using Custom DownloadDownloadManagerService You need to implement the DownloadReceiverListener in Your Activity or Fragment to get Download Uri.
	
	public class MainActivity extends AppCompatActivity implements DownloadReceiverListener {
		
			@Override
			protected void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);

			}
				
			@Override
			public void onSuccessDownload(Uri pathUri) {
					// Your Implementation With Downloaded Uri
			}

			@Override
			public void onErrorDownload(Throwable e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		}
```

```
  Initialize and Call This service using the following code
```
  Intent downloadIntent = DownloadManagerService.getInstance().getDownloadService(this
                        , DOWNLOAD_URL, DirectoryHelper.ROOT_DIRECTORY_NAME,"File Name", this);		
  startService(downloadIntent);
```


# B - Download Manager Using RxJava 

```
	RxDownloader.getInstance(this).download("download Url"
                , "File Name"
                , DirectoryHelper.getInstance(this).getDownloadDirectory()
                , RxDownloader.DEFAULT_MIME_TYPE,true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pathUri ->{
                    // Do what you want with downloaded URI
                    Log.i(TAG, pathUri.toString());
                }, throwable -> {
                    // Handle download failed here
                    Log.e(TAG, throwable.getMessage());
                });
```


# Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.
	
