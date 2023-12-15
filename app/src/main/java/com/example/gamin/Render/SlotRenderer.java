package com.example.gamin.Render;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.Matrix;

import com.example.gamin.Minecraft.ChunkColumn;
import com.example.gamin.Minecraft.Slot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @noinspection ResultOfMethodCallIgnored
 */
@SuppressLint("DiscouragedApi")
public class SlotRenderer {

    static Set<Integer> specialBlocks = new HashSet<>(Arrays.asList(2, 85, 113, 188, 189, 190, 191, 192));
    static Set<Integer> multiStateBlocks = new HashSet<>(Arrays.asList(17, 162, 43, 125, 181));
    static Map<String, List<Square>> models = new HashMap<>();
    //static final String[] woodTypes = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
    List<Square> squares = new ArrayList<>();
    float angle = 0;

    public SlotRenderer(Context context, float[] color, short id, byte metadata, int type, int x, int y, int z) throws IOException, JSONException {
        String model;
        float modelAngle = 0;
        switch (type) {
            //item
            case 0:
                JSONObject item = Slot.itemsMap.get((int) id);
                if (item.has("variations")) {
                    JSONArray variations = item.getJSONArray("variations");
                    //get the variation with the correct metadata
                    for (int i = 0; i < variations.length(); i++) {
                        JSONObject variation = variations.getJSONObject(i);
                        if (variation.getInt("metadata") == metadata) {
                            item = variation;
                        }
                    }
                }
                if (item.has("itemModel")) {
                    model = item.getString("itemModel");
                } else {
                    model = item.getString("displayName").trim().toLowerCase().replaceAll(" ", "_");
                }
                model = "models/item/" + model + ".json";
                break;
            //block
            case 1:
                id = id < 0 ? (short) (id + 256) : id;
                short slabId = id;
                if (id == 43 | id == 125 || id == 181) slabId++;
                if (specialBlocks.contains((int) id)) {
                    model = getSpecialBlockModel(id, metadata, x, y, z);
                    break;
                }
                JSONObject block = Slot.blocksMap.get((int) slabId);
                assert block != null : "Block is null" + id;
                if (block.has("variations")) {
                    JSONArray variations = block.getJSONArray("variations");
                    //get the variation with the correct metadata
                    for (int i = 0; i < variations.length(); i++) {
                        JSONObject variation = variations.getJSONObject(i);
                        if (variation.getInt("metadata") == metadata) {
                            block = variation;
                        }
                    }
                }
                if (block.has("blockModel")) {
                    model = block.getString("blockModel");
                } else if (block.has("itemModel")) {
                    model = block.getString("itemModel");
                } else {
                    model = block.getString("displayName").trim().toLowerCase().replaceAll(" ", "_");
                }
                model = "models/block/" + model + ".json";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
        if (!models.containsKey(model + id + " " + metadata)) {
            InputStream is;
            String newModel = model;
            try {
                is = context.getAssets().open(model);
            } catch (Exception e) {
                System.out.println("aModel not found: " + model);
                newModel = model.replaceFirst("block", "item");
                try {
                    is = context.getAssets().open(newModel);
                } catch (Exception e2) {
                    System.out.println("bbModel not found: " + model + "  " + newModel);
                    newModel = "models/block/pumpkin_stem_growth3.json";
                }
            }
            if (multiStateBlocks.contains((int) id)) {
                System.out.println("multiStateBlock" + id + newModel);
                newModel = getMultiStateBlockModel(id, metadata, newModel);
            }
            for (String s : newModel.split("amongus")) {
                if (s.startsWith("angle")) {
                    modelAngle = Float.parseFloat(s.substring(5));
                    continue;
                }
                is = context.getAssets().open(s);
                //assert is != null : "Model not found: " + model;
                byte[] b = new byte[is.available()];
                is.read(b);
                is.close();
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
            }
            //System.out.println("AmongUs?" + model);
            if (modelAngle != 0)
                for (Square square : squares)
                    rotateSquare(square, modelAngle);
            models.put(model + id + " " + metadata, squares);
        }

        for (Square square : Objects.requireNonNull(models.get(model + id + " " + metadata))
        ) {
            float[] old = square.squareCoords;
            if (angle != 0) {
                List<Float> coords = new ArrayList<>();
                for (float f : square.squareCoords) {
                    coords.add(f);
                }
                coords.add(12, 1.5f);
                coords.add(9, 1.5f);
                coords.add(6, 1.5f);
                coords.add(3, 1.5f);
                float[] rotationMatrix = new float[16];
                float[] matrix = new float[16];
                for (int i = 0; i < 16; i++) {
                    matrix[i] = coords.get(i) - 0.5f;
                }
                Matrix.setRotateM(rotationMatrix, 0, angle, 0, 1, 0);
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
                for (int i = 0; i < 12; i++) {
                    result[i] = coords.get(i) + 0.5f;
                }
                square.squareCoords = addCoordinates(result, x, y, z);
            } else {
                square.squareCoords = addCoordinates(square.squareCoords, x, y, z);
            }
            square.render();
            square.squareCoords = old;
        }
    }

    static void rotateSquare(Square square, float angle) {
        List<Float> coords = new ArrayList<>();
        for (float f : square.squareCoords) {
            coords.add(f);
        }
        coords.add(12, 1.5f);
        coords.add(9, 1.5f);
        coords.add(6, 1.5f);
        coords.add(3, 1.5f);
        float[] rotationMatrix = new float[16];
        float[] matrix = new float[16];
        for (int i = 0; i < 16; i++) {
            matrix[i] = coords.get(i) - 0.5f;
        }
        Matrix.setRotateM(rotationMatrix, 0, angle, 0, 1, 0);
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
        for (int i = 0; i < 12; i++) {
            result[i] = coords.get(i) + 0.5f;
        }
        square.squareCoords = result;
    }

    public static float[] addCoordinates(float[] old, float x, float y, float z) {
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

    static String getMultiStateBlockModel(int id, int metadata, String model) {
        switch (id) {
            //logs
            case 17:
            case 162:
                System.out.println("metadata2: " + metadata);
                if ((metadata & 12) == 4) model = model + "amongus" + "angle090";
                break;
            //double slabs
            case 43:
            case 125:
            case 181:
                model = model + "amongus" + model.replaceFirst("half", "upper");
                //slabs
            case 44:
            case 126:
            case 182:
                if ((metadata & 0x08) == 0x08) {
                    assert model.contains("half") : "Model doesn't contain half: " + model;
                    model = model.replaceFirst("half", "upper");
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }
        return model;
    }


    String getSpecialBlockModel(int id, int metadata, int x, int y, int z) {
        String type;
        switch (id) {
            case 2:
                if (ChunkColumn.getBlockId(x, y + 1, z) == 78 || ChunkColumn.getBlockId(x, y + 1, z) == 80) {
                    return "models/block/grass_snowed.json";
                } else {
                    return "models/block/grass_normal.json";
                }

                //fences
            case 85:
            case 113:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
                if (id == 85) type = "oak";
                else if (id == 113) type = "nether_brick";
                else if (id == 188) type = "spruce";
                else if (id == 189) type = "birch";
                else if (id == 190) type = "jungle";
                else if (id == 191) type = "dark_oak";
                else type = "acacia";
                boolean north = ChunkColumn.getBlockId(x, y, z - 1) != 0;
                boolean west = ChunkColumn.getBlockId(x - 1, y, z) != 0;
                boolean south = ChunkColumn.getBlockId(x, y, z + 1) != 0;
                boolean east = ChunkColumn.getBlockId(x + 1, y, z) != 0;
                int[] sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                int sum = sides[0] + sides[1] + sides[2] + sides[3];
                System.out.println("sum: " + sum);
                if (sum == 0) {
                    return "models/block/" + type + "_fence_post.json";
                } else if (sum == 1) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            return "models/block/" + type + "_fence_n.json";
                        }
                        angle += 90;
                    }
                } else if(sum ==2){
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i+3)%4] == 1) {
                            return "models/block/" + type + "_fence_ne.json";
                        }
                        if (sides[i] == 1 && sides[(i+2)%4] == 1) {
                            return "models/block/" + type + "_fence_ns.json";
                        }
                        angle += 90;
                    }
                } else if(sum ==3){
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            return "models/block/" + type + "_fence_nse.json";
                        }
                        angle += 90;
                    }
                } else if(sum ==4){
                    return "models/block/" + type + "_fence_nsew.json";
                    }
            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }
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
