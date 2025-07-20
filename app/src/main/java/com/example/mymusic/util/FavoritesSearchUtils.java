package com.example.mymusic.util;

import com.example.mymusic.model.Favorite;

import java.util.ArrayList;
import java.util.List;

import java.util.function.Function;

public class FavoritesSearchUtils {

    public static <T> List<Integer> getContainPositions(String keyword, List<T> list, Function<T, String> textExtractor) {
        if (list == null || list.isEmpty()) return null;
        if (keyword == null || keyword.isEmpty()) return null;

        String lowerKeyword = keyword.toLowerCase().replaceAll("\\s+", "");
        List<Integer> positions = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            String text = textExtractor.apply(list.get(i));
            if (text != null) {
                String processed = text.toLowerCase().replaceAll("\\s+", "");
                if (processed.contains(lowerKeyword))
                    positions.add(i);
            }
        }

        return positions;
    }




}
