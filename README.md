# DownloadManager-Rx-and-Service
Download Manager using Rxjava or Service Download Manager

[![](https://jitpack.io/v/AbdulqaioomAldhabab/DownloadManager-Rx-and-Service.svg)](https://jitpack.io/#AbdulqaioomAldhabab/DownloadManager-Rx-and-Service)

# SetUp dependencies
Add it in your root build.gradle at the end of repositories:
```
allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
	
```
  # dependencies Library
```
  dependencies {
	        implementation 'com.github.AbdulqaioomAldhabab:DownloadManager-Rx-and-Service:$lastVertion'
	}
```
  
  # Permission
3 - Before you Call any of Those libraries you need to gran User Storage permission and Internet Permission.

```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```		
```
# Usage

# A - Custom DownloadDownloadManagerService You need to implement the DownloadReceiverListener in Your Activity or Fragment

```
		Ex. 
		public class MainActivity extends AppCompatActivity implements DownloadReceiverListener {
		
		    private DownloadReceiverListener downloadReceiver;
				@Override
				protected void onCreate(Bundle savedInstanceState) {
						super.onCreate(savedInstanceState);
						setContentView(R.layout.activity_main);

						downloadReceiver = this;
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

  #  Initialize and Call This service using the following code
  
```
  Intent downloadIntent = DownloadManagerService.getInstance().getDownloadService(this
                        , DOWNLOAD_URL, DirectoryHelper.ROOT_DIRECTORY_NAME,"File Name", downloadReceiver);
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
	
	
