package com.example.mymusic.network;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.util.Consumer;

import com.example.mymusic.data.repository.SettingRepository;
import com.example.mymusic.model.Album;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class AlbumSearchService {

    public static void searchAlbumByAlbumId(String albumId, String accessToken, Consumer<Album> callback, Consumer<String> onFailure){
        //Api 호출시간 지연 방지하기 위해 new Thread()로 구현
        new Thread(() -> {
            Album result;
            try {
                String urlStr = "https://api.spotify.com/v1/albums/" + albumId + "?market=KR";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = conn.getResponseCode();
                Log.d("AlbumSearchService", "API calls completed");

                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){ //401 accessToken expired
                    onFailure.accept("Error,만료된 토큰입니다.");
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

                JSONObject obj = new JSONObject(response.toString());
                JSONObject artistObj = obj.getJSONArray("artists").getJSONObject(0);
                String artistId = artistObj.getString("id");
                String artistName = artistObj.getString("name");

                String albumName = obj.getString("name");
                String releaseDate = obj.getString("release_date");
                String imageUrl = obj.getJSONArray("images").getJSONObject(0).getString("url");
                int totalTracks = obj.getInt("total_tracks");

                result = new Album(
                            artistId,
                            albumId,
                            artistName,
                            albumName,
                            releaseDate,
                            imageUrl,
                        totalTracks
                    );


            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    onFailure.accept("ERROR,API 호출 중 예외 발생: " + e.getMessage());
                });
                return;
            }

            Album finalResult = result;
            //결과를 UI Thread 로 전달
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.accept(finalResult);
            });

        }).start();
    }


    public static void searchAlbumByArtist(String artistId, Context context, String accessToken, Consumer<List<Album>> callback, Consumer<String> onFailure){
        //Api 호출시간 지연 방지하기 위해 new Thread()로 구현
        new Thread(() -> {
            List<Album> result = new ArrayList<>();
            try {
                SettingRepository repository = new SettingRepository(context);
                int maxAlbumLLimit = repository.getMaxSearchedAlbumsByArtist();
                String urlStr = "https://api.spotify.com/v1/artists/" + artistId + "/albums?market=KR&limit=" + String.valueOf(maxAlbumLLimit);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = conn.getResponseCode();

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
                JSONArray items = json.getJSONArray("items");
                for (int i = 0; i < items.length(); i++){
                    JSONObject obj = items.getJSONObject(i);
                    JSONArray images = obj.getJSONArray("images");
                    String imageUrl = images.length() > 0 ? images.getJSONObject(0).getString("url") : "";
                    JSONArray artists = obj.getJSONArray("artists");
                    String artistName = "";
                    if (artists.length() > 0) {
                        JSONObject artist = artists.getJSONObject(0);
                        artistName = artist.getString("name");
                    }
                     result.add(new Album(
                            artistId,
                            obj.getString("id"),
                             artistName,
                            obj.getString("name"),
                            obj.getString("release_date"),
                            imageUrl,
                            obj.getInt("total_tracks")
                    ));
                }

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    onFailure.accept("ERROR,API 호출 중 예외 발생: " + e.getMessage());
                });
                return;
            }

            List<Album> finalResult = result;
            //결과를 UI Thread 로 전달
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalResult.isEmpty()) {
                    onFailure.accept("Album Not Found,검색 결과가 없습니다.");
                } else {
                    callback.accept(finalResult);
                }
            });

        }).start();
    }



}
