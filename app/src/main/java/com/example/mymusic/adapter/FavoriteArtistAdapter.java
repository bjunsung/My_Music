package com.example.mymusic.adapter;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.util.NumberUtils;
import com.example.mymusic.model.Artist;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FavoriteArtistAdapter extends RecyclerView.Adapter<FavoriteArtistAdapter.FavoriteArtistViewHolder> {

    List<Artist> artistList;
    OnDeleteClickListener deleteClickListener;
    OnMetadataClickListener metadataClickListener;
    FavoriteArtistViewModel viewModel;
    OnItemNavigateClickListener navigateClickListener;
    private final String transitionNameForm = "Transition_favorite_artist_adapter_to_artist_";

    public interface OnDeleteClickListener{
        void onItemClick(Artist artist);
    }

    public interface OnMetadataClickListener{
        void onItemClick(String artistId);
    }

    public interface OnItemNavigateClickListener {
        void onNavigateClick(FavoriteArtist favorite, ImageView sharedImageView, String transitionNameForm, int position);
    }

    public FavoriteArtistAdapter(List<Artist> artistList, OnDeleteClickListener deleteClickListener, OnMetadataClickListener metadataClickListener, FavoriteArtistViewModel viewModel, OnItemNavigateClickListener navigateClickListener){
        this.artistList = artistList;
        this.deleteClickListener = deleteClickListener;
        this.metadataClickListener = metadataClickListener;
        this.viewModel = viewModel;
        this.navigateClickListener = navigateClickListener;
    }
    public static class FavoriteArtistViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView artistName, followers, addedDate;
        ImageButton deleteButton, addButton;
        public FavoriteArtistViewHolder(@NonNull View itemView){
            super(itemView);
            image = itemView.findViewById(R.id.imageView);
            artistName = itemView.findViewById(R.id.artistNameTextView);
            followers = itemView.findViewById(R.id.followersTextView);
            addedDate = itemView.findViewById(R.id.addedDateTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            addButton = itemView.findViewById(R.id.add_button);
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
        FavoriteArtist favoriteArtist = new FavoriteArtist(artist);

        if (artist.artworkUrl != null && !artist.artworkUrl.isEmpty()) {
            String transitionName = transitionNameForm  + holder.getAdapterPosition()  + "_" + artist.artistName + "_" + artist.artistId  + "_" + artist.artworkUrl;
            ViewCompat.setTransitionName(holder.image, transitionName);
            Log.d("FavoriteArtistAdapter", "transition name for position " + holder.getAdapterPosition() + " is " + transitionName);

        } else {
            ViewCompat.setTransitionName(holder.image, null);
        }

        holder.artistName.setText(artist.artistName);
        holder.followers.setText(NumberUtils.formatWithComma(artist.followers));
        //String addedDate = viewModel.getAddedDateAsync(artist.artistId);

        // 비동기로 날짜 가져오기
        viewModel.getAddedDateAsync(artist.artistId, addedDate -> {
            holder.addedDate.setText(addedDate != null ? addedDate : "날짜 없음");
            favoriteArtist.addedDate = addedDate;
        });

        Picasso.get()
                .load(artist.artworkUrl)
                .error(R.drawable.ic_image_not_found_foreground)
                .into(holder.image);

        holder.deleteButton.setOnClickListener(v -> deleteClickListener.onItemClick(artist));

        holder.itemView.setOnClickListener(v -> {
            if (navigateClickListener != null && ViewCompat.getTransitionName(holder.image) != null){
                navigateClickListener.onNavigateClick(
                        favoriteArtist,
                        holder.image,
                        transitionNameForm,
                        holder.getAdapterPosition()
                );
            }
        });

        holder.addButton.setOnClickListener(v -> metadataClickListener.onItemClick(artist.artistId));

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
