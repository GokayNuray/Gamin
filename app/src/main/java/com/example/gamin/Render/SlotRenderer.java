package com.example.gamin.Render;

import android.annotation.SuppressLint;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** @noinspection ResultOfMethodCallIgnored*/
@SuppressLint("DiscouragedApi")
public class SlotRenderer {

    List<Square> squares = new ArrayList<>();
    static Map<String, List<Square>> models = new HashMap<>();
    public SlotRenderer(Context context, float[] color, String model, int x, int y, int z) throws IOException, JSONException {
        if (!models.containsKey(model)) {
            InputStream is;
            if (Arrays.asList(context.getAssets().list("models/block")).contains(model))
                is = context.getAssets().open("models/block/" + model);
            else
                is = context.getAssets().open("models/item/" + model);

            byte[] b = new byte[is.available()];
            is.read(b);
            JSONObject jsonObject = new JSONObject(new String(b));
            while (jsonObject.has("parent")) {
                JSONObject textures = jsonObject.optJSONObject("textures");
                InputStream is2 = context.getAssets().open("models/" + jsonObject.getString("parent") + ".json");
                byte[] b2 = new byte[is2.available()];
                is2.read(b2);
                is2.close();
                jsonObject = new JSONObject(new String(b2));
                String object = jsonObject.toString();
                int i = object.indexOf("#");
                while (i != -1) {
                    int j = object.indexOf("\"", i);
                    String newString;
                    if (textures != null) {

                        if (textures.has(object.substring(i + 1, j))) {
                            newString = textures.getString(object.substring(i + 1, j));
                        } else {
                            newString = jsonObject.getJSONObject("textures").getString(object.substring(i + 1, j));
                        }
                    } else {
                        newString = jsonObject.getJSONObject("textures").getString(object.substring(i + 1, j));
                    }
                    String newPart1 = object.substring(0, i);
                    String newPart2 = object.substring(j);
                    object = newPart1 + newString + newPart2;
                    i = object.indexOf("#");
                }
                jsonObject = new JSONObject(object);
            }
            JSONArray elements = jsonObject.getJSONArray("elements");
            int biggest = 0;
            int smallest = 0;
            for (int i = 0; i < elements.length(); i++) {
                int[] from = new int[3];
                int[] to = new int[3];
                JSONObject element = elements.getJSONObject(i);
                JSONArray jFrom = element.getJSONArray("from");
                for (int j = 0; j < jFrom.length(); j++) {
                    int num = jFrom.getInt(j);
                    from[j] = num;
                    if (num > biggest) {
                        biggest = num;
                    }
                    if (num < smallest) {
                        smallest = num;
                    }
                }
                JSONArray jTo = element.getJSONArray("to");
                for (int j = 0; j < jTo.length(); j++) {
                    int num = jTo.getInt(j);
                    to[j] = num;
                    if (num > biggest) {
                        biggest = num;
                    }
                    if (num < smallest) {
                        smallest = num;
                    }
                }
                if (element.getJSONObject("faces").has("north")) {
                    int[] iSquareCoordsN = {
                            to[0], to[1], from[2],
                            to[0], from[1], from[2],
                            from[0], from[1], from[2],
                            from[0], to[1], from[2]
                    };
                    float[] squareCoordsN = intToFloat(iSquareCoordsN, smallest, biggest);
                    float[] textureCoordsN;
                    if (element.getJSONObject("faces").getJSONObject("north").has("uv")) {
                        JSONArray uv = element.getJSONObject("faces").getJSONObject("north").getJSONArray("uv");
                        int[] iTextureCoords = new int[]{
                                uv.getInt(2), uv.getInt(1),
                                uv.getInt(2), uv.getInt(3),
                                uv.getInt(0), uv.getInt(3),
                                uv.getInt(0), uv.getInt(1)
                        };

                        textureCoordsN = intToFloat2(iTextureCoords, smallest, biggest);
                    } else {
                        textureCoordsN = new float[]{
                                1.0f, 0.0f,
                                1.0f, 1.0f,
                                0.0f, 1.0f,
                                0.0f, 0.0f
                        };
                    }
                    String textureN = element.getJSONObject("faces").getJSONObject("north").getString("texture");
                    textureN = textureN.substring(textureN.lastIndexOf("/") + 1);
                    int textureIdN = context.getResources().getIdentifier(textureN, "drawable", context.getPackageName());
                    Square squareN = new Square(context, squareCoordsN, color, textureCoordsN, textureIdN);
                    squares.add(squareN);
                }
                if (element.getJSONObject("faces").has("west")) {
                    int[] iSquareCoordsW = {
                            from[0], to[1], from[2],
                            from[0], from[1], from[2],
                            from[0], from[1], to[2],
                            from[0], to[1], to[2]
                    };
                    float[] squareCoordsW = intToFloat(iSquareCoordsW, smallest, biggest);
                    float[] textureCoordsW;
                    if (element.getJSONObject("faces").getJSONObject("west").has("uv")) {
                        JSONArray uv = element.getJSONObject("faces").getJSONObject("west").getJSONArray("uv");
                        int[] iTextureCoords = new int[]{
                                /*uv.getInt(0), uv.getInt(3),
                                uv.getInt(0), uv.getInt(1),
                                uv.getInt(2), uv.getInt(1),
                                uv.getInt(2), uv.getInt(3)*/
                                uv.getInt(2), uv.getInt(1),
                                uv.getInt(2), uv.getInt(3),
                                uv.getInt(0), uv.getInt(3),
                                uv.getInt(0), uv.getInt(1)
                        };
                        textureCoordsW = intToFloat2(iTextureCoords, smallest, biggest);
                    } else {
                        textureCoordsW = new float[]{
                                /*0.0f, 1.0f,
                                0.0f, 0.0f,
                                1.0f, 0.0f,
                                1.0f, 1.0f*/
                                1.0f, 0.0f,
                                1.0f, 1.0f,
                                0.0f, 1.0f,
                                0.0f, 0.0f
                        };
                    }
                    String textureW = element.getJSONObject("faces").getJSONObject("west").getString("texture");
                    textureW = textureW.substring(textureW.lastIndexOf("/") + 1);
                    int textureIdW = context.getResources().getIdentifier(textureW, "drawable", context.getPackageName());
                    Square squareW = new Square(context, squareCoordsW, color, textureCoordsW, textureIdW);
                    squares.add(squareW);
                }
                if (element.getJSONObject("faces").has("south")) {
                    int[] iSquareCoordsS = {
                            from[0], to[1], to[2],
                            from[0], from[1], to[2],
                            to[0], from[1], to[2],
                            to[0], to[1], to[2]
                    };
                    float[] squareCoordsS = intToFloat(iSquareCoordsS, smallest, biggest);
                    float[] textureCoordsS;
                    if (element.getJSONObject("faces").getJSONObject("south").has("uv")) {
                        JSONArray uv = element.getJSONObject("faces").getJSONObject("south").getJSONArray("uv");
                        int[] iTextureCoords = new int[]{
                                uv.getInt(2), uv.getInt(1),
                                uv.getInt(2), uv.getInt(3),
                                uv.getInt(0), uv.getInt(3),
                                uv.getInt(0), uv.getInt(1)
                        };
                        textureCoordsS = intToFloat2(iTextureCoords, smallest, biggest);
                    } else {
                        textureCoordsS = new float[]{
                                1.0f, 0.0f,
                                1.0f, 1.0f,
                                0.0f, 1.0f,
                                0.0f, 0.0f
                        };
                    }
                    String textureS = element.getJSONObject("faces").getJSONObject("south").getString("texture");
                    textureS = textureS.substring(textureS.lastIndexOf("/") + 1);
                    int textureIdS = context.getResources().getIdentifier(textureS, "drawable", context.getPackageName());
                    Square squareS = new Square(context, squareCoordsS, color, textureCoordsS, textureIdS);
                    squares.add(squareS);
                }
                if (element.getJSONObject("faces").has("east")) {
                    int[] iSquareCoordsE = {
                            to[0], to[1], to[2],
                            to[0], from[1], to[2],
                            to[0], from[1], from[2],
                            to[0], to[1], from[2]
                    };
                    float[] squareCoordsE = intToFloat(iSquareCoordsE, smallest, biggest);
                    float[] textureCoordsE;
                    if (element.getJSONObject("faces").getJSONObject("east").has("uv")) {
                        JSONArray uv = element.getJSONObject("faces").getJSONObject("east").getJSONArray("uv");
                        int[] iTextureCoords = new int[]{
                                uv.getInt(2), uv.getInt(1),
                                uv.getInt(2), uv.getInt(3),
                                uv.getInt(0), uv.getInt(3),
                                uv.getInt(0), uv.getInt(1)
                        };
                        textureCoordsE = intToFloat2(iTextureCoords, smallest, biggest);
                    } else {
                        textureCoordsE = new float[]{
                                1.0f, 0.0f,
                                1.0f, 1.0f,
                                0.0f, 1.0f,
                                0.0f, 0.0f
                        };
                    }
                    String textureE = element.getJSONObject("faces").getJSONObject("east").getString("texture");
                    textureE = textureE.substring(textureE.lastIndexOf("/") + 1);
                    int textureIdE = context.getResources().getIdentifier(textureE, "drawable", context.getPackageName());
                    Square squareE = new Square(context, squareCoordsE, color, textureCoordsE, textureIdE);
                    squares.add(squareE);
                }
                if (element.getJSONObject("faces").has("up")) {
                    int[] iSquareCoordsU = {
                            to[0], to[1], to[2],
                            to[0], to[1], from[2],
                            from[0], to[1], from[2],
                            from[0], to[1], to[2]
                    };
                    float[] squareCoordsU = intToFloat(iSquareCoordsU, smallest, biggest);
                    float[] textureCoordsU;
                    if (element.getJSONObject("faces").getJSONObject("up").has("uv")) {
                        JSONArray uv = element.getJSONObject("faces").getJSONObject("up").getJSONArray("uv");
                        int[] iTextureCoords = new int[]{
                                uv.getInt(2), uv.getInt(1),
                                uv.getInt(2), uv.getInt(3),
                                uv.getInt(0), uv.getInt(3),
                                uv.getInt(0), uv.getInt(1)
                        };
                        textureCoordsU = intToFloat2(iTextureCoords, smallest, biggest);
                    } else {
                        textureCoordsU = new float[]{
                                1.0f, 0.0f,
                                1.0f, 1.0f,
                                0.0f, 1.0f,
                                0.0f, 0.0f
                        };
                    }
                    float[] color2;
                    if (element.getJSONObject("faces").getJSONObject("up").has("tintindex")) {
                        color2 = new float[]{0.0f, 1.0f, 0.0f, 1.0f};
                    } else {
                        color2 = color;
                    }
                    String textureU = element.getJSONObject("faces").getJSONObject("up").getString("texture");
                    textureU = textureU.substring(textureU.lastIndexOf("/") + 1);
                    int textureIdU = context.getResources().getIdentifier(textureU, "drawable", context.getPackageName());
                    Square squareU = new Square(context, squareCoordsU, color2, textureCoordsU, textureIdU);
                    squares.add(squareU);

                }
                if (element.getJSONObject("faces").has("down")) {
                    int[] iSquareCoordsD = {
                            to[0], from[1], to[2],
                            to[0], from[1], from[2],
                            from[0], from[1], from[2],
                            from[0], from[1], to[2]
                    };
                    float[] squareCoordsD = intToFloat(iSquareCoordsD, smallest, biggest);
                    float[] textureCoordsD;
                    if (element.getJSONObject("faces").getJSONObject("down").has("uv")) {
                        JSONArray uv = element.getJSONObject("faces").getJSONObject("down").getJSONArray("uv");
                        int[] iTextureCoords = new int[]{
                                uv.getInt(2), uv.getInt(1),
                                uv.getInt(2), uv.getInt(3),
                                uv.getInt(0), uv.getInt(3),
                                uv.getInt(0), uv.getInt(1)
                        };
                        textureCoordsD = intToFloat2(iTextureCoords, smallest, biggest);
                    } else {
                        textureCoordsD = new float[]{
                                1.0f, 0.0f,
                                1.0f, 1.0f,
                                0.0f, 1.0f,
                                0.0f, 0.0f
                        };
                    }
                    String textureD = element.getJSONObject("faces").getJSONObject("down").getString("texture");
                    textureD = textureD.substring(textureD.lastIndexOf("/") + 1);
                    int textureIdD = context.getResources().getIdentifier(textureD, "drawable", context.getPackageName());
                    Square squareD = new Square(context, squareCoordsD, color, textureCoordsD, textureIdD);
                    squares.add(squareD);
                }
            }
            System.out.println("AmongUs?");
            models.put(model, squares);
        }

        for (Square square : Objects.requireNonNull(models.get(model))
        ) {
            float[] old = square.squareCoords;
            square.squareCoords = addCoordinates(square.squareCoords, x, y, z);
            square.render();
            square.squareCoords = old;
        }
    }

    public static float[] addCoordinates(float[] old, int x, int y, int z) {
        float[] newCoords = new float[old.length];
        for (int i = 0; i < old.length; i++) {
            int a = i % 3;
            switch (a) {
                case 0:
                    newCoords[i] = old[i] + x;
                    break;
                case 1:
                    newCoords[i] = old[i] + y;
                    break;
                case 2:
                    newCoords[i] = old[i] + z;
                    break;
            }
        }
        return newCoords;
    }

    public static float[] intToFloat(int[] in, int smallest, int biggest) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (((float) (in[i] - smallest)) / ((float) biggest - smallest))/*0.5f*/;
        }
        return out;
    }

    public static float[] intToFloat2(int[] in, int smallest, int biggest) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (((float) (in[i] - smallest)) / ((float) biggest - smallest));
        }
        return out;
    }

}
