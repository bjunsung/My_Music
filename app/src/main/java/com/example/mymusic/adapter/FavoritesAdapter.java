package com.example.mymusic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {
    List<Favorite> favoritesList;
    OnDeleteClickListener deleteClickListener;
    OnLyricClickListener lyricClickListener;

    public interface OnDeleteClickListener{
        void onItemClick(Track track);
    }

    public interface OnLyricClickListener{
        void onItemClick();
    }

    public FavoritesAdapter(List<Favorite> favoritesList, OnDeleteClickListener deleteClickListener, OnLyricClickListener lyricClickListener){
        this.favoritesList = favoritesList;
        this.deleteClickListener = deleteClickListener;
        this.lyricClickListener = lyricClickListener;
    }

    public class FavoriteViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView title, artist, album, duration, releasedDate, addedDate;
        ImageButton deleteButton, lyricButton;
        public FavoriteViewHolder(@NonNull View itemView){
            super(itemView);
            image = itemView.findViewById(R.id.imageView);
            title = itemView.findViewById(R.id.titleTextView);
            artist = itemView.findViewById(R.id.artistTextView);
            album = itemView.findViewById(R.id.albumTextView);
            duration = itemView.findViewById(R.id.durationTextView);
            releasedDate = itemView.findViewById(R.id.releaseDateTextView);
            addedDate = itemView.findViewById(R.id.addedDateTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            lyricButton = itemView.findViewById(R.id.lyric_button);
        }
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position){
        Favorite favorite = favoritesList.get(position);
        Track track = favorite.track;
        Picasso.get().load(track.artworkUrl).into(holder.image);
        holder.title.setText(track.trackName);
        holder.artist.setText(track.artistName);
        holder.album.setText(track.albumName);
        int durationSec = (int) Double.parseDouble(track.durationMs)/1000;
        String durationStr = durationSec/60 + "분 " + durationSec%60 + "초";
        holder.duration.setText(durationStr);
        holder.addedDate.setText(favorite.addedDate);
        holder.releasedDate.setText(track.releaseDate);
        holder.deleteButton.setOnClickListener(v -> {
            deleteClickListener.onItemClick(track);
        });

       holder.lyricButton.setOnClickListener(v -> {
           lyricClickListener.onItemClick();
       });
    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }


    public void updateData(List<Favorite> newList) {
        this.favoritesList = newList;
        notifyDataSetChanged();
    }


}
