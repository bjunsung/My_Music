package com.example.mymusic.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    private List<Artist> artists;
    private Context context;

    OnDetailClickListener detailClickListener;
    OnAddClickListener addClickListener;

    public interface OnDetailClickListener{
        void onClickItem(Artist artist);
    }

    public interface OnAddClickListener{
        void onClickItem(Artist artist);
    }
    public ArtistAdapter(List<Artist> artists, Context context, OnDetailClickListener detailClickListener, OnAddClickListener addClickListener){
        this.artists = artists;
        this.context = context;
        this.detailClickListener = detailClickListener;
        this.addClickListener = addClickListener;
    }
    public static class ArtistViewHolder extends RecyclerView.ViewHolder{
        TextView nameTextView, followersTextView;
        ImageView imageTextView;
        ImageButton detailButton, addButton;
        public ArtistViewHolder(@NonNull View itemView){
            super(itemView);
            nameTextView = itemView.findViewById(R.id.artistNameTextView);
            followersTextView = itemView.findViewById(R.id.followersTextView);
            imageTextView = itemView.findViewById(R.id.imageView);
            detailButton = itemView.findViewById(R.id.showDetailButton);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }

    @NonNull
    @Override
    public ArtistAdapter.ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_artist, parent, false);
        return new ArtistAdapter.ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position){
        Artist artist = artists.get(position);
        holder.nameTextView.setText(artist.artistName);
        //String followers = NumberFormat.getNumberInstance().format(artist.followers);
        DecimalFormat formatter = new DecimalFormat("#,###");
        String followers = formatter.format(artist.followers);
        holder.followersTextView.setText(followers);

        if (artist.artworkUrl != null && !artist.artworkUrl.isEmpty()) {
            Picasso.get()
                    .load(artist.artworkUrl)
                    //.placeholder(R.drawable.default_artist_image) // 로딩 중 보여줄 이미지
                    .error(R.drawable.ic_image_not_found_foreground)       // 실패 시 보여줄 이미지
                    .into(holder.imageTextView);
        } else {
            holder.imageTextView.setImageResource(R.drawable.ic_image_not_found_foreground); // 기본 이미지로 대체
        }

        holder.detailButton.setOnClickListener(v -> {detailClickListener.onClickItem(artist);});
        holder.addButton.setOnClickListener(v -> addClickListener.onClickItem(artist));
    }

    @Override
    public int getItemCount(){
        return artists.size();
    }

}
