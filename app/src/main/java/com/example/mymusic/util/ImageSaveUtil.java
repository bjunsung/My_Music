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

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.OutputStream;

public class ImageSaveUtil {

    public static void saveImageFromUrl(Context context, String imageUrl) {
        Picasso.get().load(imageUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(@NonNull Bitmap bitmap, @Nullable Picasso.LoadedFrom from) {
                saveImageToGallery(context, bitmap);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Toast.makeText(context, "이미지를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLoad(@Nullable Drawable placeHolderDrawable) {
                // No-op
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
