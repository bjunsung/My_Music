package com.example.mymusic.adapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.model.Album;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SimpleAlbumAdapter extends RecyclerView.Adapter<SimpleAlbumAdapter.AlbumViewHolder> {

    List<Album> albumList;
    AlbumViewHolder viewHolder;

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

    public SimpleAlbumAdapter(List<Album> albumList){
        this.albumList = albumList;
    }

    @NonNull
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_simple, parent, false);
        return new AlbumViewHolder(view);
    }

    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position){
        Album album = albumList.get(position);
        Picasso.get()
                .load(album.artworkUrl)
                .error(R.drawable.ic_image_not_found_foreground)
                .into(holder.albumImage);
        holder.albumName.setText(album.albumName);
        holder.releaseYear.setText(album.releaseDate.substring(0, 4));

        holder.itemView.setOnClickListener(v -> {
            //todo move album info fragment
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

}
