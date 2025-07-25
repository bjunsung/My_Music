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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mymusic.R;
import com.example.mymusic.main.playlist.PlaylistTrackAdapter;
import com.example.mymusic.model.Favorite;

import com.example.mymusic.model.Track;
import com.example.mymusic.util.DarkModeUtils;
import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.MyColorUtils;
import com.google.android.material.card.MaterialCardView;


import java.util.List;
import java.util.Objects;

public class FavoritesWithCardViewAdapter extends RecyclerView.Adapter<FavoritesWithCardViewAdapter.FavoritesWithCardViewHolder> {
    private final String TAG = "FavoritesWithCardViewAdapter";
    List<Favorite> favoritesList;
    private final int invalidColor = -2;
    private int textColor = invalidColor;
    private int backgroundColor = invalidColor;
    private Context context;
    private OnItemClickListener itemClickListener;
    private boolean favoritesColorUnification = false;

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }


    public interface OnItemClickListener{
        void onItemClick(String trackId, String trackName, String albumName, String artistName, int position);
    }

    public void setPrimaryBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }



    public FavoritesWithCardViewAdapter(Context context,
                                        List<Favorite> favoritesList,
                                        OnItemClickListener itemClickListener,
                                        boolean favoritesColorUnification
                                        ){
        this.favoritesList = favoritesList;
        this.context = context;
        this.itemClickListener = itemClickListener;
        this.favoritesColorUnification = favoritesColorUnification;
    }

    public class FavoritesWithCardViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView title, titleKr, artist, album, duration, releasedDate, addedDate;
        TextView textDash, textDuration, textReleaseDate;
        MaterialCardView containerCardView;
        public FavoritesWithCardViewHolder(@NonNull View itemView){
            super(itemView);
            image = itemView.findViewById(R.id.imageView);
            title = itemView.findViewById(R.id.titleTextView);
            titleKr = itemView.findViewById(R.id.titleKRTextView);
            artist = itemView.findViewById(R.id.artistTextView);
            album = itemView.findViewById(R.id.albumTextView);
            duration = itemView.findViewById(R.id.durationTextView);
            releasedDate = itemView.findViewById(R.id.releaseDateTextView);
            addedDate = itemView.findViewById(R.id.addedDateTextView);
            textDash = itemView.findViewById(R.id.text_dash);
            textDuration = itemView.findViewById(R.id.text_duration);
            textReleaseDate = itemView.findViewById(R.id.text_release_date);
            containerCardView = itemView.findViewById(R.id.item_container_card_view);
        }
    }

    @NonNull
    @Override
    public FavoritesWithCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_with_cardview, parent, false);
        context = parent.getContext();
        return new FavoritesWithCardViewHolder(view);
    }

    private void setBackgroundColor(FavoritesWithCardViewHolder holder, int primaryColor) {
        int darkenColor;
        if (DarkModeUtils.isDarkMode(context)){
            darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.45f);
        }else{
            darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f);
        }
        int adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor);
        int blended = MyColorUtils.blendColors(adjustedForWhiteText, backgroundColor, 0.3141592f);
        holder.containerCardView.setCardBackgroundColor(blended);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesWithCardViewHolder holder, int position){

        Favorite favorite = favoritesList.get(position);
        favorite.recyclerViewPosition = holder.getAdapterPosition();
        Track track = favorite.track;
        if (track.artworkUrl != null && !track.artworkUrl.isEmpty()) {
            String transitionName = "Transition_favorite_adapter_to_music_"  + track.artworkUrl + "_" + track.trackId + "_" + track.durationMs + "_" + track.releaseDate + "_" + position;
            ViewCompat.setTransitionName(holder.image, transitionName);
        } else {
            ViewCompat.setTransitionName(holder.image, null);
        }


        if (context != null) {
            if (favoritesColorUnification){
               // holder.containerCardView.setCardBackgroundColor(backgroundColor);
                setBackgroundColor(holder, backgroundColor);
                favorite.backgroundColor = backgroundColor;
            }else if (track.primaryColor != null) {
                setBackgroundColor(holder, track.primaryColor);
            }
            else{
                ImageColorAnalyzer.analyzePrimaryColor(context, track.artworkUrl, new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
                    @Override
                    public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                        setBackgroundColor(holder, primaryColor);
                    }
                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Fail to analyze primary color");
                    }
                });
            }
        }


        Glide.with(holder.itemView)
                .load(track.artworkUrl)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(holder.image);


        holder.title.setText(track.trackName);
        if (favorite.metadata != null && favorite.metadata.title != null){
            holder.titleKr.setText(favorite.metadata.title);
            if (textColor != invalidColor){
                holder.titleKr.setTextColor(textColor);
            }
        }
        else {
            holder.titleKr.setText(track.trackName);
            if (textColor != invalidColor){
                holder.titleKr.setTextColor(textColor);
            }
        }


        holder.artist.setText(track.artistName);
        if (textColor != invalidColor){
            holder.artist.setTextColor(textColor);
        }
        holder.album.setText(track.albumName);
        if (textColor != invalidColor){
            holder.album.setTextColor(textColor);
        }
        int durationSec = (int) Double.parseDouble(track.durationMs)/1000;
        String durationStr = durationSec/60 + "분 " + durationSec%60 + "초";
        holder.duration.setText(durationStr);
        if (textColor != invalidColor){
            holder.duration.setTextColor(textColor);
        }
        holder.addedDate.setText(favorite.addedDate);
        if (textColor != invalidColor){
            holder.addedDate.setTextColor(textColor);
        }
        holder.releasedDate.setText(track.releaseDate);
        if (textColor != invalidColor){
            holder.releasedDate.setTextColor(textColor);
        }


        if (textColor != invalidColor){
            holder.textReleaseDate.setTextColor(textColor);
            holder.textDuration.setTextColor(textColor);
            holder.textDash.setTextColor(textColor);
        }


        holder.itemView.setOnClickListener(v -> {
            itemClickListener.onItemClick(track.trackId, track.trackName, track.albumName, track.artistName, holder.getBindingAdapterPosition());
        });
    }
    public void updateColors(){
        notifyItemRangeChanged(0, getItemCount());
    }


    @Override
    public int getItemCount() {
        return favoritesList.size();
    }

    public void updateData(List<Favorite> newList, int newColor) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new DiffUtil.Callback() {

                    @Override
                    public int getOldListSize() {
                        return favoritesList.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return newList.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        Favorite oldItem = favoritesList.get(oldItemPosition);
                        Favorite newItem = newList.get(newItemPosition);
                        return Objects.equals(oldItem.track.trackId, newItem.track.trackId);
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        Favorite oldItem = favoritesList.get(oldItemPosition);
                        Favorite newItem = newList.get(newItemPosition);
                        return Objects.equals(oldItem.track.trackId, newItem.track.trackId) &&
                                Objects.equals(FavoritesWithCardViewAdapter.this.backgroundColor, newColor);
                    }
                }
        );
        this.favoritesList = newList;
        diff.dispatchUpdatesTo(this);
    }



}
