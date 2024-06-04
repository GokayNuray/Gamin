package com.example.gamin.Render;

import android.opengl.Matrix;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Square {
    final float[] squareColors;
    final float[] textures1;
    final float[] textures2;
    float[] coords1;
    float[] coords2;
    boolean isFace = false;
    float[] squareCoords;

    Square(float[] squareCoords, float[] color, float[] textureCoords) {
        this.squareCoords = squareCoords;

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

    private Square(float[] coords1, float[] coords2, float[] textures1, float[] textures2, float[] squareColors) {
        this.coords1 = coords1;
        this.coords2 = coords2;
        this.textures1 = textures1;
        this.textures2 = textures2;
        this.squareColors = squareColors;
        this.squareCoords = new float[12];
        System.arraycopy(coords1, 0, squareCoords, 0, 9);
        System.arraycopy(coords2, 3, squareCoords, 9, 3);
    }

    static float[] fourCoordsToSix(float[] coords) {
        float[] result = new float[coords.length * 3 / 2];
        for (int i = 0; i < coords.length; i += 12) {
            float[] coords1 = Arrays.copyOfRange(coords, i, i + 9);
            float[] coords2 = new float[9];
            System.arraycopy(Arrays.copyOfRange(coords, i + 6, i + 12), 0, coords2, 0, 6);
            System.arraycopy(Arrays.copyOfRange(coords, i, i + 3), 0, coords2, 6, 3);

            System.arraycopy(coords1, 0, result, i * 3 / 2, 9);
            System.arraycopy(coords2, 0, result, i * 3 / 2 + 9, 9);
        }
        return result;
    }

    @NonNull
    static float[][] getRectangularPrism(float[] to, float[] from) {
        return new float[][]{
                //north
                {to[0], to[1], from[2],
                        to[0], from[1], from[2],
                        from[0], from[1], from[2],
                        from[0], to[1], from[2]},
                //west
                {from[0], to[1], from[2],
                        from[0], from[1], from[2],
                        from[0], from[1], to[2],
                        from[0], to[1], to[2]},
                //south
                {from[0], to[1], to[2],
                        from[0], from[1], to[2],
                        to[0], from[1], to[2],
                        to[0], to[1], to[2]},
                //east
                {to[0], to[1], to[2],
                        to[0], from[1], to[2],
                        to[0], from[1], from[2],
                        to[0], to[1], from[2]},
                //up
                {to[0], to[1], to[2],
                        to[0], to[1], from[2],
                        from[0], to[1], from[2],
                        from[0], to[1], to[2]},
                //down
                {to[0], from[1], to[2],
                        from[0], from[1], to[2],
                        from[0], from[1], from[2],
                        to[0], from[1], from[2]}
        };
    }

    static float[] rotateUV(float[] uv, float angle) {
        List<Float> coords = new ArrayList<>();
        for (float f : uv) {
            coords.add(f);
        }
        for (int i = 8; i > 0; i--) {
            coords.add(i, 2.5f);
        }
        float[] rotationMatrix = new float[16];
        float[] matrix = new float[16];
        for (int i = 0; i < 16; i++) {
            matrix[i] = coords.get(i) - 0.5f;
        }
        Matrix.setRotateM(rotationMatrix, 0, angle, 0, 1, 0);
        Matrix.multiplyMM(matrix, 0, rotationMatrix, 0, matrix, 0);
        coords.clear();
        for (float f : matrix) {
            coords.add(Math.abs(f) < 0.0001 ? 0.0f : f);
        }
        for (int i = 8; i > 0; i--) {
            coords.remove(2 * i - 1);
        }
        float[] result = new float[8];
        for (int i = 0; i < 8; i++) {
            result[i] = coords.get(i) + 0.5f;
        }
        return result;
    }

    static void addCoordinates(float[] in, float x, float y, float z) {
        for (int i = 0; i < in.length; i++) {
            int a = i % 3;
            switch (a) {
                case 0:
                    in[i] = in[i] + x;
                    break;
                case 1:
                    in[i] = in[i] + y;
                    break;
                case 2:
                    in[i] = in[i] + z;
                    break;
            }
        }
    }

    void rotate(float angle, int rotationAxis, float originX, float originY, float originZ) {
        float x = 0;
        float y = 0;
        float z = 0;
        switch (rotationAxis) {
            case 0:
                x = 1;
                break;
            case 1:
                y = 1;
                break;
            case 2:
                z = 1;
                break;
        }
        List<Float> coords = new ArrayList<>();
        for (float f : squareCoords) {
            coords.add(f);
        }
        coords.add(12, 1f);
        coords.add(9, 1f);
        coords.add(6, 1f);
        coords.add(3, 1f);
        float[] rotationMatrix = new float[16];
        float[] matrix = new float[16];
        for (int i = 0; i < 16; i += 4) {
            matrix[i] = coords.get(i) - originX;
            matrix[i + 1] = coords.get(i + 1) - originY;
            matrix[i + 2] = coords.get(i + 2) - originZ;
            matrix[i + 3] = coords.get(i + 3);
        }
        Matrix.setRotateM(rotationMatrix, 0, angle, x, y, z);
        Matrix.multiplyMM(matrix, 0, rotationMatrix, 0, matrix, 0);

        coords.clear();
        for (float f : matrix) {
            coords.add(f);
        }
        coords.remove(15);
        coords.remove(11);
        coords.remove(7);
        coords.remove(3);
        float[] result = new float[12];
        for (int i = 0; i < 12; i += 3) {
            result[i] = coords.get(i) + originX;
            result[i + 1] = coords.get(i + 1) + originY;
            result[i + 2] = coords.get(i + 2) + originZ;
        }
        squareCoords = result;
    }

    void scale(float x, float y, float z) {
        for (int i = 0; i < squareCoords.length; i++) {
            int a = i % 3;
            switch (a) {
                case 0:
                    squareCoords[i] = squareCoords[i] * x;
                    break;
                case 1:
                    squareCoords[i] = squareCoords[i] * y;
                    break;
                case 2:
                    squareCoords[i] = squareCoords[i] * z;
                    break;
            }
        }
    }

    void translate(float x, float y, float z) {
        for (int i = 0; i < squareCoords.length; i++) {
            int a = i % 3;
            switch (a) {
                case 0:
                    squareCoords[i] = squareCoords[i] + x;
                    break;
                case 1:
                    squareCoords[i] = squareCoords[i] + y;
                    break;
                case 2:
                    squareCoords[i] = squareCoords[i] + z;
                    break;
            }
        }
    }

    //flip the square upside down
    void flip() {
        float[] coords = squareCoords;
        for (int i = 1; i < coords.length; i += 3) {
            coords[i] = coords[i] - 0.5f;
            coords[i] = -coords[i];
            coords[i] = coords[i] + 0.5f;
        }
    }


    void splitCoords() {
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
        Square square = new Square(coords1, coords2, textures1, textures2, squareColors);
        square.isFace = isFace;
        return square;
    }
}
