package com.example.camerademo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private String TAG = "CameraDemo";
    private Button photoBtn;
    ImageView ivPreview;

    private static final int REQUEST_CAPTURE_IMAGE = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 45;
    String imageFilePath;
    File image;
    String imageFileName;
    String[] permArray = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        photoBtn = (Button) findViewById(R.id.takePhotoBtn);
        ivPreview = (ImageView) findViewById(R.id.ivPreview);

        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions(MainActivity.this, permArray)) {
                    Log.d(TAG, "Permissions ok opening camera");
                    openCameraIntent();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.permmsg), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Permissions not set up starting request");
                    ActivityCompat.requestPermissions(MainActivity.this, permArray, REQUEST_CODE_PERMISSIONS);
                }
            }
        });
    }


    //helper to check if permissions are all granted
    public boolean checkPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "has permission check failed for:" + permission);
                    return false;
                }
                else
                {
                    Log.d(TAG, "has permission check passed for:" + permission);
                }
            }
            Log.d(TAG, "has permission check passed for:" + Arrays.toString(permissions));
            return true;
        }
        return false;
    }

    // Get results from permission request (NOTE I don't need all these perms I just included them to check when I was having issues with getExternalStoragePublicDirectory() as it was throwing a permission error
    // which turned out to be due to the method being deprecated and unsuitable for >= API 29
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length == 3)
            {
                boolean writeExternalPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean cameraPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean readPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, permissions[0] + " " + writeExternalPermission);
                Log.d(TAG, permissions[1] + " " + cameraPermission);
                Log.d(TAG, permissions[2] + " " + readPermission);
                if (writeExternalPermission && cameraPermission && readPermission) {
                    Toast.makeText(this, "Permissions Granted for write external & camera : intent request start", Toast.LENGTH_LONG).show();
                    openCameraIntent();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void openCameraIntent() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {
            //Create a file to store the image
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d(TAG, "Camera intent failed" + ex.toString());
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.camerademo.fileprovider", photoFile);
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(pictureIntent, REQUEST_CAPTURE_IMAGE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_IMAGE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Image ok, Filepath: " + imageFilePath);
                // by this point we have the camera photo on disk
                Bitmap takenImage = BitmapFactory.decodeFile(image.getAbsolutePath());
                // Load the taken image into a preview
                ImageView ivPreview = (ImageView) findViewById(R.id.ivPreview);
                ivPreview.setImageBitmap(takenImage);
                galleryAddPic();
            } else { // Result was a failure
                Log.d(TAG, "Picture was not taken!");
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
        imageFileName = "IMG_" + timeStamp + "_";
        //Below saves files local & private to app they will not appear in tha gallery!
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);  // THIS is the code stopping the image being added to the gallery!!
        image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        imageFilePath = image.getAbsolutePath();
        saveImage(this);
        return image;
    }

    private void saveImage(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/CameraDemo");
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        }
    }

    private void galleryAddPic() {  // this wil NOT work with getExternalFilesDir() but the alternative(in googles own docs) is deprecated getExternalStoragePublicDirectory
        Log.d(TAG, "Adding pic to gallery");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imageFilePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
}