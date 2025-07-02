package com.example.mymusic.ui.imageDetail;

import android.view.LayoutInflater;
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

    private List<String> imageUrls;
    private View.OnClickListener onClickListener;

    // 생성자에서 이미지 리스트와 클릭 리스너를 받음
    public DetailImagePagerAdapter(List<String> imageUrls, View.OnClickListener listener) {
        this.imageUrls = imageUrls;
        this.onClickListener = listener;
    }

    @NonNull
    @Override
    public DetailImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail_image, parent, false);
        return new DetailImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailImageViewHolder holder, int position) {
        // 이전 프래그먼트에서 설정한 transitionName과 동일하게 설정
        String imageUrl = imageUrls.get(position);
        holder.imageView.setTransitionName("image_detail_" + imageUrl);

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .into(holder.imageView);

        // 이미지를 클릭하면 프래그먼트를 닫도록 리스너 설정
        holder.imageView.setOnClickListener(onClickListener);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class DetailImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView;
        DetailImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.detail_image);
        }
    }
}