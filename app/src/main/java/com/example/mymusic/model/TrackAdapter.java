package com.example.mymusic.model;

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
import com.squareup.picasso.Picasso;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<Track> tracks;
    private Context context;
    private OnDetailClickListener detailClickListener;
    private OnAddClickListener addClickListener;

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

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.title.setText(track.trackName);
        holder.artist.setText(track.artistName);

        // 이미지 로딩 (Picasso 필요)
        Picasso.get().load(track.artworkUrl).into(holder.image);

        holder.itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("track", track);
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
        return tracks.size();
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView image;
        ImageButton addButton, detailButton;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.titleTextView);
            artist = itemView.findViewById(R.id.artistTextView);
            image = itemView.findViewById(R.id.imageView);
            detailButton = itemView.findViewById(R.id.showDetailButton);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }
}
