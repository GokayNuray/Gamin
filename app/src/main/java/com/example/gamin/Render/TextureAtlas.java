package com.example.gamin.Render;

import android.graphics.Bitmap;

import com.example.gamin.Minecraft.ChunkColumn;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TextureAtlas {
    static final Map<String, TextureAtlas> atlases = new HashMap<>();
    public final int width;
    public final int height;
    final Bitmap bitmap;
    final Map<String, Float> offsets = new HashMap<>();
    FloatBuffer coordsBuffer;
    FloatBuffer texturesBuffer;
    FloatBuffer colorsBuffer;
    int textureHandle;

    TextureAtlas(Bitmap bitmap, Map<String, Integer> offsets) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        offsets.forEach((key, value) -> this.offsets.put(key, value / (float) width));
    }

    void setBuffers(ChunkColumn[] chunks) {
        int squareCount = 0;
        for (ChunkColumn chunk : chunks) {
            squareCount += chunk.squares.getOrDefault(this, new ArrayList<>()).size();
        }
        coordsBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorsBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texturesBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

        if (squareCount == 0) return;

        for (ChunkColumn chunk : chunks) {
            synchronized (chunk) {
                if (!chunk.isLoaded || chunk.coordsBuffers.get(this) == null) {
                    continue;
                }
                coordsBuffer.put(chunk.coordsBuffers.get(this));
                colorsBuffer.put(chunk.colorsBuffers.get(this));
                texturesBuffer.put(chunk.texturesBuffers.get(this));
                chunk.coordsBuffers.get(this).position(0);
                chunk.colorsBuffers.get(this).position(0);
                chunk.texturesBuffers.get(this).position(0);
            }
        }
        coordsBuffer.position(0);
        colorsBuffer.position(0);
        texturesBuffer.position(0);
    }

}
