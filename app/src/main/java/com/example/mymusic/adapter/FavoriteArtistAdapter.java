package com.example.mymusic.adapter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.model.FavoriteArtistDiffCallback;
import com.example.mymusic.util.NumberUtils;
import com.example.mymusic.model.Artist;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class FavoriteArtistAdapter extends RecyclerView.Adapter<FavoriteArtistAdapter.FavoriteArtistViewHolder> {

    List<FavoriteArtist> favoriteArtistList;
    OnDeleteClickListener deleteClickListener;
    OnMetadataClickListener metadataClickListener;
    FavoriteArtistViewModel viewModel;
    OnItemNavigateClickListener navigateClickListener;
    private final String transitionNameForm = "Transition_favorite_artist_adapter_to_artist_";
    private boolean isSelectionMode = false;
    private OnAddSelectedListener addSelectedListener;
    private OnRemoveSelectedListener removeSelectedListener;
    public interface OnAddSelectedListener{
        void onItemClick(Artist artist);
    }

    public interface OnRemoveSelectedListener{
        void onItemClick(Artist artist);
    }

    public interface OnDeleteClickListener{
        void onItemClick(Artist artist);
    }

    public interface OnMetadataClickListener{
        void onItemClick(String artistId);
    }

    public interface OnItemNavigateClickListener {
        void onNavigateClick(FavoriteArtist favorite, ImageView sharedImageView, String transitionNameForm, int position);
    }

    public FavoriteArtistAdapter(List<FavoriteArtist> favoriteArtistList,
                                 OnDeleteClickListener deleteClickListener,
                                 OnMetadataClickListener metadataClickListener,
                                 FavoriteArtistViewModel viewModel,
                                 OnItemNavigateClickListener navigateClickListener,
                                 OnAddSelectedListener addSelectedListener,
                                 OnRemoveSelectedListener removeSelectedListener){
        this.favoriteArtistList = favoriteArtistList;
        this.deleteClickListener = deleteClickListener;
        this.metadataClickListener = metadataClickListener;
        this.viewModel = viewModel;
        this.navigateClickListener = navigateClickListener;
        this.addSelectedListener = addSelectedListener;
        this.removeSelectedListener = removeSelectedListener;
    }
    public static class FavoriteArtistViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView artistName, followers, addedDate;
        ImageButton deleteButton, addButton;
        CheckBox selectCheckBox;

        public int recyclerViewPosition = -1;
        public FavoriteArtistViewHolder(@NonNull View itemView){
            super(itemView);
            image = itemView.findViewById(R.id.imageView);
            artistName = itemView.findViewById(R.id.artistNameTextView);
            followers = itemView.findViewById(R.id.followersTextView);
            addedDate = itemView.findViewById(R.id.addedDateTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            addButton = itemView.findViewById(R.id.add_button);
            selectCheckBox = itemView.findViewById(R.id.select_checkbox);
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
        FavoriteArtist favorite = favoriteArtistList.get(position);
        Artist artist = favorite.artist;
        FavoriteArtist favoriteArtist = new FavoriteArtist(artist);

        if (viewModel.selectedList.contains(artist)){
            holder.selectCheckBox.setChecked(true);
        }

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

        holder.selectCheckBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (!isSelectionMode) {
                if (navigateClickListener != null && ViewCompat.getTransitionName(holder.image) != null) {
                    navigateClickListener.onNavigateClick(
                            favoriteArtist,
                            holder.image,
                            transitionNameForm,
                            holder.getAdapterPosition()
                    );
                }
            }
            else{
                if (holder.selectCheckBox.isChecked()){
                    holder.selectCheckBox.setChecked(false);
                    removeSelectedListener.onItemClick(artist);
                }
                else{
                    holder.selectCheckBox.setChecked(true);
                    addSelectedListener.onItemClick(artist);
                }
            }
        });

        holder.addButton.setOnClickListener(v -> metadataClickListener.onItemClick(artist.artistId));

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                setSelectionMode(true);
            }
            if (!holder.selectCheckBox.isChecked()) {
                Log.d("sex", "sex");
                holder.selectCheckBox.setChecked(true);
                addSelectedListener.onItemClick(artist);// UI 갱신// 상태 반영
            }

            return true;
        });

        holder.selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // 프로그래밍적으로 변경된 경우 무시

            if(isChecked){
                addSelectedListener.onItemClick(artist);
            }
            else{
                removeSelectedListener.onItemClick(artist);
            }
        });

    }

    @Override
    public int getItemCount() {
        return favoriteArtistList.size();
    }


    /*
    public void updateData(List<Artist> newList){
        this.artistList = newList;
        notifyDataSetChanged();
    }
     */

    public void updateData(List<FavoriteArtist> newList){
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FavoriteArtistDiffCallback(this.favoriteArtistList, newList));
        this.favoriteArtistList = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    public void setSelectionMode(boolean selectionMode){
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }




}
