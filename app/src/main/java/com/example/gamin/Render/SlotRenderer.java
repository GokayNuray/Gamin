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
    static Set<Integer> multiStateBlocks = new HashSet<>(Arrays.asList(17, 26, 162, 43, 125, 181));
    static Map<String, List<Square>> models = new HashMap<>();
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
                newModel = model.replaceFirst("block", "item");
                try {
                    is = context.getAssets().open(newModel);
                } catch (Exception e2) {
                    System.out.println("bbModel not found: " + model + "  " + newModel);
                    newModel = "models/block/pumpkin_stem_growth3.json";
                }
            }
            if (multiStateBlocks.contains((int) id)) {
                newModel = getMultiStateBlockModel(id, metadata, newModel);
            }
            for (String s : newModel.split("amongus")) {
                if (s.startsWith("angle")) {
                    modelAngle = Float.parseFloat(s.substring(5));
                    continue;
                }
                is = context.getAssets().open(s);
                byte[] b = new byte[is.available()];
                is.read(b);
                is.close();
                JSONObject jsonObject = new JSONObject(new String(b));

                do {
                    JSONObject textures = jsonObject.optJSONObject("textures");
                    if (jsonObject.has("parent")) {
                        InputStream is2 = context.getAssets().open("models/" + jsonObject.getString("parent") + ".json");
                        byte[] b2 = new byte[is2.available()];
                        is2.read(b2);
                        is2.close();
                        jsonObject = new JSONObject(new String(b2));
                    }
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
                } while (jsonObject.has("parent"));
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
                    String[] faces = {"north", "west", "south", "east", "up", "down"};
                    int[][] iSquareCoords = {
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
                                    to[0], from[1], from[2],
                                    from[0], from[1], from[2],
                                    from[0], from[1], to[2]}
                    };
                    for (int j = 0; j < faces.length; j++) {
                        if (element.getJSONObject("faces").has(faces[j])) {
                            int[] iSquareCoords1 = iSquareCoords[j];
                            float[] squareCoords1 = intToFloat(iSquareCoords1, smallest, biggest);
                            float[] textureCoords1;
                            JSONObject face = element.getJSONObject("faces").getJSONObject(faces[j]);
                            if (face.has("uv")) {
                                JSONArray uv = face.getJSONArray("uv");
                                int[] iTextureCoords = new int[]{
                                        uv.getInt(0), uv.getInt(1),
                                        uv.getInt(0), uv.getInt(3),
                                        uv.getInt(2), uv.getInt(3),
                                        uv.getInt(2), uv.getInt(1)
                                };
                                textureCoords1 = intToFloat(iTextureCoords, smallest, biggest);
                            } else {
                                textureCoords1 = new float[]{
                                        0.0f, 0.0f,
                                        0.0f, 1.0f,
                                        1.0f, 1.0f,
                                        1.0f, 0.0f
                                };
                            }
                            float[] color2;
                            if (face.has("tintindex")) {
                                color2 = new float[]{0.0f, 1.0f, 0.0f, 1.0f};
                            } else {
                                color2 = color;
                            }
                            if (face.has("rotation")) {
                                textureCoords1 = rotateUV(textureCoords1, face.getInt("rotation"));
                            }
                            String texture1 = element.getJSONObject("faces").getJSONObject(faces[j]).getString("texture");
                            texture1 = texture1.substring(texture1.lastIndexOf("/") + 1);
                            int textureId1 = context.getResources().getIdentifier(texture1, "drawable", context.getPackageName());
                            Square square1 = new Square(context, squareCoords1, color2, textureCoords1, textureId1);
                            squares.add(square1);
                        }
                    }
                }
            }
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

    static float[] addCoordinates(float[] old, float x, float y, float z) {
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

    static float[] intToFloat(int[] in, int smallest, int biggest) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (((float) (in[i] - smallest)) / ((float) biggest - smallest))/*0.5f*/;
        }
        return out;
    }

    static String getMultiStateBlockModel(int id, int metadata, String model) {
        switch (id) {
            //logs
            case 17:
            case 162:
                if ((metadata & 12) == 4) model = model + "amongus" + "angle090";
                break;
            //beds
            case 26:
                if ((metadata & 8) == 8) {
                    model = "models/block/bed_head.json";
                } else {
                    model = "models/block/bed_foot.json";
                }
                if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 3) == 2) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //double slabs
            case 43:
            case 125:
            case 181:
                model = model + "amongus" + model.replaceFirst("half", "upper");
                break;
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
                if (sum == 0) {
                    return "models/block/" + type + "_fence_post.json";
                } else if (sum == 1) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            return "models/block/" + type + "_fence_n.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            return "models/block/" + type + "_fence_ne.json";
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            return "models/block/" + type + "_fence_ns.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            return "models/block/" + type + "_fence_nse.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    return "models/block/" + type + "_fence_nsew.json";
                }
            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }
    }

}
