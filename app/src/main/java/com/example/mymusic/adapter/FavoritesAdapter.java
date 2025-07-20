package com.example.mymusic.adapter;

import android.content.Context;
import android.graphics.Color;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mymusic.R;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteDiffCallback;
import com.example.mymusic.model.Track;

import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {
    private static final String TAG = "FavoritesAdapter";
    private boolean isSelectionMode = false;
    List<Favorite> favoritesList;
    OnDeleteClickListener deleteClickListener;
    OnLyricClickListener lyricClickListener;
    OnLyricLongClickListener lyricLongClickListener;
    OnItemLongClickListener itemLongClickListener;
    OnItemNavigateClickListener navigateClickListener;
    private final int invalidColor = -2;
    private int textColor = invalidColor;
    private boolean removeButtonVisibilityGone = false;
    private Context context;

    private String keyword = null;

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    long lastClickTime = 0;

    public void setRemoveButtonVisibilityGone(boolean removeButtonVisibilityGone) {
        this.removeButtonVisibilityGone = removeButtonVisibilityGone;
    }

    public interface OnDeleteClickListener{
        void onItemClick(Favorite favorite);
    }

    public interface OnLyricClickListener{
        void onItemClick(String trackId, String trackName, String albumName, String artistName, int position);
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
    public FavoritesAdapter(List<Favorite> favoritesList,
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
    }

    public class FavoriteViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView title, titleKr, artist, album, duration, releasedDate, addedDate;
        ImageButton deleteButton, lyricButton;
        CheckBox selectCheckBox;
        TextView textDash, textDuration, textReleaseDate;
        public FavoriteViewHolder(@NonNull View itemView){
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
        }
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        context = parent.getContext();
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position){

        Favorite favorite = favoritesList.get(position);
        favorite.recyclerViewPosition = holder.getAdapterPosition();
        Track track = favorite.track;
        if (track.artworkUrl != null && !track.artworkUrl.isEmpty()) {
            String transitionName = "Transition_favorite_adapter_to_music_"  + track.artworkUrl + "_" + track.trackId + "_" + track.durationMs + "_" + track.releaseDate + "_";
            ViewCompat.setTransitionName(holder.image, transitionName);
        } else {
            ViewCompat.setTransitionName(holder.image, null);
        }



        Glide.with(context)
                .load(track.artworkUrl)
                .override(160, 160)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
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


        if (keyword != null && !keyword.isEmpty()){
            highlightText(holder.titleKr, keyword);
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

        holder.lyricButton.clearColorFilter(); // 기본값으로

        if (favorite.metadata == null || favorite.metadata.lyrics == null || favorite.metadata.lyrics.isEmpty()){
            holder.lyricButton.setColorFilter(Color.GRAY);
        }

        //lyrics button 클릭 이벤트
        holder.lyricButton.setOnClickListener(v -> {
            lyricClickListener.onItemClick(
                    track.trackId,
                    track.trackName,
                    track.albumName,
                    track.artistName,
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

        //아이템 롤클릭 이벤트
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                favorite.isSelected = true;
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

    public void setKeyword(String keyword){
        this.keyword = keyword;
<<<<<<< HEAD
        notifyDataSetChanged();
=======
        updateData(this.favoritesList);
        for (Favorite item : favoritesList){
            item.keyword = keyword;
        }
>>>>>>> b8bc983 (Preserve RecyclerView animations when highlighting search keywords by using DiffUtil with keyword in Favorite)
    }
    private void highlightText(TextView textView, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;

        String original = textView.getText().toString();
        String normalizedTitle = original.replaceAll("\\s+", "").toLowerCase();
        String normalizedKeyword = keyword.replaceAll("\\s+", "").toLowerCase();

        if (normalizedTitle.contains(normalizedKeyword)){
            int start = normalizedTitle.indexOf(normalizedKeyword);
            int end = start + normalizedKeyword.length() - 1;

            String cleaned = original.replace('\u00A0', ' ');

            for (int i=0; i <= start; ++i){
                if (cleaned.charAt(i) == ' ') {
                    start ++;
                }
            }

            for (int i=0; i <= end; ++i){
                if (cleaned.charAt(i) == ' ') {
                    end ++;
                }
            }

            SpannableString spannable = new SpannableString(original);
            spannable.setSpan(
                    new BackgroundColorSpan(Color.parseColor("#4682b4")),
                    start,
                    end + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannable.setSpan(
                    new ForegroundColorSpan(Color.WHITE),
                    start,
                    end + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            textView.setText(spannable);
            return;
        }

        // 매칭 실패 시 원래 텍스트 그대로 설정
        textView.setText(original);
    }


    public void setSelectionMode(boolean enable) {
        isSelectionMode = enable;
        notifyDataSetChanged(); // 전체 갱신
    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }


    public void updateData(List<Favorite> newList){
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new FavoriteDiffCallback(this.favoritesList, newList, keyword));
        this.favoritesList = newList;
        diffResult.dispatchUpdatesTo(this);
    }


}
