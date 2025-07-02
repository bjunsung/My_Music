package com.example.mymusic.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.util.Consumer;

import com.example.mymusic.model.Artist;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ArtistSearchService {
    public static void searchArtistByArtistId(String artistId, String accessToken, Consumer<Artist> callback, Consumer<String> onFailure){
        new Thread(() ->{
            try{
                String urlStr = "https://api.spotify.com/v1/artists/" + artistId;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = conn.getResponseCode();
                Log.d("ArtistSearchService", "API calls completed");
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){ //401 accessToken expired
                    Log.d("ArtistSearchService", "Access Token expired, token: " + accessToken);
                    onFailure.accept("ERROR,만료된 토큰입니다.");
                    return;
                }
                else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) { //403
                    Log.d("ArtistSearchService", "접근 제한됨,이 요청에 대한 권한이 없습니다, token: " + accessToken);
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
                int followers = obj.getJSONObject("followers").getInt("total");
                JSONArray genresJSONArray = obj.getJSONArray("genres");
                List<String> genres = new ArrayList<>();
                for (int i = 0; i < genresJSONArray.length(); ++i){
                    genres.add(genresJSONArray.getString(i));
                }
                String name = obj.getString("name");
                JSONArray images = obj.getJSONArray("images");
                String imageUrl = (images.length() > 0) ? images.getJSONObject(0).getString("url") : "";
                int popularity = obj.getInt("popularity");

                new Handler(Looper.getMainLooper()).post(() -> {
                    Log.d("ArtistSearchService", "Success to fetch Artist by artist id");
                    callback.accept(new Artist(
                            artistId,
                            name,
                            imageUrl,
                            genres,
                            followers,
                            popularity
                    ));
                });



            }catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Log.d("ArtistSearchService", "ERROR,API 호출 중 예외 발생:" + e.getMessage());
                    onFailure.accept("ERROR,API 호출 중 예외 발생: " + e.getMessage());
                });
                return;
            }
        }).start();
    }

}
