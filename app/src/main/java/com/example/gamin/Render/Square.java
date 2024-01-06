package com.example.gamin.Render;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

public class Square {
    public int direction;
    float[] color;
    float[] squareCoords;
    float[] textureCoords;
    String resId;
    Context context;
    List<Float> coords;
    List<Float> textures;
    List<Float> colors;

    public Square(Context context, float[] squareCoords, float[] color, float[] textureCoords, String resId, int direction) {
        this.color = color;
        this.squareCoords = squareCoords;
        this.resId = resId;
        this.context = context;
        this.direction = direction;
        String type = resId.split("/")[0];
        String name = resId.split("/")[1];
        float offset = 0;
        int atlasWidth = 0;
        int atlasHeight = 0;
        if (type.equals("blocks")) {
            assert YourRenderer.blocksAtlas.offsets.containsKey(name + ".png") : "Texture " + name + " not found in blocks atlas";
            offset = YourRenderer.blocksAtlas.offsets.get(name + ".png");
            atlasWidth = YourRenderer.blocksAtlas.width;
            atlasHeight = YourRenderer.blocksAtlas.height;
            coords = YourRenderer.blockCoords;
            textures = YourRenderer.blockTextures;
            colors = YourRenderer.blockColors;
        } else {
            offset = YourRenderer.itemsAtlas.offsets.get(name + ".png");
            atlasWidth = YourRenderer.itemsAtlas.width;
            atlasHeight = YourRenderer.itemsAtlas.height;
            coords = YourRenderer.itemCoords;
            textures = YourRenderer.itemTextures;
            colors = YourRenderer.itemColors;
        }
        this.textureCoords = new float[textureCoords.length];
        for (int i = 0; i < textureCoords.length; i++) {
            if (i % 2 == 0) {
                this.textureCoords[i] = textureCoords[i] * 16 / atlasWidth + offset;
            } else {
                this.textureCoords[i] = textureCoords[i] * 16 / atlasHeight;
            }
        }
    }

    public void render() {
        float[] squareColors = new float[24];
        for (int i = 0; i < 6; i++) {
            System.arraycopy(color, 0, squareColors, i * 4, 4);
        }

        float[] coords1 = Arrays.copyOfRange(squareCoords, 0, 9);
        float[] coords2 = new float[9];
        System.arraycopy(Arrays.copyOfRange(squareCoords, 6, 12), 0, coords2, 0, 6);
        System.arraycopy(Arrays.copyOfRange(squareCoords, 0, 3), 0, coords2, 6, 3);

        float[] textures1 = Arrays.copyOfRange(textureCoords, 0, 6);
        float[] textures2 = new float[6];
        System.arraycopy(Arrays.copyOfRange(textureCoords, 4, 8), 0, textures2, 0, 4);
        System.arraycopy(Arrays.copyOfRange(textureCoords, 0, 2), 0, textures2, 4, 2);

        for (int i = 0; i < 9; i++) {
            coords.add(coords1[i]);
        }
        for (int i = 0; i < 9; i++) {
            coords.add(coords2[i]);
        }

        for (int i = 0; i < 6; i++) {
            textures.add(textures1[i]);
        }
        for (int i = 0; i < 6; i++) {
            textures.add(textures2[i]);
        }

        for (int i = 0; i < 24; i++) {
            colors.add(squareColors[i]);
        }

    }
}
