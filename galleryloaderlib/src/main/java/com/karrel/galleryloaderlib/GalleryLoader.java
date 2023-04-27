package com.karrel.galleryloaderlib;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.karrel.galleryloaderlib.databinding.GalleryLoaderBinding;
import com.karrel.galleryloaderlib.presenter.GalleryLoaderPresenter;
import com.karrel.galleryloaderlib.presenter.GalleryLoaderPresenterImpl;

/**
 * Created by 이주영 on 2017-05-18.
 */

public class GalleryLoader extends AppCompatDialogFragment implements GalleryLoaderPresenter.View {

    public Builder mBuilder;
    private GalleryLoaderBinding mBinding;
    private GalleryLoaderPresenter mPresenter;
    private Toast mToast;

    public void show(FragmentManager fragmentManager) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(this, getTag());
        ft.commitAllowingStateLoss();
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mBuilder.mContext), R.layout.gallery_loader, null, false);

        // layout to display
        dialog.setContentView(mBinding.getRoot());

        mPresenter = new GalleryLoaderPresenterImpl(getContext(), this);

        // set color transpartent
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 버튼 세팅
        setupButtons();
    }

    private void setupButtons() {
        mBinding.camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBuilder.haveFileName()) {
                    mPresenter.startCamera(mBuilder.imageFileName, mBuilder.fileNameExtension);
                } else {
                    mPresenter.startCamera();
                }
            }
        });
        mBinding.gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.startGallery();
            }
        });
    }

    @Override
    public void errorMessage(String message) {
        if (mToast != null) mToast.cancel();

        mToast = Toast.makeText(getContext(), message, Toast.LENGTH_LONG);
        mToast.show();
    }

    @Override
    public void grantUriPermission(String packageName, Uri photoURI, int i) {
        getActivity().grantUriPermission(packageName, photoURI, i);
    }

    @Override
    public void onSelectedImage(Uri uri) {
        mBuilder.onImageSelectedListener.onImageSelected(uri);
        dismissAllowingStateLoss();
    }

    public interface OnImageSelectedListener {
        void onImageSelected(Uri uri);
    }

    public static class Builder {

        public Context mContext;
        public OnImageSelectedListener onImageSelectedListener;
        private String imageFileName;
        private String fileNameExtension;
        private boolean haveFileName = false;

        public Builder(@NonNull Context context) {
            this.mContext = context;
        }

        public Builder setOnImageSelectedListener(OnImageSelectedListener onImageSelectedListener) {
            this.onImageSelectedListener = onImageSelectedListener;
            return this;
        }

        public GalleryLoader create() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    && ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                throw new RuntimeException("Missing required WRITE_EXTERNAL_STORAGE permission. Did you remember to request it first?");
            }

            if (onImageSelectedListener == null) {
                throw new RuntimeException("You have to use setOnImageSelectedListener() or setOnMultiImageSelectedListener() for receive selected Uri");
            }

            GalleryLoader galleryLoader = new GalleryLoader();
            galleryLoader.mBuilder = this;
            return galleryLoader;
        }

        /**
         * @param imageFileName     파일명
         * @param fileNameExtension 확장자
         * @return
         */
        public Builder setImageFileName(String imageFileName, String fileNameExtension) {
            this.imageFileName = imageFileName;
            this.fileNameExtension = fileNameExtension;
            haveFileName = true;
            return this;
        }

        public boolean haveFileName() {
            return haveFileName;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPresenter.onActivityResult(requestCode, resultCode, data);
    }
}
