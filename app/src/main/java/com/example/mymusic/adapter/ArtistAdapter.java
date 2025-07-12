package com.example.mymusic.adapter;

import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mymusic.R;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.util.NumberUtils;
import com.example.mymusic.model.Artist;

import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    private List<Artist> artists;
    private Context context;
    private final String transitionNameForm = "transition_starts_at_artist_adapter_(POSITION_AT_";
    OnDetailClickListener detailClickListener;
    OnAddClickListener addClickListener;
    OnArtistClickListener artistClickListener;
    FavoriteArtistViewModel viewModel;

    public interface OnArtistClickListener{
        void onItemClick(Artist artist, ImageView sharedImageView, int position,  String transitionNameForm);

    }
    public interface OnDetailClickListener {
        void onClickItem(Artist artist);
    }

    public interface OnAddClickListener {
        void onClickItem(Artist artist, int position);
    }

    public ArtistAdapter(List<Artist> artists, Context context, OnDetailClickListener detailClickListener, OnAddClickListener addClickListener, OnArtistClickListener artistClickListener) {
        this.artists = artists;
        this.context = context;
        this.detailClickListener = detailClickListener;
        this.addClickListener = addClickListener;
        this.artistClickListener = artistClickListener;
    }

    public static class ArtistViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, followersTextView;
        ImageView artworkImage;
        ImageButton detailButton, addButton;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.artistNameTextView);
            followersTextView = itemView.findViewById(R.id.followersTextView);
            artworkImage = itemView.findViewById(R.id.imageView);
            detailButton = itemView.findViewById(R.id.showDetailButton);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }

    @NonNull
    @Override
    public ArtistAdapter.ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_artist, parent, false);
        Context context = parent.getContext(); // Adapter에 전달받은 context
        Application app = (Application) context.getApplicationContext();
        viewModel = new FavoriteArtistViewModel(app);
        return new ArtistAdapter.ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artists.get(position);

        holder.addButton.setVisibility(View.VISIBLE);
        viewModel.loadFavoriteArtistByArtistId(artist.artistId, favoriteArtist -> {
            if (favoriteArtist != null){
                holder.addButton.setVisibility(View.GONE);
            }
        });

        holder.nameTextView.setText(artist.artistName);
        //String followers = NumberFormat.getNumberInstance().format(artist.followers);
        String followers = NumberUtils.formatWithComma(artist.followers);
        holder.followersTextView.setText(followers);

        String transitionName = transitionNameForm + holder.getAdapterPosition() + "_" + artist.artistName + "_" + artist.artistId + "_" + artist.artworkUrl;
        ViewCompat.setTransitionName(holder.artworkImage, transitionName);

        if (artist.artworkUrl != null && !artist.artworkUrl.isEmpty()) {
            Glide.with(context)
                    .load(artist.artworkUrl)
                    .override(120, 120)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    //.placeholder(R.drawable.default_artist_image) // 로딩 중 보여줄 이미지
                    .error(R.drawable.ic_image_not_found_foreground)       // 실패 시 보여줄 이미지
                    .into(holder.artworkImage);
        } else {
            holder.artworkImage.setImageResource(R.drawable.ic_image_not_found_foreground); // 기본 이미지로 대체
        }

        holder.detailButton.setOnClickListener(v -> {
            detailClickListener.onClickItem(artist);
        });
        holder.addButton.setOnClickListener(v -> addClickListener.onClickItem(artist, holder.getAdapterPosition()));


        holder.itemView.setOnClickListener(v -> {
            artistClickListener.onItemClick(artist, holder.artworkImage, holder.getAdapterPosition(), transitionNameForm);
        });


    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    public void updateData(List<Artist> newList){
        this.artists = newList;
        notifyDataSetChanged();
    }


}
