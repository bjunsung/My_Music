package com.example.mymusic.data.util;

import java.text.DecimalFormat;

public class NumberUtils {
    public NumberUtils(){}

    public static String formatWithComma(String number){
        try {
            DecimalFormat formatter = new DecimalFormat("#,###");
            return formatter.format(number);
        }catch(NumberFormatException e){
            return number;
        }
    }

    public static String formatWithComma(int number){
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(number);
    }

    public static String formatWithComma(double number){
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        return formatter.format(number);
    }


}
