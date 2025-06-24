package com.example.mymusic.adapter;

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
import com.example.mymusic.data.util.NumberUtils;
import com.example.mymusic.model.Artist;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FavoriteArtistAdapter extends RecyclerView.Adapter<FavoriteArtistAdapter.FavoriteArtistViewHolder> {

    List<Artist> artistList;
    OnDeleteClickListener deleteClickListener;
    FavoriteArtistViewModel viewModel;

    public interface OnDeleteClickListener{
        void onItemClick(Artist artist);
    }

    public FavoriteArtistAdapter(List<Artist> artistList, OnDeleteClickListener deleteClickListener, FavoriteArtistViewModel viewModel){
        this.artistList = artistList;
        this.deleteClickListener = deleteClickListener;
        this.viewModel = viewModel;
    }
    public static class FavoriteArtistViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView artistName, followers, addedDate;
        ImageButton deleteButton;
        public FavoriteArtistViewHolder(@NonNull View itemView){
            super(itemView);
            image = itemView.findViewById(R.id.imageView);
            artistName = itemView.findViewById(R.id.artistNameTextView);
            followers = itemView.findViewById(R.id.followersTextView);
            addedDate = itemView.findViewById(R.id.addedDateTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    @NonNull
    @Override
    public FavoriteArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_artist, parent, false);
        return new FavoriteArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteArtistViewHolder holder, int position){
        Artist artist = artistList.get(position);
        holder.artistName.setText(artist.artistName);
        holder.followers.setText(NumberUtils.formatWithComma(artist.followers));
        //String addedDate = viewModel.getAddedDateAsync(artist.artistId);

        // 비동기로 날짜 가져오기
        viewModel.getAddedDateAsync(artist.artistId, addedDate -> holder.addedDate.setText(addedDate != null ? addedDate : "날짜 없음"));

        Picasso.get()
                .load(artist.artworkUrl)
                .error(R.drawable.ic_image_not_found_foreground)
                .into(holder.image);

        holder.deleteButton.setOnClickListener(v -> deleteClickListener.onItemClick(artist));

        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable("artist", artist);
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.artist_info, bundle);
        });

    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }

    public void updateData(List<Artist> newList){
        this.artistList = newList;
        notifyDataSetChanged();
    }

}
