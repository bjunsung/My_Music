package com.example.mymusic.ui.musicInfo;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.mymusic.R;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.example.mymusic.util.DateUtils;
import com.example.mymusic.util.EdgeSwipeBackGestureHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MusicInfoFragment extends Fragment {
    private Favorite favorite;
    private Track track;
    FavoritesViewModel favoritesViewModel;
    TextView trackTitle, trackTitleKr, addedDate;
    LinearLayout addedDateLayout;
    private boolean savedInDb;

    //ViewModel 연결
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_music_info, container, false);

        savedInDb = false;

        //스와이프로 뒤로가기 구현
        View swipeContent =  rootView.findViewById(R.id.swipe_content);
        View gestureOverlay = rootView.findViewById(R.id.gesture_overlay);
        View backgroundView = requireActivity().findViewById(R.id.background_fragment_container);

        new EdgeSwipeBackGestureHelper().attachToView(
                gestureOverlay,
                swipeContent,
                this
        );
        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        favorite = getArguments().getParcelable("favorite");

        if (favorite != null && favorite.track != null) {
            track = favorite.track;
            TrackMetadata metadata = favorite.metadata;

            ImageView artworkImage = view.findViewById(R.id.artworkImage);
            trackTitle = view.findViewById(R.id.trackTitle);
            TextView artistName = view.findViewById(R.id.artistName);
            TextView albumName = view.findViewById(R.id.albumName);
            TextView releaseDate = view.findViewById(R.id.releaseDate);
            TextView durationMs = view.findViewById(R.id.durationMs);
            //ScrollView scrollView = view.findViewById(R.id.infoScroll);
            //scrollView.setNestedScrollingEnabled(false);

            trackTitleKr = view.findViewById(R.id.trackTitleKr);
            LinearLayout lyricistLayout = view.findViewById(R.id.lyricists_layout);
            LinearLayout composersLayout = view.findViewById(R.id.composers_layout);
            TextView lyricist = view.findViewById(R.id.lyricists);
            TextView composers = view.findViewById(R.id.composers);
            TextView lyrics = view.findViewById(R.id.metadata_lyrics);
            LinearLayout vocalistsLayout = view.findViewById(R.id.vocalists_layout);
            TextView vocalists = view.findViewById(R.id.vocalists);
            TextView daysBetween = view.findViewById(R.id.days_between);
            addedDateLayout = view.findViewById(R.id.added_date_layout);
            addedDate = view.findViewById(R.id.added_date);


            trackTitle.setText(track.trackName);
            if (metadata != null && metadata.title != null){
                trackTitleKr.setText(metadata.title);
                trackTitle.setVisibility(TextView.GONE);
                trackTitleKr.setVisibility(TextView.VISIBLE);
            }
            else {
                trackTitleKr.setVisibility(TextView.GONE);
                trackTitle.setVisibility(TextView.VISIBLE);
            }

            if (metadata != null && metadata.lyricists != null){
                lyricist.setText(String.join(", ", metadata.lyricists));
                lyricistLayout.setVisibility(TextView.VISIBLE);
            }

            if (metadata != null && metadata.composers != null){
                composers.setText(String.join(", ", metadata.composers));
                composersLayout.setVisibility(TextView.VISIBLE);
            }

            if (metadata != null && metadata.vocalists != null && !metadata.vocalists.isEmpty()){
                vocalists.setText(metadata.vocalistsToString());
                vocalistsLayout.setVisibility(TextView.VISIBLE);
            }

            if (favorite.addedDate != null && !favorite.addedDate.isEmpty()){
                addedDate.setText(favorite.addedDate);
                addedDateLayout.setVisibility(View.VISIBLE);
            }

            daysBetween.setText(String.valueOf(DateUtils.calculateDateDiffrence(track.releaseDate, DateUtils.today())));


            trackTitle.setOnClickListener(v -> {
                if (savedInDb)
                    switchTitleLanguage();
                else{
                    favoritesViewModel.loadFavoriteItem(track.trackId, loaded -> {
                        if (loaded != null) {
                            savedInDb = true;
                            switchTitleLanguage();
                        }
                    }
                    );
                }
            });
            trackTitleKr.setOnClickListener(v -> switchTitleLanguage());

            // 이미지 표시: Picasso 등 사용
            if (track.artworkUrl != null && !track.artworkUrl.isEmpty()) {
                //com.squareup.picasso.Picasso.get().load(track.artworkUrl).into(artworkImage);
                Glide.with(requireContext())
                        .load(track.artworkUrl)
                        .transition(DrawableTransitionOptions.withCrossFade(550))
                        .error(R.drawable.ic_image_not_found_foreground) // 실패 시 이미지
                        .centerCrop()
                        .into(artworkImage);

            }

            artistName.setText(track.artistName);
            artistName.setOnClickListener(v -> {
                ArtistApiHelper apiHelper = new ArtistApiHelper(getContext(), requireActivity());
                apiHelper.getArtist(track.artistId, 0, artist -> {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("artist", artist);
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.navigate(R.id.action_musicInfoFragment_to_artistInfoFragment, bundle);
                });
            });

            albumName.setText(track.albumName);

            //music_info fragment to album_info fragment : shared element transition
            albumName.setOnClickListener(v -> {
                ArtistApiHelper apiHelper = new ArtistApiHelper(getContext(), requireActivity());
                apiHelper.getAlbum(track.albumId, 0, album -> {
                    if (album != null) {
                        Bundle bundle = new Bundle();
                        artworkImage.setTransitionName("music_info_to_album_info");
                        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                                .addSharedElement(artworkImage, artworkImage.getTransitionName())
                                        .build();

                        bundle.putParcelable("album", album);
                        NavController navController = NavHostFragment.findNavController(this);
                        navController.navigate(R.id.action_musicInfoFragment_to_albumInfoFragment, bundle, null, extras);
                    }
                });
            });


            releaseDate.setText(track.releaseDate);
            int durationSec = (int) Double.parseDouble(track.durationMs)/1000;
            durationMs.setText(durationSec/60 + "분 " + durationSec%60 + "초");
            Log.d("TraceId - artist ID", track.artistId);
            Log.d("TraceId - album ID", track.albumId);
            Log.d("TraceId - track ID", track.trackId);
        }
        else {
            Log.e("MusicInfoFragment", "track(Track) is null!");
        }

        ImageButton addButton = view.findViewById(R.id.addButton);

        if (savedInDb){
            addButton.setVisibility(TextView.GONE);
        }
        else {
            favoritesViewModel.loadFavoriteItem(track.trackId, loaded_ -> {
                if (loaded_ != null) {
                    savedInDb = true;
                    addButton.setVisibility(TextView.GONE);
                }
            });
        }

        addButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("관심목록에 추가")
                    .setMessage(track.trackName + " - " + track.artistName + " 을(를) Favorites List 에 추가할까요?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
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
        });



        trackTitle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Title", trackTitle.getText().toString());
                clipboard.setPrimaryClip(clip);
                return true;
            }
        });

        trackTitleKr.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Title", trackTitleKr.getText().toString());
                clipboard.setPrimaryClip(clip);
                return true;
            }
        });


    }

    private void switchTitleLanguage(){
        if (trackTitle.getVisibility() == TextView.VISIBLE  && favorite.metadata != null && favorite.metadata.title != null && !favorite.metadata.title.isEmpty()){
            trackTitle.setVisibility(TextView.GONE);
            trackTitleKr.setVisibility(TextView.VISIBLE);
        }
        else if (!track.trackName.isEmpty()){
            trackTitleKr.setVisibility(TextView.GONE);
            trackTitle.setVisibility(TextView.VISIBLE);
        }
    }


}