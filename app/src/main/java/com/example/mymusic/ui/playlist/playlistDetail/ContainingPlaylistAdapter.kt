package com.example.mymusic.ui.playlist.playlistDetail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.model.Playlist
import com.google.android.material.card.MaterialCardView

class ContainingPlaylistAdapter(
    var playlists: List<Playlist>,
    val listener: OnClickListener
) : RecyclerView.Adapter<ContainingPlaylistAdapter.ViewHolder>() {

    interface OnClickListener{
        fun onItemClick(playlist: Playlist)
        fun onPlayButtonClick(playlist: Playlist)
        fun onShuffleButtonClick(playlist: Playlist)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_containing_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = playlists.get(position)

        holder.playlistName.text = item.playlistName
        holder.trackCountTextView.text = item.trackIds.size.toString() + "곡"
        holder.playTimeTextView.text = item.getDurationStr()
        holder.shuffleButton.setOnClickListener { listener.onShuffleButtonClick(item) }
        holder.playButton.setOnClickListener { listener.onPlayButtonClick(item) }
        holder.itemView.setOnClickListener { listener.onItemClick(item) }
    }

    override fun getItemCount(): Int = playlists.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Playlist>) {
        playlists = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val playlistName = view.findViewById<TextView>(R.id.playlist_name)
        val trackCountTextView = view.findViewById<TextView>(R.id.track_count_text)
        val playTimeTextView = view.findViewById<TextView>(R.id.playlist_play_time_text)
        val shuffleButton = view.findViewById<ImageButton>(R.id.playlist_shuffle_button)
        val playButton = view.findViewById<ImageButton>(R.id.playlist_play_button)

    }
}