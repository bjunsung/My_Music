package com.example.mymusic.ui.albumInfo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.mymusic.R;
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.model.Album;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.example.mymusic.util.EdgeSwipeBackGestureHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AlbumInfoFragment extends Fragment {

    private Album album;

    private ArtistApiHelper apiHelper;
    private TrackAdapter trackAdapter;
    private FavoritesViewModel favoritesViewModel;



    //View
    private ImageView albumImageView;
    private TextView albumNameTextView, artistNameTextView, releaseDateTextView, totalTracksTextView;
    private RecyclerView trackRecyclerView;


    @Override
    public void onCreate(@Nullable Bundle savedInstatceState){
        super.onCreate(savedInstatceState);

        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_album_info, container, false);
        new EdgeSwipeBackGestureHelper().attachToView(view.findViewById(R.id.gesture_overlay), view.findViewById(R.id.swipe_content) , this);
        setSharedElementEnterTransition(new ChangeBounds().setDuration(300));
        setSharedElementReturnTransition(new ChangeBounds().setDuration(300));
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);


        LinearLayout metadata_container = view.findViewById(R.id.metadata_container);
        Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_bottom);
        metadata_container.startAnimation(anim);

        //View connection
        bindView(view);

        apiHelper = new ArtistApiHelper(getContext(), requireActivity());

        apiHelper.searchTrackByAlbum(album, 0, trackList -> {
            trackAdapter = new TrackAdapter(trackList, getContext(), this::showTrackDetails, this::addFavoriteSong);
            trackAdapter.setShowImage(false);
            trackAdapter.setShowPosition(true);
            trackRecyclerView.setAdapter(trackAdapter);

        });

        setView();

    }

    public void artistClickEvent(String artistId){
        apiHelper.getArtist(artistId, 0, artist -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable("artist", artist);
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_albumInfoFragment_to_artist_infoFragment, bundle);
        });

    }

    public void trackItemClickEvent(Favorite favorite){
        Bundle bundle = new Bundle();
        bundle.putParcelable("favorite", favorite);
        NavController navController = Navigation.findNavController((requireView()));
        navController.navigate(R.id.action_searchFragment_to_musicInfoFragment, bundle);
    }

    private void bindView(View view){
        albumNameTextView = view.findViewById(R.id.album_name);
        artistNameTextView = view.findViewById(R.id.artist_name);
        releaseDateTextView = view.findViewById(R.id.release_date);
        totalTracksTextView = view.findViewById(R.id.total_tracks);
        trackRecyclerView = view.findViewById(R.id.track_result_recycler_view);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        album = getArguments().getParcelable("album");
        albumImageView = view.findViewById(R.id.artwork_image);
        albumImageView.setTransitionName("music_info_to_album_info");
    }

    private void setView(){

        postponeEnterTransition();
        Glide.with(requireContext())
                .load(album.artworkUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_image_not_found_foreground)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        albumImageView.setImageDrawable(resource);
                        startPostponedEnterTransition(); // ✅ 성공 시 전환 시작
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 필요하면 placeholder 정리
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        albumImageView.setImageResource(R.drawable.ic_image_not_found_foreground);
                        startPostponedEnterTransition(); // ✅ 실패해도 전환 시작
                    }
                });

        albumNameTextView.setText(album.albumName);
        artistNameTextView.setText(album.artistName);
        releaseDateTextView.setText(album.releaseDate);
        totalTracksTextView.setText(String.valueOf(album.totalTracks));

        artistNameTextView.setOnClickListener(v -> this.artistClickEvent(album.artistId));

    }

    private void addFavoriteSong(Track track){
        new AlertDialog.Builder(getContext())
                .setTitle("관심목록에 추가")
                .setMessage(track.trackName + " - " + track.artistName + " 을(를) Favorites List 에 추가할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        new Thread(() -> {
                            try {
                                favoritesViewModel.insert(track, today);
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                });
                            } catch (SQLiteConstraintException e) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }).start();

                    }
                })
                .show();
    }

    private void showTrackDetails(Track track) {
        new AlertDialog.Builder(getContext())
                .setTitle("세부사항")
                .setMessage("제목: " + track.trackName +
                        "\n아티스트: " + track.artistName +
                        "\n앨범: " + track.albumName +
                        "\n발매일: " + track.releaseDate.substring(0, 10))
                .setPositiveButton("닫기", null)
                .show();
    }


}
