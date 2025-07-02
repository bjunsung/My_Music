package com.example.mymusic.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.squareup.picasso.Picasso;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<Track> tracks;
    private Context context;
    private OnDetailClickListener detailClickListener;
    private OnAddClickListener addClickListener;
    private boolean showImage = true;
    private boolean showPosition = false;
    public interface OnDetailClickListener {
        void onItemClick(Track track);
    }

    public interface OnAddClickListener{
        void onItemClick(Track track);
    }

    public TrackAdapter(List<Track> tracks, Context context, OnDetailClickListener detailClickListener, OnAddClickListener addClickListener) {
        this.tracks = tracks;
        this.context = context;
        this.detailClickListener = detailClickListener;
        this.addClickListener = addClickListener;
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist, positionTextView;
        ImageView image;
        ImageButton addButton, detailButton;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.titleTextView);
            artist = itemView.findViewById(R.id.artistTextView);
            image = itemView.findViewById(R.id.imageView);
            detailButton = itemView.findViewById(R.id.showDetailButton);
            addButton = itemView.findViewById(R.id.addButton);
            positionTextView = itemView.findViewById(R.id.position);
        }
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        if (showPosition){
            holder.positionTextView.setText(String.valueOf(position+1));
            holder.positionTextView.setVisibility(TextView.VISIBLE);
        }

        Track track = tracks.get(position);

        holder.title.setText(track.trackName);
        holder.artist.setText(track.artistName);

        // 이미지 로딩 (Picasso 필요)
        if (track.artworkUrl != null && !track.artworkUrl.isEmpty()) {
            Picasso.get()
                    .load(track.artworkUrl)
                    //.placeholder(R.drawable.default_artist_image) // 로딩 중 보여줄 이미지
                    .error(R.drawable.ic_image_not_found_foreground)       // 실패 시 보여줄 이미지
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_image_not_found_foreground); // 기본 이미지로 대체
        }

        if (!showImage)
            holder.image.setVisibility(TextView.GONE);


        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            Favorite favorite = new Favorite(track);
            bundle.putParcelable("favorite", favorite);
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.musicInfoFragment, bundle);
        });


        //detailButton 클릭 event
        holder.detailButton.setOnClickListener(v -> detailClickListener.onItemClick(track));

        //addButton 클릭 event
        holder.addButton.setOnClickListener(v -> addClickListener.onItemClick(track));

    }

    @Override
    public int getItemCount() {
        if (tracks != null)
            return tracks.size();
        else
            return 0;
    }


    public void setShowImage(boolean show) {
        this.showImage = show;
        notifyDataSetChanged(); // 변경 반영 위해 전체 갱신
    }

    public void setShowPosition(boolean show) {
        this.showPosition = show;
        notifyDataSetChanged(); // 변경 반영 위해 전체 갱신
    }


}