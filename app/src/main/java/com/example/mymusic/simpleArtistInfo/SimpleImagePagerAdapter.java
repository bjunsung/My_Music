package com.example.mymusic.simpleArtistInfo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.mymusic.R;

import java.util.List;

public class SimpleImagePagerAdapter extends RecyclerView.Adapter<SimpleImagePagerAdapter.ImageViewHolder>{
    private final static String TAG = "SimpleImagePagerAdapter";
    private List<String> imageUrls;
    private int recyclerViewPosition;
    private Context viewGroupContext;
    private OnImageLoadListener imageLoadListener;

    public interface OnImageLoadListener{
        void onLoadSuccess(int position, Bitmap bitmap);
    }

    public void setImageLoadListener(OnImageLoadListener imageLoadListener){
        this.imageLoadListener = imageLoadListener;
    }

    public SimpleImagePagerAdapter(List<String> imageUrls){
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public SimpleImagePagerAdapter.ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        viewGroupContext = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_pager_circle, parent, false);
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
        String url = imageUrls.get(holder.getAdapterPosition());

        if (url != null){
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
                            if (imageLoadListener != null){
                                imageLoadListener.onLoadSuccess(holder.getAdapterPosition(), resource);
                            }
                            Log.d(TAG, "image bitmap ready for position " + holder.getAdapterPosition());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        }


    }

    public void updateList(List<String> imageUrls){
        this.imageUrls = imageUrls;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }



}
