package com.example.gamin.Render;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextureAtlas {
    public Bitmap bitmap;

    public int width;
    public int height;
    public List<Float> coords = new ArrayList<>();
    public List<Float> textures = new ArrayList<>();
    public List<Float> colors = new ArrayList<>();
    int textureHandle;
    public Map<String, Float> offsets = new HashMap<>();
    public static Map<String,TextureAtlas> atlases = new HashMap<>();

    public TextureAtlas(Bitmap bitmap, Map<String, Integer> offsets) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        offsets.forEach((key, value) -> this.offsets.put(key, value / (float) width));
    }


}
