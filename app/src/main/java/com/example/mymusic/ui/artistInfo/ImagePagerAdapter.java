package com.example.mymusic.ui.artistInfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.mymusic.R;
import com.example.mymusic.model.Artist;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
    private final String TAG = "ImagePagerAdapter";
    private List<String> imageUrls; // Glide로 처리할 URL 리스트
    private Context context;
    private OnImageLongClickListener longClickListener;
    private Artist artist;
    private String transitionNameForm;
    private int recyclerviewPosition;
    private OnImageLoadListener imageLoadListener;
    public interface OnImageLoadListener{
        void onLoadSuccess(ImageView imageView);
        void onLoadFailed();

    }

    public interface OnImageLongClickListener {
        void onLongClick(ImageView imageView, MotionEvent event, String imageUrl);
    }


    public ImagePagerAdapter(Context context, List<String> urls, OnImageLongClickListener longClickListener, Artist artist, String transitionNameForm, int recyclerviewPosition) {
        this.context = context;
        this.imageUrls = urls;
        this.longClickListener = longClickListener;
        this.artist = artist;
        this.transitionNameForm = transitionNameForm;
        this.recyclerviewPosition = recyclerviewPosition;
    }

    public void setImageLoadListener(OnImageLoadListener imageLoadListener) {
        this.imageLoadListener = imageLoadListener;
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = imageUrls.get(position);
        String transitionName = transitionNameForm + recyclerviewPosition + "_" + artist.artistName + "_" + artist.artistId + "_" + url;
        Log.d(TAG, "setting transition name for (ViewPager2) position at " + position + " transitionName: " + transitionName);
        ViewCompat.setTransitionName(holder.imageView, transitionName);

        Glide.with(context)
                .load(imageUrls.get(position))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @androidx.annotation.NonNull Target<Drawable> target, boolean isFirstResource) {
                        imageLoadListener.onLoadFailed();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@androidx.annotation.NonNull Drawable resource, @androidx.annotation.NonNull Object model, Target<Drawable> target, @androidx.annotation.NonNull DataSource dataSource, boolean isFirstResource) {
                        imageLoadListener.onLoadSuccess(holder.imageView);
                        return false;
                    }
                })
                .error(R.drawable.ic_image_not_found_foreground)
                .into(holder.imageView);





        // ✅ 2. 롱클릭을 감지할 GestureDetector 생성
        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (longClickListener != null) {
                    // 롱클릭이 발생하면 인터페이스를 통해 이벤트와 이미지 URL 전달
                    longClickListener.onLongClick(holder.imageView, e, url);
                }
            }
        });

        // ✅ 3. OnLongClickListener 대신 OnTouchListener 설정
        holder.imageView.setOnTouchListener((v, event) -> {
            // 터치 이벤트를 GestureDetector에 전달하여 롱클릭 감지
            gestureDetector.onTouchEvent(event);
            return true; // 이벤트를 소비했음을 알림
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
