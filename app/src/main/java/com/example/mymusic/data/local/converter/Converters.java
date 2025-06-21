package com.example.mymusic.data.local.converter;

import androidx.room.TypeConverter;
import java.util.Arrays;
import java.util.List;

public class Converters {

    @TypeConverter
    public static String fromList(List<String> list) {
        return list != null ? String.join(",", list) : null;
    }

    @TypeConverter
    public static List<String> toList(String value) {
        return value != null ? Arrays.asList(value.split(",")) : null;
    }
}
