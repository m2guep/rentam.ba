package com.digitalvision.rentam;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView view;
    ProgressBar progressBar;
    private static final int REQUEST_FILE_PICKER = 1;
    private ValueCallback<Uri> mUploadMessage;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private final static int FILE_CHOOSER_REQUEST_CODE = 1;
    private ValueCallback<Uri[]> mFilePathCallback5;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE  = 101;
    private Context context = this;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mUploadMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        //Runtime External storage permission for saving download files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                Log.d("permission", "permission denied to WRITE_EXTERNAL_STORAGE - requesting it");
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 1);
            }
        }

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        view = (WebView) this.findViewById(R.id.webView);
        if(!isNetworkStatusAvialable (getApplicationContext())) {
            view.setVisibility(view.INVISIBLE);
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("No Internet Connection!");
            builder.setMessage("Internet not available, Cross check your internet connectivity and try again.");
            final AlertDialog dialog = builder.create();
            dialog.show();
        }




        view.setWebChromeClient(new WebChromeClient(){
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {

                if (!task.isSuccessful()) {
                    Log.w("newToken", "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token

                String currentToken = task.getResult();
                byte[] currentTokenHashed = Base64.encode(currentToken.getBytes(), Base64.DEFAULT);
                Log.e("newToken mainActovoy", currentToken);;

                view.getSettings().setDomStorageEnabled(true);
                view.getSettings().setDatabaseEnabled(true);
                view.getSettings().setMinimumFontSize(1);
                view.getSettings().setMinimumLogicalFontSize(1);
                view.getSettings().setJavaScriptEnabled(true);
                view.getSettings().setAllowFileAccess(true);
                view.getSettings().setAllowContentAccess(true);
                view.getSettings().setLoadWithOverviewMode(true);
                view.getSettings().setUseWideViewPort(true);
                view.setWebViewClient(new MyBrowser());
                view.loadUrl("https://staging.rentam.ba/login?token_app_login="+new String(currentTokenHashed));


                //handle downloading
                view.setDownloadListener(new DownloadListener()
                {
                    @Override
                    public void onDownloadStart(String url, String userAgent,
                                                String contentDisposition, String mimeType,
                                                long contentLength) {
                        DownloadManager.Request request = new DownloadManager.Request(
                                Uri.parse(url));
                        request.setMimeType(mimeType);
                        String cookies = CookieManager.getInstance().getCookie(url);
                        request.addRequestHeader("cookie", cookies);
                        request.addRequestHeader("User-Agent", userAgent);
                        request.setDescription("Downloading File...");
                        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                                        url, contentDisposition, mimeType));
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
                    }});




                view.setWebChromeClient(new WebChromeClient() {

                    public void onProgressChanged(WebView view, int progress) {
                        checkPermission();
                        progressBar.setProgress(progress);
                        progressBar.setVisibility(View.GONE);
                        /*
                        findViewById(R.id.imageLoading1).setVisibility(View.GONE);

                        if (progress == 100) {
                            progressBar.setVisibility(View.GONE);
                            findViewById(R.id.imageLoading1).setVisibility(View.GONE);

                        } else {
                            progressBar.setVisibility(View.VISIBLE);

                        }

                         */
                    }

                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onPermissionRequest(final PermissionRequest request) {
                        request.grant(request.getResources());
                    }

                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Log.e("errorCode", errorCode + " : " + description + " at " + failingUrl);
                    }

                    // For Android 3.0+
                    public void openFileChooser(ValueCallback<Uri[]> uploadMsg) {
                        checkPermission();
                        // Create an intent to open the file picker activity
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                        // Start the file picker activity
                        startActivityForResult(Intent.createChooser(intent, "Select Files"), FILE_CHOOSER_REQUEST_CODE);

                        // Save the ValueCallback instance to use it later
                        mUploadMessages = uploadMsg;
                    }

                    // For Android 3.0+
                    public void openFileChooser(ValueCallback<Uri[]> uploadMsg, String acceptType) {
                        checkPermission();
                        // Create an intent to open the file picker activity
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                        // Start the file picker activity
                        startActivityForResult(Intent.createChooser(intent, "Select Files"), FILE_CHOOSER_REQUEST_CODE);

                        // Save the ValueCallback instance to use it later
                        mUploadMessages = uploadMsg;
                    }

                    //For Android 4.1
                    public void openFileChooser(ValueCallback<Uri[]> uploadMsg, String acceptType, String capture) {
                        checkPermission();
                        // Create an intent to open the file picker activity
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                        // Start the file picker activity
                        startActivityForResult(Intent.createChooser(intent, "Select Files"), FILE_CHOOSER_REQUEST_CODE);

                        // Save the ValueCallback instance to use it later
                        mUploadMessages = uploadMsg;
                    }

                    @Override
                    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                        openFileChooser(filePathCallback);
                        return true;
                    }




                    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        String message = "SSL Certificate error.";
                        switch (error.getPrimaryError()) {
                            case SslError.SSL_UNTRUSTED:
                                message = "The certificate authority is not trusted.";
                                break;
                            case SslError.SSL_EXPIRED:
                                message = "The certificate has expired.";
                                break;
                            case SslError.SSL_IDMISMATCH:
                                message = "The certificate Hostname mismatch.";
                                break;
                            case SslError.SSL_NOTYETVALID:
                                message = "The certificate is not yet valid.";
                                break;
                        }
                        message += " Do you want to continue anyway?";

                        builder.setTitle("SSL Certificate Error");
                        builder.setMessage(message);
                        builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.proceed();
                            }
                        });
                        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //handler.cancel();
                                finish();
                            }
                        });
                        final AlertDialog dialog = builder.create();
                        dialog.show();
                    }

                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (url.endsWith(".pdf")) {
                            //view.loadUrl("https://docs.google.com/gview?embedded=true&url=" + url);
                            //view.loadUrl("https://drive.google.com/viewerng/viewer?embedded=true&url=" + url);
                            String urlString = "https://docs.google.com/viewerng/viewer?url=" + url;
                            Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(urlString));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setPackage("com.android.chrome");
                            try {
                                context.startActivity(intent);
                            } catch (ActivityNotFoundException ex) {
                                // Chrome browser presumably not installed and open Kindle Browser
                                intent.setPackage("com.amazon.cloud9");
                                context.startActivity(intent);
                            }
                        }else {
                            view.loadUrl(url);
                        }
                        return true;
                    }
                });
            }
        });
    }
    public boolean checkPermission() {
        int camera = checkSelfPermission(Manifest.permission.CAMERA);
        int write_external_storage = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_external_storage = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);


        List<String> listPermissionsNeeded = new ArrayList<>();

        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (write_external_storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (read_external_storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //ketika disentuh tombol back
        if ((keyCode == KeyEvent.KEYCODE_BACK) && view.canGoBack()) {
            view.goBack(); //method goback() dieksekusi untuk kembali pada halaman sebelumnya
            return true;
        }
        // Jika tidak ada history (Halaman yang sebelumnya dibuka)
        // maka akan keluar dari activity
        return super.onKeyDown(keyCode, event);
    }
    public static boolean isNetworkStatusAvialable (Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null)
        {
            NetworkInfo netInfos = connectivityManager.getActiveNetworkInfo();
            if(netInfos != null)
                if(netInfos.isConnected())
                    return true;
        }
        return false;
    }

    private class MyBrowser extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(view.GONE);

        }
        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            String message = "SSL Certificate error.";
            switch (error.getPrimaryError()) {
                case SslError.SSL_UNTRUSTED:
                    message = "The certificate authority is not trusted.";
                    break;
                case SslError.SSL_EXPIRED:
                    message = "The certificate has expired.";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message = "The certificate Hostname mismatch.";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message = "The certificate is not yet valid.";
                    break;
            }
            message += " Do you want to continue anyway?";

            builder.setTitle("SSL Certificate Error");
            builder.setMessage(message);
            builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.proceed();
                }
            });
            builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //handler.cancel();
                    finish();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(isNetworkStatusAvialable (getApplicationContext())) {
                if (url.startsWith("https://accounts.google.com/o/oauth2/auth")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                else if (url.endsWith(".pdf")) {
                    //view.loadUrl("https://docs.google.com/gview?embedded=true&url=" + url);
                    //view.loadUrl("https://drive.google.com/viewerng/viewer?embedded=true&url=" + url);
                    String urlString = "https://docs.google.com/viewerng/viewer?url=" + url;
                    Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(urlString));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setPackage("com.android.chrome");
                    try {
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException ex) {
                        // Chrome browser presumably not installed and open Kindle Browser
                        intent.setPackage("com.amazon.cloud9");
                        context.startActivity(intent);
                    }
                }else {
                    view.loadUrl(url);
                }
                return true;
            } else {
                view.setVisibility(view.INVISIBLE);
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("No Internet Connection!");
                builder.setMessage("Internet not available, Cross check your internet connectivity and try again.");
                final AlertDialog dialog = builder.create();
                dialog.show();

                return false;
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {

            if (null == this.mUploadMessage && null == this.mUploadMessages) {
                return;
            }

           /* Uri result;
            if (requestCode != RESULT_OK){
                result = null;
            }else {
                result = intent == null ? this.mCapturedImageURI : intent.getData();
            }
            this.mUploadMessage.onReceiveValue(result);
            this.mUploadMessage = null;*/
            if (null != mUploadMessage) {
                handleUploadMessage(requestCode, resultCode, intent);

            } else if (mUploadMessages != null) {
                handleUploadMessages(requestCode, resultCode, intent);
            }
        }
    }

    private void handleUploadMessage(int requestCode, int resultCode, Intent intent) {
        Uri result = null;
        try {
            if (resultCode != RESULT_OK) {
                result = null;
            } else {
                // retrieve from the private variable if the intent is null

                result = intent == null ? mCapturedImageURI : intent.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void handleUploadMessages(int requestCode, int resultCode, Intent intent) {
        Uri[] results = null;
        try {
            if (resultCode != RESULT_OK) {
                results = null;
            } else {
                if (intent != null) {
                    String dataString = intent.getDataString();
                    ClipData clipData = intent.getClipData();
                    if (clipData != null) {
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            results[i] = item.getUri();
                        }
                    }
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                } else {
                    results = new Uri[]{mCapturedImageURI};
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUploadMessages.onReceiveValue(results);
        mUploadMessages = null;
    }
}