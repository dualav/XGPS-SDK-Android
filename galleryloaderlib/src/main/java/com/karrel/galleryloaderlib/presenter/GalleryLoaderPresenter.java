package com.karrel.galleryloaderlib.presenter;

import android.content.Intent;
import android.net.Uri;

/**
 * Created by 이주영 on 2017-05-18.
 */

public interface GalleryLoaderPresenter {
    void startCamera();

    void onActivityResult(int requestCode, int resultCode, Intent data);

    void startGallery();

    void startCamera(String imageFileName, String fileNameExtension);

    interface View {

        void errorMessage(String message);

        void grantUriPermission(String packageName, Uri photoURI, int i);

        void startActivityForResult(Intent cameraInent, int requestCamera);

        void onSelectedImage(Uri uri);
    }
}
