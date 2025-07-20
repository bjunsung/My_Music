package com.example.mymusic.simpleArtistInfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.mymusic.R;
import com.example.mymusic.cache.reader.CustomFavoriteArtistImageReader;
import com.example.mymusic.cache.writer.CustomFavoriteArtistImageWriter;

import java.util.List;

public class SimpleRectangleImagePagerAdapter extends RecyclerView.Adapter<SimpleRectangleImagePagerAdapter.ImageViewHolder>{
    private final static String TAG = "SimpleRectangleImagePagerAdapter";
    private List<String> imageUrls;
    private final int FAKE_MULTIPLIER = 1000;
    private Context viewGroupContext;
    private OnImageLoadListener imageLoadListener;

    public interface OnImageLoadListener{
        void onLoadSuccess(int position, Bitmap bitmap);
    }

    public void setImageLoadListener(OnImageLoadListener imageLoadListener){
        this.imageLoadListener = imageLoadListener;
    }

    public SimpleRectangleImagePagerAdapter(List<String> imageUrls){
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public SimpleRectangleImagePagerAdapter.ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        viewGroupContext = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_pager, parent, false);
        return new ImageViewHolder(view);
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        int realPosition = position % imageUrls.size();
        String url = imageUrls.get(realPosition);

        Bitmap bitmapCache = CustomFavoriteArtistImageReader.get(viewGroupContext, url);
        if (bitmapCache != null){
            holder.imageView.setImageBitmap(bitmapCache);
            Log.d(TAG, "cache hit at realPosition " + realPosition);
            if (imageLoadListener != null){
                imageLoadListener.onLoadSuccess(realPosition, bitmapCache);
            }
        } else {
            Glide.with(viewGroupContext)
                    .asBitmap()
                    .load(url)
                    .centerCrop()
                    .override(SimpleArtistDialogHelper.ARTWORK_SIZE, SimpleArtistDialogHelper.ARTWORK_SIZE)
                    .error(R.drawable.ic_image_not_found_foreground)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            holder.imageView.setImageBitmap(resource);

                            CustomFavoriteArtistImageWriter.storeImageByBitmap(
                                    viewGroupContext, url, resource,
                                    SimpleArtistDialogHelper.ARTWORK_SIZE,
                                    SimpleArtistDialogHelper.ARTWORK_SIZE
                            );

                            if (imageLoadListener != null){
                                imageLoadListener.onLoadSuccess(realPosition, resource);
                            }

                            Log.d(TAG, "image loaded for realPosition " + realPosition);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        }

    }

    public int getRealCount() {
        return imageUrls.size();
    }
    public void updateList(List<String> imageUrls){
        this.imageUrls = imageUrls;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return imageUrls == null ? 0 : imageUrls.size() * FAKE_MULTIPLIER;
    }



}
