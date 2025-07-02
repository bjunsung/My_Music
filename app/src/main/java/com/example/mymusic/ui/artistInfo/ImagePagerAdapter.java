package com.example.mymusic.ui.artistInfo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mymusic.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
    private List<String> imageUrls; // Glide로 처리할 URL 리스트
    private Context context;
    private OnImageLongClickListener longClickListener;

    public interface OnImageLongClickListener {
        void onLongClick(ImageView imageView, String imageUrl);
    }

    public void setOnImageLongClickListener(OnImageLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public ImagePagerAdapter(Context context, List<String> urls) {
        this.context = context;
        this.imageUrls = urls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new ImageViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Glide.with(context)
                .load(imageUrls.get(position))
                .error(R.drawable.ic_image_not_found_foreground)
                .into(holder.imageView);

        String url = imageUrls.get(position);
        holder.imageView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(holder.imageView, url);
            }
            return true; // 이벤트 소비
        });

    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}
