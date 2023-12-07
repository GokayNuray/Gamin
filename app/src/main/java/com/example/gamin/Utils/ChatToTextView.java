package com.example.gamin.Utils;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public final class ChatToTextView {

    public static SpannableStringBuilder convert(String chat) {
        SpannableStringBuilder builder;
        try {
            JSONObject jsonObject = new JSONObject(chat);
            if (jsonObject.has("text")) {
                builder = new SpannableStringBuilder();
                String text = jsonObject.optString("text");
                builder.append(text);
                boolean bold = jsonObject.optBoolean("bold");
                boolean strikethrough = jsonObject.optBoolean("strikethrough");
                String color = jsonObject.optString("color");
                if (text.length() != 0) {
                    builder.setSpan(new ForegroundColorSpan(getMcColor(color)),builder.length()-text.length(),builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (bold) {
                        builder.setSpan(new StyleSpan(Typeface.BOLD),builder.length()-text.length(),builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (strikethrough) {
                        builder.setSpan(new StrikethroughSpan(),builder.length()-text.length(),builder.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                if (jsonObject.has("extra")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("extra");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        String localcolor = color;
                        boolean localbold = bold;
                        boolean localstrikethrough = strikethrough;
                        if (object.has("color")) {
                            localcolor = object.getString("color");
                        }
                        if (object.has("bold")) {
                            localbold = object.getBoolean("bold");
                        }
                        if (object.has("strikethrough")) {
                            localstrikethrough = object.getBoolean("strikethrough");
                        }
                        text = object.getString("text");
                        if (text.length() != 0) {
                            builder.append(text);
                            builder.setSpan(new ForegroundColorSpan(getMcColor(localcolor)),builder.length()-text.length(),builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            if (localbold) {
                                builder.setSpan(new StyleSpan(Typeface.BOLD),builder.length()-text.length(),builder.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (localstrikethrough) {
                                builder.setSpan(new StrikethroughSpan(),builder.length()-text.length(),builder.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }

                    }
                }


            }
            else {
                builder = new SpannableStringBuilder();
                builder.append(chat);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            builder = new SpannableStringBuilder();
            builder.append(chat);
        }
        if (builder.length()>2) {
            return builder.append("\n");
        }
        else {
            return SpannableStringBuilder.valueOf("");
        }
    }

    public static int getMcColor(String color) {
        int renk;
        switch (color) {
            case "black":
                renk = 0xFF000000;
                break;
            case "dark_blue":
                renk = 0xFF0000aa;
                break;
            case "dark_green":
                renk = 0xFF00aa00;
                break;
            case "dark_aqua":
                renk = 0xFF00aaaa;
                break;
            case "dark_red":
                renk = 0xFFaa0000;
                break;
            case "dark_purple":
                renk = 0xFFaa00aa;
                break;
            case "gold":
                renk = 0xFFffaa00;
                break;
            case "gray":
                renk = 0xFFaaaaaa;
                break;
            case "dark_gray":
                renk = 0xFF555555;
                break;
            case "blue":
                renk = 0xFF5555ff;
                break;
            case "green":
                renk = 0xFF55ff55;
                break;
            case "aqua":
                renk = 0xFF55ffff;
                break;
            case "red":
                renk = 0xFFff5555;
                break;
            case "light_purple":
                renk = 0xFFff55ff;
                break;
            case "yellow":
                renk = 0xFFffff55;
                break;
            case "white":
                renk = 0xFFffffff;
                break;
            default:
                renk = Color.WHITE;

        }
        return renk;
    }
}
