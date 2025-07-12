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

import com.example.mymusic.R;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.MyColorUtils;
import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import java.util.List;

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
        void onItemClick(String trackId, String trackName, int position);
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
        return new FavoritesWithCardViewHolder(view);
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
                holder.containerCardView.setCardBackgroundColor(backgroundColor);
            }else {
                ImageColorAnalyzer.analyzePrimaryColor(context, track.artworkUrl, new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
                    @Override
                    public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                        int darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f);
                        int adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor);
                        int blended = MyColorUtils.blendColors(adjustedForWhiteText, backgroundColor, 0.3141592f);
                        holder.containerCardView.setCardBackgroundColor(blended);
                    }
                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Fail to analyze primary color");
                    }
                });
            }
        }


        if (backgroundColor != invalidColor){
            holder.containerCardView.setCardBackgroundColor(backgroundColor);
        }

        Picasso.get().load(track.artworkUrl).into(holder.image);
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
            itemClickListener.onItemClick(track.trackId, track.trackName, holder.getAdapterPosition());
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
