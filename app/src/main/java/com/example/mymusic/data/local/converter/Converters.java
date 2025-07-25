package com.example.mymusic.data.local.converter;

import android.util.Log;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converters {

    @TypeConverter
    public static String fromList(List<String> list) {
        return list != null ? String.join(",", list) : null;
    }

    @TypeConverter
    public static List<String> toList(String value) {
        return value != null ? Arrays.asList(value.split(",")) : null;
    }

    private static final Gson gson = new Gson();
    @TypeConverter
    public static String fromListList(List<List<String>> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<List<String>> toListList(String data) {
        Log.d("Converters", "toListList called with data: " + data);

        if (data == null || data.trim().isEmpty()) return new ArrayList<>();

        Gson gson = new Gson();
        try {
            // 정상적인 JSON
            return gson.fromJson(data, new TypeToken<List<List<String>>>() {}.getType());
        } catch (Exception e1) {
            // 예전 방식: 쉼표로 나뉜 이름들 (문자열 하나)
            List<List<String>> fallback = new ArrayList<>();
            String[] names = data.split(",");
            for (String name : names) {
                fallback.add(Arrays.asList(name.trim(), null));
            }
            return fallback;
        }
    }

    @TypeConverter
    public static String fromMap(Map<String, Integer> map) {
        return map == null ? null : gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, Integer> toMap(String data) {
        if (data == null || data.trim().isEmpty()) return new HashMap<>();
        Type type = new TypeToken<Map<String, Integer>>() {}.getType();
        return gson.fromJson(data, type);
    }



}
