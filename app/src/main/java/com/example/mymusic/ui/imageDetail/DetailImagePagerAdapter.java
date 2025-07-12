package com.example.mymusic.ui.imageDetail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mymusic.R; // 자신의 R 클래스 경로
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class DetailImagePagerAdapter extends RecyclerView.Adapter<DetailImagePagerAdapter.DetailImageViewHolder> {
    public interface ZoomListener {
        void onZoomIn();
        void onZoomOut();
    }
    private ZoomListener zoomListener; // 줌 이벤트 리스너

    public void setZoomListener(ZoomListener zoomListener){
        this.zoomListener = zoomListener;
    }
    private List<String> imageUrls;
    private View.OnClickListener onClickListener;
    private Context context;

    // 생성자에서 이미지 리스트와 클릭 리스너를 받음
    public DetailImagePagerAdapter(Context context, List<String> imageUrls, View.OnClickListener listener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.onClickListener = listener;
    }

    public interface OnImageLongClickListener {
        void onLongClick(ImageView imageView, MotionEvent event, String imageUrl);
    }

    @NonNull
    @Override
    public DetailImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail_image, parent, false);
        return new DetailImageViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull DetailImageViewHolder holder, int position) {
        // 이전 프래그먼트에서 설정한 transitionName과 동일하게 설정
        String imageUrl = imageUrls.get(position);
        holder.imageView.setTransitionName("Transition_" + imageUrl);
        holder.imageUrl = imageUrl;

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .into(holder.imageView);


        holder.imageView.setOnClickListener(onClickListener);

        //holder.imageView.setOnTouchListener();

        holder.imageView.setOnScaleChangeListener((ScaleFactor, focusX, focusY) -> {
            float currentScale = holder.imageView.getScale();
            if (currentScale > 1.05f) {
                if (zoomListener != null) zoomListener.onZoomIn();
            } else{
                if (zoomListener != null) zoomListener.onZoomOut();
            }
        });


    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class DetailImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView;
        String imageUrl;
        DetailImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.detail_image);
        }
    }
}