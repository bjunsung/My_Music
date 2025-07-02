package com.example.mymusic.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.util.Consumer;


import com.example.mymusic.model.Album;
import com.example.mymusic.model.Track;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TrackSearchService {
    private static List<Track> albumList;

    public static void searchTrackByArtist(String artistId, String accessToken, Consumer<List<Track>> callback, Consumer<String> onFailure){
        //Api 호출시간 지연 방지하기 위해 new Thread()로 구현
        new Thread(() -> {
            List<Track> result = new ArrayList<>();

            try {
                String urlStr = "https://api.spotify.com/v1/artists/" + artistId + "/top-tracks?market=KR";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = conn.getResponseCode();
                Log.d("TrackSearchService", "API calls completed");
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){ //401 accessToken expired
                    onFailure.accept("ERROR,만료된 토큰입니다.");
                    return;
                }
                else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) { //403
                    onFailure.accept("접근 제한됨,이 요청에 대한 권한이 없습니다.");
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream())); //conn.getInputStream() 여기서 401, 403 등 exception throw 할 수 있음
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray tracks = json.getJSONArray("tracks");
                for(int i = 0; i < tracks.length(); i++){
                    JSONObject obj = tracks.getJSONObject(i);
                    JSONObject album = obj.getJSONObject("album");
                    JSONArray artists = obj.getJSONArray("artists");
                    JSONArray images = album.getJSONArray("images");
                    String imageUrl = images.length() > 0 ? images.getJSONObject(0).getString("url") : "";
                    String albumId = album.getString("id");
                    String albumName = album.getString("name");
                    String releaseDate = album.getString("release_date");
                    String artistName = artists.getJSONObject(0).getString("name");
                    String trackId = obj.getString("id");
                    String trackName = obj.getString("name");
                    String durationMs = obj.getString("duration_ms");

                    result.add(new Track(
                            trackId,
                            albumId,
                            artistId,
                            trackName,
                            albumName,
                            artistName,
                            imageUrl,
                            releaseDate,
                            durationMs
                    ));
                }

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    onFailure.accept("ERROR,API 호출 중 예외 발생: " + e.getMessage());
                });
                return;
            }

            List<Track> finalResult = result;
            //결과를 UI Thread 로 전달
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalResult.isEmpty()) {
                    onFailure.accept("Track Not Found,검색 결과가 없습니다.");
                } else {
                    callback.accept(finalResult);
                }
            });

        }).start();
    }

    public static void searchTrackByAlbum(Album album, String accessToken, Consumer<List<Track>> callback, Consumer<String> onFailure){
        //Api 호출시간 지연 방지하기 위해 new Thread()로 구현
        new Thread(() -> {
            List<Track> result = new ArrayList<>();

            try {
                String urlStr = "https://api.spotify.com/v1/albums/" + album.albumId + "/tracks?market=KR&limit=50";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = conn.getResponseCode();
                Log.d("TrackSearchService", "API calls completed");
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){ //401 accessToken expired
                    onFailure.accept("ERROR,만료된 토큰입니다.");
                    return;
                }
                else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) { //403
                    onFailure.accept("접근 제한됨,이 요청에 대한 권한이 없습니다.");
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream())); //conn.getInputStream() 여기서 401, 403 등 exception throw 할 수 있음
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONObject json = new JSONObject(response.toString());
                JSONArray tracks = json.getJSONArray("items");
                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject obj = tracks.getJSONObject(i);
                    JSONArray artists = obj.getJSONArray("artists");
                    JSONObject artist = artists.getJSONObject(0);
                    String trackId = obj.getString("id");
                    String trackName = obj.getString("name");
                    String artistId = artist.getString("id");
                    String artistName = artist.getString("name");
                    String durationMs = obj.getString("duration_ms");

                    result.add(new Track(
                            trackId,
                            album.albumId,
                            artistId,
                            trackName,
                            album.albumName,
                            artistName,
                            album.artworkUrl,
                            album.releaseDate,
                            durationMs
                    ));

                }


            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    onFailure.accept("ERROR,API 호출 중 예외 발생: " + e.getMessage());
                });
                return;
            }

            List<Track> finalResult = result;
            //결과를 UI Thread 로 전달
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalResult.isEmpty()) {
                    onFailure.accept("Track Not Found,검색 결과가 없습니다.");
                } else {
                    callback.accept(finalResult);
                }
            });

        }).start();
    }
}
