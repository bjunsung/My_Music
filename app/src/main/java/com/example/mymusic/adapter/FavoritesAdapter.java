package com.example.mymusic.adapter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {
    private boolean isSelectionMode = false;
    List<Favorite> favoritesList;
    OnDeleteClickListener deleteClickListener;
    OnLyricClickListener lyricClickListener;
    OnLyricLongClickListener lyricLongClickListener;
    OnItemLongClickListener itemLongClickListener;
    long lastClickTime = 0;
    public interface OnDeleteClickListener{
        void onItemClick(Favorite favorite);
    }

    public interface OnLyricClickListener{
        void onItemClick(String trackId, String trackName);
    }

    public interface OnLyricLongClickListener{
        void onItemClick(String trackId, String trackName);
    }

    public interface OnItemLongClickListener{
        void onItemClick();
    }

    public FavoritesAdapter(List<Favorite> favoritesList,
                            OnDeleteClickListener deleteClickListener,
                            OnLyricClickListener lyricClickListener,
                            OnLyricLongClickListener lyricLongClickListener,
                            OnItemLongClickListener itemLongClickListener){
        this.favoritesList = favoritesList;
        this.deleteClickListener = deleteClickListener;
        this.lyricClickListener = lyricClickListener;
        this.lyricLongClickListener = lyricLongClickListener;
        this.itemLongClickListener = itemLongClickListener;
    }

    public class FavoriteViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView title, titleKr, artist, album, duration, releasedDate, addedDate;
        ImageButton deleteButton, lyricButton;
        CheckBox selectCheckBox;
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
        favorite.recyclerViewPosition = holder.getAdapterPosition();
        Track track = favorite.track;
        String transitionName = "Transition_favorite_adapter_to_music" + track.artworkUrl + track.trackId + track.durationMs + track.releaseDate;
        holder.image.setTransitionName(transitionName);
        Picasso.get().load(track.artworkUrl).into(holder.image);
        holder.title.setText(track.trackName);
        if (favorite.metadata != null && favorite.metadata.title != null){
            holder.titleKr.setText(favorite.metadata.title);
        }
        else {
            holder.titleKr.setText(track.trackName);
        }
        holder.artist.setText(track.artistName);
        holder.album.setText(track.albumName);
        int durationSec = (int) Double.parseDouble(track.durationMs)/1000;
        String durationStr = durationSec/60 + "분 " + durationSec%60 + "초";
        holder.duration.setText(durationStr);
        holder.addedDate.setText(favorite.addedDate);
        holder.releasedDate.setText(track.releaseDate);
        // ✅ 체크박스 표시 여부
        holder.selectCheckBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);

        // ✅ 체크 상태 설정
        if (holder.selectCheckBox.getVisibility() == View.VISIBLE) {
            holder.selectCheckBox.setChecked(favorite.isSelected);
        }

        holder.deleteButton.setOnClickListener(v -> {
            deleteClickListener.onItemClick(favorite);
        });

        //lyrics button 클릭 이벤트
        holder.lyricButton.setOnClickListener(v -> {
            lyricClickListener.onItemClick(track.trackId, track.trackName);
        });

        //lyrics button long 클릭 이벤트
        holder.lyricButton.setOnLongClickListener(v -> {
            lyricLongClickListener.onItemClick(track.trackId,  track.trackName);
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
                Bundle bundle = new Bundle();
                bundle.putParcelable("favorite", favorite);
                bundle.putString("transitionName", transitionName);

                FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                        .addSharedElement(holder.image, transitionName)
                        .build();

                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_favoritesFragment_to_musicInfoFragment, bundle, null, extras);
            }
        });

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

    }

    public boolean isSelectionMode() {
        return isSelectionMode;
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
