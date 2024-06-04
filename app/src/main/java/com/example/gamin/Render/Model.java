package com.example.gamin.Render;

import java.util.ArrayList;
import java.util.List;

public abstract class Model {
    public final List<Square> squares = new ArrayList<>();
    public float[] coords;
    public float[] textureCoords;
    public float[] colors;

    public TextureAtlas textureAtlas;

    protected void setBuffers() {
        coords = new float[squares.size() * 6 * 3];
        textureCoords = new float[squares.size() * 6 * 2];
        colors = new float[squares.size() * 6 * 4];
        for (int i = 0; i < squares.size(); i++) {
            Square square = squares.get(i);
            System.arraycopy(square.coords1, 0, coords, i * 6 * 3, 3 * 3);
            System.arraycopy(square.coords2, 0, coords, i * 6 * 3 + 3 * 3, 3 * 3);
            System.arraycopy(square.textures1, 0, textureCoords, i * 6 * 2, 2 * 3);
            System.arraycopy(square.textures2, 0, textureCoords, i * 6 * 2 + 2 * 3, 2 * 3);
            System.arraycopy(square.squareColors, 0, colors, i * 6 * 4, 4 * 6);
        }
    }
}
