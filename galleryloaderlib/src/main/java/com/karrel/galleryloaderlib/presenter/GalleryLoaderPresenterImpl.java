package com.karrel.galleryloaderlib.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.karrel.galleryloaderlib.util.RealPathUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimerTask;

/**
 * Created by 이주영 on 2017-05-18.
 */

public class GalleryLoaderPresenterImpl implements GalleryLoaderPresenter {

    private GalleryLoaderPresenter.View mView;
    private Uri mCameraImageUri;
    private Context mContext;
    private static final int REQUEST_CAMERA = 0;
    private static final int REQUEST_GALLERY = 1;

    public GalleryLoaderPresenterImpl(Context context, GalleryLoaderPresenter.View view) {
        mContext = context;
        mView = view;
    }

    @Override
    public void startCamera() {
        startCamera(null, null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                onActivityResultCamera(mCameraImageUri);
            } else if (requestCode == REQUEST_GALLERY) {
                onActivityResultGallery(data);
            }
        }
    }

    @Override
    public void startGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (galleryIntent.resolveActivity(mContext.getPackageManager()) == null) {
            mView.errorMessage("This Application do not have Gallery Application");
            return;
        }

        mView.startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    public void startCamera(String imageFileName, String fileNameExtension) {
        Intent cameraInent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraInent.resolveActivity(mContext.getPackageManager()) != null) {

            File imageFile = getImageFile(imageFileName, fileNameExtension);
            Uri photoURI;
            try {
                photoURI = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", imageFile);
            } catch (NullPointerException e) {
                e.printStackTrace();
                mView.errorMessage(e.getMessage());
                return;
            }

            List<ResolveInfo> resolvedIntentActivities = mContext.getPackageManager().queryIntentActivities(cameraInent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                String packageName = resolvedIntentInfo.activityInfo.packageName;
                mView.grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            cameraInent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            mView.startActivityForResult(cameraInent, REQUEST_CAMERA);
        } else {
            mView.errorMessage("This Application do not have Camera Application");
            return;
        }
    }

    private File getImageFile(@Nullable String imageFileName, @Nullable String fileNameExtension) {
        // Create an image file name
        File imageFile = null;
        try {
            if (imageFileName == null || imageFileName.isEmpty()) {
                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
                imageFileName = "JPEG_" + timeStamp + "_";
            }
            if (fileNameExtension == null || fileNameExtension.isEmpty()) {
                fileNameExtension = ".jpg";
            }

            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            if (!storageDir.exists())
                storageDir.mkdirs();

            imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    fileNameExtension,         /* suffix */
                    storageDir      /* directory */
            );


            // Save a file: path for use with ACTION_VIEW intents
            mCameraImageUri = Uri.fromFile(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            mView.errorMessage("Could not create imageFile for camera");
        }


        return imageFile;
    }

    private void onActivityResultCamera(final Uri cameraImageUri) {

        MediaScannerConnection.scanFile(mContext, new String[]{cameraImageUri.getPath()}, new String[]{"image/jpeg"}, new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                Log.e("PresenterImpl", "onMediaScannerConnected");
            }

            @Override
            public void onScanCompleted(String s, final Uri uri) {
                ((Activity) mContext).runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        mView.onSelectedImage(uri);
                    }
                });

                Log.e("PresenterImpl", String.format("onScanCompleted > s : %s, uri : %s", s, uri.getPath()));
            }
        });
    }

    private void onActivityResultGallery(Intent data) {
        Uri temp = data.getData();

        if (temp == null) {
            mView.errorMessage("file is null");
        }

        String realPath = RealPathUtil.getRealPath(mContext, temp);

        Uri uri = null;
        try {
            uri = Uri.fromFile(new File(realPath));
        } catch (Exception ex) {
            uri = Uri.parse(realPath);
        }

        mView.onSelectedImage(uri);
        Log.e("PresenterImpl", String.format("onActivityResultGallery > uri : %s", uri.getPath()));
    }
}
