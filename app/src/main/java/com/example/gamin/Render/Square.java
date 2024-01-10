package com.example.gamin.Render;

import java.util.Arrays;

public class Square {
    public int direction;
    public float[] squareCoords;
    float[] squareColors;
    float[] coords1;
    float[] coords2;
    float[] textures1;
    float[] textures2;
    TextureAtlas atlas;

    public Square(float[] squareCoords, float[] color, float[] textureCoords, TextureAtlas atlas, int direction) {
        this.squareCoords = squareCoords;
        this.direction = direction;
        this.atlas = atlas;

        squareColors = new float[24];
        for (int i = 0; i < 6; i++) {
            System.arraycopy(color, 0, squareColors, i * 4, 4);
        }

        splitCoords();

        textures1 = Arrays.copyOfRange(textureCoords, 0, 6);
        textures2 = new float[6];
        System.arraycopy(Arrays.copyOfRange(textureCoords, 4, 8), 0, textures2, 0, 4);
        System.arraycopy(Arrays.copyOfRange(textureCoords, 0, 2), 0, textures2, 4, 2);
    }
    public Square(float[] coords1, float[] coords2, float[] textures1, float[] textures2, float[] squareColors, TextureAtlas atlas, int direction) {
        this.coords1 = coords1;
        this.coords2 = coords2;
        this.textures1 = textures1;
        this.textures2 = textures2;
        this.squareColors = squareColors;
        this.atlas = atlas;
        this.direction = direction;
        this.squareCoords = new float[12];
        System.arraycopy(coords1, 0, squareCoords, 0, 9);
        System.arraycopy(coords2, 3, squareCoords, 9, 3);
    }

    public void splitCoords() {
        coords1 = Arrays.copyOfRange(squareCoords, 0, 9);
        coords2 = new float[9];
        System.arraycopy(Arrays.copyOfRange(squareCoords, 6, 12), 0, coords2, 0, 6);
        System.arraycopy(Arrays.copyOfRange(squareCoords, 0, 3), 0, coords2, 6, 3);
    }

    public Square copy() {
        float[] coords1 = new float[9];
        float[] coords2 = new float[9];
        float[] textures1 = new float[6];
        float[] textures2 = new float[6];
        float[] squareColors = new float[24];
        System.arraycopy(this.coords1, 0, coords1, 0, 9);
        System.arraycopy(this.coords2, 0, coords2, 0, 9);
        System.arraycopy(this.textures1, 0, textures1, 0, 6);
        System.arraycopy(this.textures2, 0, textures2, 0, 6);
        System.arraycopy(this.squareColors, 0, squareColors, 0, 24);
        return new Square(coords1, coords2, textures1, textures2, squareColors, atlas, direction);
    }

    public void render() {

        for (int i = 0; i < 9; i++) {
            atlas.coords.add(coords1[i]);
        }
        for (int i = 0; i < 9; i++) {
            atlas.coords.add(coords2[i]);
        }

        for (int i = 0; i < 6; i++) {
            atlas.textures.add(textures1[i]);
        }
        for (int i = 0; i < 6; i++) {
            atlas.textures.add(textures2[i]);
        }

        for (int i = 0; i < 24; i++) {
            atlas.colors.add(squareColors[i]);
        }

    }
}
