package com.example.mymusic.adapter;

import android.content.Context;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mymusic.R;
import com.example.mymusic.model.Album;


import java.util.List;

public class SimpleAlbumAdapter extends RecyclerView.Adapter<SimpleAlbumAdapter.AlbumViewHolder> {

    private final String TAG = "SimpleAlbumAdapter";
    private Context context;
    List<Album> albumList;
    public interface OnAlbumClickListener{
        void onItemClick(Album album, ImageView sharedImageView, int position);
    }

    private OnAlbumClickListener albumClickListener;
    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView albumImage;
        TextView albumName, releaseYear;

        AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumImage = itemView.findViewById(R.id.album_image);
            albumName = itemView.findViewById(R.id.album_name);
            releaseYear = itemView.findViewById(R.id.release_year);
        }
    }

    public SimpleAlbumAdapter(List<Album> albumList, OnAlbumClickListener albumClickListener){
        this.albumList = albumList;
        this.albumClickListener = albumClickListener;
    }

    @NonNull
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_simple, parent, false);
        context = parent.getContext();
        return new AlbumViewHolder(view);
    }

    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position){
        Album album = albumList.get(position);

        String transitionName = "transition_starts_at_simple_album_adapter_" + holder.getAdapterPosition() + "_" + album.albumId + "_" + album.artworkUrl + "_" + album.releaseDate;
        ViewCompat.setTransitionName(holder.albumImage, transitionName);
        Log.d(TAG, "set transitionName at position " + holder.getAdapterPosition() + " : " + transitionName);

        Glide.with(holder.itemView)
                .load(album.artworkUrl)
                .override(400, 400)
                .error(R.drawable.ic_image_not_found_foreground)
                .into(holder.albumImage);
        holder.albumName.setText(album.albumName);
        holder.releaseYear.setText(album.releaseDate.substring(0, 4));

        holder.itemView.setOnClickListener(v -> {
            albumClickListener.onItemClick(
                    album,
                    holder.albumImage,
                    holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        if (albumList == null) return 0;
        return albumList.size();
    }


    public void updateData(List<Album> newList){
        this.albumList = newList;
        notifyDataSetChanged();
    }

}
