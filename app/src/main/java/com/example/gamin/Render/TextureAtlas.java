package com.example.gamin.Render;

import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;

public class TextureAtlas {
    public Bitmap bitmap;

    public int width;
    public int height;
    public Map<String, Float> offsets = new HashMap<>();

    public TextureAtlas(Bitmap bitmap, Map<String, Integer> offsets) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        offsets.forEach((key, value) -> this.offsets.put(key, value / (float) width));
    }


}
