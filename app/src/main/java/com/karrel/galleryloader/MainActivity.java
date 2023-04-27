package com.karrel.galleryloader;

import android.Manifest;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.karrel.galleryloader.databinding.ActivityMainBinding;
import com.karrel.galleryloaderlib.GalleryLoader;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding mBinding;
    public RequestManager mGlideRequestManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.galleryLoader.setOnClickListener(onClickListener);

        mGlideRequestManager = Glide.with(this);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startGalleryLoader();
        }
    };

    private void startGalleryLoader() {

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                GalleryLoader loader = new GalleryLoader.Builder(MainActivity.this)
                        .setOnImageSelectedListener(onImageSelectedListener)
                        .create();
                loader.show(getSupportFragmentManager());
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        new TedPermission(MainActivity.this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
    }

    private GalleryLoader.OnImageSelectedListener onImageSelectedListener = new GalleryLoader.OnImageSelectedListener() {
        @Override
        public void onImageSelected(Uri uri) {
            mGlideRequestManager
                    .load(uri)
                    .into(mBinding.image);

            mBinding.path.setText(String.format("path : %s", uri.getPath()));
        }
    };
}
