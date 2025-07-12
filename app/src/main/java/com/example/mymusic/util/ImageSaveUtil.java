package com.example.mymusic.util;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;


import java.io.OutputStream;

public class ImageSaveUtil {


    public static void saveImageFromUrl(Context context, String imageUrl) {
        Glide.with(context).asBitmap().load(imageUrl).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                saveImageToGallery(context, resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {}



            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                Toast.makeText(context, "이미지를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
            }

        });
    }


    private static void saveImageToGallery(Context context, Bitmap bitmap) {
        String filename = "IMG_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyMusic");

            Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                fos = context.getContentResolver().openOutputStream(imageUri);
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    Toast.makeText(context, "이미지가 저장되었습니다", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "저장에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
