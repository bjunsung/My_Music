package com.example.mymusic.adapter;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
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

import java.util.ArrayList;
import java.util.List;

public class FavoritesWithCardViewAdapter extends RecyclerView.Adapter<FavoritesWithCardViewAdapter.FavoritesWithCardViewHolder> {
    private final String TAG = "FavoritesWithCardViewAdapter";
    private boolean isSelectionMode = false;
    List<Favorite> favoritesList;
    OnDeleteClickListener deleteClickListener;
    OnLyricClickListener lyricClickListener;
    OnLyricLongClickListener lyricLongClickListener;
    OnItemLongClickListener itemLongClickListener;
    OnItemNavigateClickListener navigateClickListener;
    private final int invalidColor = -2;
    private int textColor = invalidColor;
    private int backgroundColor = invalidColor;
    private boolean removeButtonVisibilityGone = false;
    private Context context;

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    long lastClickTime = 0;

    public void setRemoveButtonVisibilityGone(boolean removeButtonVisibilityGone) {
        this.removeButtonVisibilityGone = removeButtonVisibilityGone;
    }


    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public interface OnDeleteClickListener{
        void onItemClick(Favorite favorite);
    }

    public interface OnLyricClickListener{
        void onItemClick(String trackId, String trackName, int position);
    }

    public interface OnLyricLongClickListener{
        void onItemClick(String trackId, String trackName);
    }

    public interface OnItemLongClickListener{
        void onItemClick();
    }

    public interface OnItemNavigateClickListener {
        void onNavigateClick(Favorite favorite, ImageView sharedImageView, int position);
    }
    public FavoritesWithCardViewAdapter(Context context, List<Favorite> favoritesList,
                                        OnDeleteClickListener deleteClickListener,
                                        OnLyricClickListener lyricClickListener,
                                        OnLyricLongClickListener lyricLongClickListener,
                                        OnItemLongClickListener itemLongClickListener,
                                        OnItemNavigateClickListener navigateClickListener){
        this.favoritesList = favoritesList;
        this.deleteClickListener = deleteClickListener;
        this.lyricClickListener = lyricClickListener;
        this.lyricLongClickListener = lyricLongClickListener;
        this.itemLongClickListener = itemLongClickListener;
        this.navigateClickListener = navigateClickListener;
        this.context = context;
    }

    public class FavoritesWithCardViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView title, titleKr, artist, album, duration, releasedDate, addedDate;
        ImageButton deleteButton, lyricButton;
        CheckBox selectCheckBox;
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
            deleteButton = itemView.findViewById(R.id.deleteButton);
            lyricButton = itemView.findViewById(R.id.lyric_button);
            selectCheckBox = itemView.findViewById(R.id.select_checkbox);
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
            ImageColorAnalyzer.analyzePrimaryColor(context, track.artworkUrl, new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
                @Override
                public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                    int darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f);
                    int adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor);
                    holder.containerCardView.setCardBackgroundColor(adjustedForWhiteText);
                    //holder.containerCardView.setCardBackgroundColor(primaryColor);
                }

                @Override
                public void onFailure() {
                    Log.d(TAG, "Fail to analyze primary color");
                }
            });

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
        // ✅ 체크박스 표시 여부
        holder.selectCheckBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);

        // ✅ 체크 상태 설정
        if (holder.selectCheckBox.getVisibility() == View.VISIBLE) {
            holder.selectCheckBox.setChecked(favorite.isSelected);
        }

        if (removeButtonVisibilityGone){
            holder.deleteButton.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(v -> {
            deleteClickListener.onItemClick(favorite);
        });

        if (textColor != invalidColor){
            holder.deleteButton.setColorFilter(textColor);

        }

        //lyrics button 클릭 이벤트
        holder.lyricButton.setOnClickListener(v -> {
            lyricClickListener.onItemClick(
                    track.trackId,
                    track.trackName,
                    holder.getAdapterPosition());
        });

        //아이템 클릭 이벤트
        holder.itemView.setOnClickListener(v -> {
            lyricClickListener.onItemClick(
                    track.trackId,
                    track.trackName,
                    holder.getAdapterPosition());
        });

        if (textColor != invalidColor){
            holder.lyricButton.setColorFilter(textColor);
            holder.textReleaseDate.setTextColor(textColor);
            holder.textDuration.setTextColor(textColor);
            holder.textDash.setTextColor(textColor);
        }

        //lyrics button long 클릭 이벤트
        holder.lyricButton.setOnLongClickListener(v -> {
            lyricLongClickListener.onItemClick(track.trackId, track.trackName);
            return true;
        });




        /*
        //아이템 클릭 이벤트
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (holder.selectCheckBox.isChecked())
                    favorite.isSelected = false;
                else
                    favorite.isSelected = true;
                holder.selectCheckBox.setChecked(!holder.selectCheckBox.isChecked());
                itemLongClickListener.onItemClick();
            }
            else {
                if (navigateClickListener != null && ViewCompat.getTransitionName(holder.image) != null){
                    navigateClickListener.onNavigateClick(
                            favorite,
                            holder.image,
                            holder.getAdapterPosition()
                    );
                }
            }
        });

         */

        /*
        //아이템 롤클릭 이벤트
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                setSelectionMode(true);
                itemLongClickListener.onItemClick();
            }

            if (!holder.selectCheckBox.isChecked()) {
                holder.selectCheckBox.setChecked(true);     // UI 갱신
                favorite.isSelected = true;                 // 상태 반영
                favorite.recyclerViewPosition = holder.getAdapterPosition();
                itemLongClickListener.onItemClick();
            }

            return true;
        });



        holder.selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // 프로그래밍적으로 변경된 경우 무시
            favorite.isSelected = isChecked;
            favorite.recyclerViewPosition = holder.getAdapterPosition();
            itemLongClickListener.onItemClick();
        });
 */
    }


    public List<Favorite> getSelectedList(){
        List<Favorite> selectedList = new ArrayList<>();
        for (Favorite item : favoritesList){
            if (item.isSelected)
                selectedList.add(item);
        }
        return selectedList;
    }

    public int getSelectedSize(){
        List<Favorite> selectedList = new ArrayList<>();
        for (Favorite item : favoritesList){
            if (item.isSelected)
                selectedList.add(item);
        }
        return selectedList.size();
    }


    public void setSelectionMode(boolean enable) {
        isSelectionMode = enable;
        notifyDataSetChanged(); // 전체 갱신
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
