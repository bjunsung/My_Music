package com.example.mymusic.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.mymusic.R;

import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.model.FavoriteArtistDiffCallback;
import com.example.mymusic.util.NumberUtils;
import com.example.mymusic.model.Artist;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;


import java.util.List;


public class FavoriteArtistAdapter extends RecyclerView.Adapter<FavoriteArtistAdapter.FavoriteArtistViewHolder> {
    private final String TAG = "FavoriteArtistAdapter";
    List<FavoriteArtist> favoriteArtistList;
    OnDeleteClickListener deleteClickListener;
    OnMetadataClickListener metadataClickListener;
    FavoriteArtistViewModel viewModel;
    OnItemNavigateClickListener navigateClickListener;
    private final String transitionNameForm = "Transition_favorite_artist_adapter_to_artist_";
    private boolean isSelectionMode = false;
    private OnAddSelectedListener addSelectedListener;
    private OnRemoveSelectedListener removeSelectedListener;
    private OnMetadataLongClickListener metadataLongClickListener;

    private Context context;
    public interface OnMetadataLongClickListener{
        void onItemLongClick(String artistId, String artistName);
    }
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
        void onItemClick(String artistId, String artistName);
    }

    public interface OnItemNavigateClickListener {
        void onNavigateClick(FavoriteArtist favorite, ImageView sharedImageView, String transitionNameForm, int position);
    }

    public FavoriteArtistAdapter(List<FavoriteArtist> favoriteArtistList,
                                 OnDeleteClickListener deleteClickListener,
                                 OnMetadataClickListener metadataClickListener,
                                 OnMetadataLongClickListener metadataLongClickListener,
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
        this.metadataLongClickListener = metadataLongClickListener;
    }

    public static class FavoriteArtistViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView artistName, followers, addedDate;
        ImageButton deleteButton, addButton, imageAlbumButton;
        CheckBox selectCheckBox;
        LinearLayout debutLayout;
        TextView debutDateTextView;
        private LinearLayout additionalLayout;
        private TextView additionalStatement, additionalInfo;

        int position = -1;

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
            imageAlbumButton = itemView.findViewById(R.id.image_album_button);
            debutLayout = itemView.findViewById(R.id.debut_layout);
            debutDateTextView = itemView.findViewById(R.id.debut_date);
            additionalLayout = itemView.findViewById(R.id.additional_info_layout);
            additionalStatement = itemView.findViewById(R.id.additional_statement);
            additionalInfo = itemView.findViewById(R.id.additional_info);
        }
    }

    @NonNull
    @Override
    public FavoriteArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_artist, parent, false);
        context = parent.getContext();
        return new FavoriteArtistViewHolder(view);
    }




    @Override
    public void onBindViewHolder(@NonNull FavoriteArtistViewHolder holder, int position){
        FavoriteArtist favoriteArtist = favoriteArtistList.get(position);
        Artist artist = favoriteArtist.artist;

        if (artist.artworkUrl != null && !artist.artworkUrl.isEmpty()) {
            holder.position = holder.getAdapterPosition();
            String transitionName = transitionNameForm  + holder.position  + "_" + artist.artistName + "_" + artist.artistId  + "_" + artist.artworkUrl;
            ViewCompat.setTransitionName(holder.image, transitionName);
            Log.d("FavoriteArtistAdapter", "transition name for position " + holder.getAdapterPosition() + " is " + transitionName);

        } else {
            ViewCompat.setTransitionName(holder.image, null);
        }



        Glide.with(context)
                .load(artist.artworkUrl)
                .override(160, 160)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .error(R.drawable.ic_image_not_found_foreground)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "Image resourceReady for position " + holder.getAdapterPosition() + " and load from " + dataSource.toString());
                        return false;
                    }
                })
                .into(holder.image);

/*
        Glide.with(context)
                .load(artist.artworkUrl)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .error(R.drawable.ic_image_not_found_foreground)
                .preload(480, 480);

 */

        viewModel.loadArtistMetadataBySpotifyId(artist.artistId, new FavoriteArtistViewModel.MetadataCallback() {
            @Override
            public void onSuccess(ArtistMetadata metadata) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    favoriteArtist.metadata = metadata;
                    holder.addButton.setVisibility(View.GONE);
                    holder.imageAlbumButton.setVisibility(View.VISIBLE);
                    holder.debutLayout.setVisibility(View.VISIBLE);
                    if(metadata.debutDate != null && !metadata.debutDate.isEmpty()){
                        holder.debutDateTextView.setText(metadata.debutDate);
                    } else{
                        holder.debutDateTextView.setText("정보없음");
                    }
                    removeAdditionalInfo(holder);

                    setAdditionalInfo(holder, metadata);

                });
            }

            @Override
            public void onFailure(String reason) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    holder.addButton.setVisibility(View.VISIBLE);
                    holder.addButton.setColorFilter(Color.GRAY);
                    holder.imageAlbumButton.setVisibility(View.GONE);
                    holder.debutLayout.setVisibility(View.GONE);
                    removeAdditionalInfo(holder);
                    setAdditionalInfo(holder, null);
                });
            }
        });

        if (viewModel.selectedList.contains(artist)){
            holder.selectCheckBox.setChecked(true);
        }



        holder.artistName.setText(artist.artistName);
        holder.followers.setText(NumberUtils.formatWithComma(artist.followers));
        //String addedDate = viewModel.getAddedDateAsync(artist.artistId);

        // 비동기로 날짜 가져오기
        viewModel.getAddedDateAsync(artist.artistId, addedDate -> {
            holder.addedDate.setText(addedDate != null ? addedDate : "날짜 없음");
            favoriteArtist.addedDate = addedDate;
        });



        holder.deleteButton.setOnClickListener(v -> deleteClickListener.onItemClick(artist));

        holder.selectCheckBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);




        holder.itemView.setOnClickListener(v -> {
            if (!isSelectionMode) {
                if (navigateClickListener != null && ViewCompat.getTransitionName(holder.image) != null) {
                    int tempPosition = holder.getAdapterPosition();
                    if (tempPosition != holder.position){
                        Log.d(TAG + "DEBUG", "position change occurred, adjust holder.image transition name from old position(" +  holder.position + ") to latest position(" + tempPosition + ")");
                        String adjustedTransitionName = transitionNameForm  + tempPosition  + "_" + artist.artistName + "_" + artist.artistId  + "_" + artist.artworkUrl;
                        ViewCompat.setTransitionName(holder.image, adjustedTransitionName);
                    }
                    navigateClickListener.onNavigateClick(
                            favoriteArtist,
                            holder.image,
                            transitionNameForm,
                            tempPosition
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

        holder.addButton.setOnClickListener(v -> metadataClickListener.onItemClick(artist.artistId, artist.artistName));
        holder.addButton.setOnLongClickListener(v -> {
            Log.d(TAG, "artist: " + artist);
            Log.d(TAG + "Debug", "artist id: " + artist.artistId);
            Log.d(TAG + "Debug", "artist name: " + artist.artistName);
            metadataLongClickListener.onItemLongClick(artist.artistId, artist.artistName);
            return true;
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                setSelectionMode(true);
            }
            if (!holder.selectCheckBox.isChecked()) {
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

    private void removeAdditionalInfo(@NonNull FavoriteArtistViewHolder holder){
        holder.additionalLayout.setVisibility(View.GONE);
    }

    private void setAdditionalInfo(@NonNull FavoriteArtistViewHolder holder, ArtistMetadata metadata){
        SharedPreferences prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
        String filterOption = prefs.getString("sort_option", "ADDED_DATE");
        switch (filterOption) {
            case "MEMBER_COUNTS":
                if (metadata != null && metadata.members != null && !metadata.members.isEmpty()) {
                    holder.additionalLayout.setVisibility(View.VISIBLE);
                    holder.additionalStatement.setText("멤버");
                    StringBuilder memberCount = new StringBuilder();
                    memberCount.append(metadata.members.size());
                    memberCount.append(" 명");
                    holder.additionalInfo.setText(memberCount);
                }
                break;
            case "IMAGE_COUNTS":
                holder.additionalLayout.setVisibility(View.VISIBLE);
                holder.additionalStatement.setText("PHOTO");
                if (metadata != null && metadata.images != null && !metadata.images.isEmpty()) {
                    int imageCount = metadata.images.size();
                    holder.additionalInfo.setText(String.valueOf(imageCount));
                } else{
                    holder.additionalInfo.setText("1");
                }
                break;
            default:
                holder.additionalStatement.setText("OTHER CASE");
                holder.additionalInfo.setText("OTHER CASE");
                holder.additionalLayout.setVisibility(View.GONE);
                break;
        }
    }

    public void updateData(List<FavoriteArtist> newList){
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new FavoriteArtistDiffCallback(this.favoriteArtistList, newList, 0, 0)
        );
        this.favoriteArtistList = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateData(List<FavoriteArtist> newList, int oldSortOpt, int newSortOpt){

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new FavoriteArtistDiffCallback(this.favoriteArtistList, newList, oldSortOpt, newSortOpt)
        );
        this.favoriteArtistList = newList;
        diffResult.dispatchUpdatesTo(this);
    }



    public void setSelectionMode(boolean selectionMode){
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }


    public Context getRecyclerViewContext(){
        return context;
    }

}
