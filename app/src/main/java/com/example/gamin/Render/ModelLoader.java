package com.example.gamin.Render;

import static com.example.gamin.Render.BlockModelUtils.getMultiStateBlockModel;

import android.content.Context;
import android.util.Log;

import com.example.gamin.Minecraft.Slot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModelLoader {

    private static final Set<Integer> multiStateBlocks = new HashSet<>(Arrays.asList(50, 75, 76, 17, 23, 158, 26, 27, 28, 66, 157, 29, 33, 34, 59, 60, 61, 62, 65, 69, 70, 72, 77, 86, 91, 93, 94, 96, 99, 100, 107, 115, 117, 120, 145, 183, 184, 185, 186, 187, 167, 143, 147, 148, 149, 150, 162, 43, 125, 141, 142, 154, 175, 181));


    private ModelLoader() {
    }

    static BlockModel loadBlockModel(Context context, short blockId, byte metadata) throws JSONException, IOException {
        String modelPath;
        blockId = blockId < 0 ? (short) (blockId + 256) : blockId;
        short slabId = blockId;
        if (blockId == 43 | blockId == 125 || blockId == 181) slabId++;
        JSONObject block = Slot.blocksMap.get((int) slabId);
        assert block != null : "Block is null" + blockId;
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
            modelPath = block.getString("blockModel");
        } else if (block.has("itemModel")) {
            modelPath = block.getString("itemModel");
        } else {
            modelPath = block.getString("displayName").trim().toLowerCase().replaceAll(" ", "_");
        }
        modelPath = "models/block/" + modelPath + ".json";

        try {
            context.getAssets().open(modelPath).close();
        } catch (Exception e) {
            modelPath = modelPath.replaceFirst("block", "item");
            try {
                context.getAssets().open(modelPath).close();
            } catch (Exception e2) {
                System.out.println("bbModel not found: " + modelPath + "  " + modelPath);
                modelPath = "models/block/pumpkin_stem_growth3.json";
            }
        }

        if (multiStateBlocks.contains((int) blockId)) {
            modelPath = getMultiStateBlockModel(blockId, metadata, modelPath);
        }

        List<Square> squares = new ArrayList<>();
        TextureAtlas atlas = loadFromPath(context, modelPath, squares);

        return new BlockModel(squares, atlas);
    }

    static ItemModel loadItemModel(Context context, short itemId, byte metadata) throws JSONException, IOException {
        String modelPath;
        JSONObject item = Slot.itemsMap.get((int) itemId);
        assert item != null : "Item is null" + itemId;
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
            modelPath = item.getString("itemModel");
        } else {
            modelPath = item.getString("displayName").trim().toLowerCase().replaceAll(" ", "_");
        }
        modelPath = "models/item/" + modelPath + ".json";

        try {
            context.getAssets().open(modelPath).close();
        } catch (Exception e) {
            System.out.println("bbModel not found: " + modelPath);
            modelPath = "models/item/iron_sword.json";
        }

        List<Square> squares = new ArrayList<>();
        TextureAtlas atlas = loadFromPath(context, modelPath, squares);

        for (Square square : squares) {
            //rotate the model if it is a block
            if (itemId >= 256) break;
            square.rotate(20, 0, 0.5f, 0.5f, 0.5f);
            square.rotate(-45, 1, 0.5f, 0.5f, 0.5f);
            square.rotate(0, 2, 0.5f, 0.5f, 0.5f);
            square.scale(0.75f, 0.75f, 0.75f);
            square.translate(0, 0f, -2.75f);
            square.splitCoords();
        }

        return new ItemModel(squares, atlas);
    }

    private static TextureAtlas loadFromPath(Context context, String path, List<Square> squares) throws JSONException, IOException {
        TextureAtlas atlas = null;
        float modelAngle = 0;
        float modelXAngle = 0;
        for (String s : path.split("amongus")) {
            if (s.startsWith("angle")) {
                modelAngle = Float.parseFloat(s.substring(5));
                continue;
            }
            if (s.startsWith("up")) {
                modelXAngle = 90;
                continue;
            }
            if (s.startsWith("flipped")) {
                modelXAngle = 180;
                continue;
            }
            if (s.startsWith("down")) {
                modelXAngle = 270;
                continue;
            }
            if (s.endsWith("json")) atlas = readJsonModel(context, s, squares);
        }
        if (modelXAngle != 0) {
            for (Square square : squares) {
                square.rotate(modelXAngle, 0, 0.5f, 0.5f, 0.5f);
                square.splitCoords();
            }
        }
        if (modelAngle != 0) {
            for (Square square : squares) {
                square.rotate(modelAngle, 1, 0.5f, 0.5f, 0.5f);
                square.splitCoords();
            }
        }

        assert atlas != null : "Atlas is null" + path;
        return atlas;
    }

    static TextureAtlas readJsonModel(Context context, String s, List<Square> modelSquares) throws IOException, JSONException {
        InputStream is = context.getAssets().open(s);
        byte[] b = new byte[is.available()];
        is.read(b);
        is.close();
        JSONObject jsonObject = new JSONObject(new String(b));
        if (jsonObject.toString().contains("entity")) {//TODO temporary workaround
            return readJsonModel(context, "models/item/iron_sword.json", modelSquares);
        }

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
        return readJsonObject(jsonObject, modelSquares);
    }

    static TextureAtlas readJsonObject(JSONObject jsonObject, List<Square> modelSquares) throws JSONException {
        TextureAtlas atlas = null;
        JSONArray elements = jsonObject.getJSONArray("elements");
        for (int i = 0; i < elements.length(); i++) {
            float[] from = new float[3];
            float[] to = new float[3];
            JSONObject element = elements.getJSONObject(i);

            JSONArray jFrom = element.getJSONArray("from");
            for (int j = 0; j < jFrom.length(); j++)
                from[j] = (float) (jFrom.getDouble(j) / 16);
            JSONArray jTo = element.getJSONArray("to");
            for (int j = 0; j < jTo.length(); j++) to[j] = (float) (jTo.getDouble(j) / 16);

            float[] fOrigin = null;
            int rotAngle = 0;
            String axis = "";
            if (element.has("rotation")) {
                JSONObject rotation = element.getJSONObject("rotation");
                fOrigin = new float[3];
                JSONArray jOrigin = rotation.getJSONArray("origin");
                for (int j = 0; j < jOrigin.length(); j++)
                    fOrigin[j] = (float) (jOrigin.getDouble(j) / 16);
                rotAngle = rotation.getInt("angle");
                axis = rotation.getString("axis");
            }
            String[] faces = {"north", "west", "south", "east", "up", "down"};
            float[][] squareCoords = Square.getRectangularPrism(to, from);
            for (int j = 0; j < faces.length; j++) {
                if (element.getJSONObject("faces").has(faces[j])) {
                    float[] squareCoords1 = squareCoords[j];
                    float[] textureCoords1;
                    JSONObject face = element.getJSONObject("faces").getJSONObject(faces[j]);
                    if (face.has("uv")) {
                        JSONArray uv = face.getJSONArray("uv");
                        textureCoords1 = new float[]{
                                (float) (uv.getDouble(0) / 16), (float) (uv.getDouble(1) / 16),
                                (float) (uv.getDouble(0) / 16), (float) (uv.getDouble(3) / 16),
                                (float) (uv.getDouble(2) / 16), (float) (uv.getDouble(3) / 16),
                                (float) (uv.getDouble(2) / 16), (float) (uv.getDouble(1) / 16)
                        };
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
                        color2 = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
                    }

                    if (face.has("rotation"))
                        textureCoords1 = Square.rotateUV(textureCoords1, face.getInt("rotation"));

                    String texture1 = element.getJSONObject("faces").getJSONObject(faces[j]).getString("texture");
                    String textureType = texture1.split("/")[0];
                    String textureName = texture1.substring(texture1.indexOf("/") + 1);
                    if (atlas == null) atlas = TextureAtlas.atlases.get(textureType);
                    assert atlas != null : "Atlas is null: " + textureType;
                    assert atlas.offsets.containsKey(textureName + ".png") : "Texture " + textureName + " not found in " + textureType + " atlas";
                    float offset = atlas.offsets.get(textureName + ".png");
                    int atlasWidth = atlas.width;
                    int atlasHeight = atlas.height;
                    float[] textureCoords2 = new float[textureCoords1.length];
                    for (int k = 0; k < textureCoords1.length; k++) {
                        if (k % 2 == 0) {
                            textureCoords2[k] = textureCoords1[k] * 16 / atlasWidth + offset;
                        } else {
                            textureCoords2[k] = textureCoords1[k] * 16 / atlasHeight;
                        }
                    }

                    Square square1 = new Square(squareCoords1, color2, textureCoords2);
                    if (element.has("rotation")) {
                        int rotationAxis = -1;
                        if (axis.equals("x")) rotationAxis = 0;
                        if (axis.equals("y")) rotationAxis = 1;
                        if (axis.equals("z")) rotationAxis = 2;
                        assert rotationAxis != -1;
                        square1.rotate(rotAngle, rotationAxis, fOrigin[0], fOrigin[1], fOrigin[2]);
                        square1.splitCoords();
                    }

                    if (face.has("cullface")) square1.isFace = true;
                    modelSquares.add(square1);
                }
            }
        }
        return atlas;
    }

    static List<Square> readJemModel(Context context, String model, String texture, int angle) {
        try {
            InputStream is = context.getAssets().open("models/cem/" + model);
            byte[] b = new byte[is.available()];
            is.read(b);
            is.close();
            JSONObject cem = new JSONObject(new String(b));
            JSONArray models = cem.getJSONArray("models");
            JSONArray elements = new JSONArray();
            for (int i = 0; i < models.length(); i++) {
                JSONObject part = models.getJSONObject(i);
                JSONArray translate = part.getJSONArray("translate");
                int[] translateArray = new int[]{translate.getInt(0), translate.getInt(1), translate.getInt(2)};

                if (!part.has("boxes")) {
                    continue;
                }
                JSONArray boxes = part.getJSONArray("boxes");
                for (int j = 0; j < boxes.length(); j++) {
                    JSONObject box = boxes.getJSONObject(j);
                    JSONArray coordinates = box.getJSONArray("coordinates");
                    JSONArray textureOffset = box.getJSONArray("textureOffset");
                    int[] coords = new int[]{coordinates.getInt(0) + 8, coordinates.getInt(1), coordinates.getInt(2) + 8};
                    int[] size = new int[]{coordinates.getInt(3), coordinates.getInt(4), coordinates.getInt(5)};
                    int[] offset = new int[]{textureOffset.getInt(0), textureOffset.getInt(1)};
                    JSONArray from = new JSONArray(coords);
                    JSONArray to = new JSONArray(new int[]{coords[0] + size[0], coords[1] + size[1], coords[2] + size[2]});
                    JSONObject faces = new JSONObject();
                    int[] upUv = {offset[0] + size[2], offset[1], offset[0] + size[0] + size[2], offset[1] + size[2]};
                    int[] downUv = {offset[0] + size[2] + size[0], offset[1], offset[0] + size[0] + size[2] + size[0], offset[1] + size[2]};
                    int[] westUv = {offset[0], offset[1] + size[2], offset[0] + size[2], offset[1] + size[2] + size[1]};
                    int[] northUv = {offset[0] + size[2], offset[1] + size[2], offset[0] + size[2] + size[0], offset[1] + size[2] + size[1]};
                    int[] eastUv = {offset[0] + size[2] + size[0], offset[1] + size[2], offset[0] + size[2] + size[0] + size[2], offset[1] + size[2] + size[1]};
                    int[] southUv = {offset[0] + size[2] + size[0] + size[2], offset[1] + size[2], offset[0] + size[2] + size[0] + size[2] + size[0], offset[1] + size[2] + size[1]};
                    faces.put("down", new JSONObject().put("uv", new JSONArray(downUv)).put("texture", texture));
                    faces.put("up", new JSONObject().put("uv", new JSONArray(upUv)).put("texture", texture));
                    faces.put("north", new JSONObject().put("uv", new JSONArray(northUv)).put("texture", texture));
                    faces.put("south", new JSONObject().put("uv", new JSONArray(southUv)).put("texture", texture));
                    faces.put("west", new JSONObject().put("uv", new JSONArray(westUv)).put("texture", texture));
                    faces.put("east", new JSONObject().put("uv", new JSONArray(eastUv)).put("texture", texture));
                    JSONObject element = new JSONObject();
                    element.put("from", from);
                    element.put("to", to);
                    element.put("faces", faces);
                    elements.put(element);
                }
            }
            JSONObject json = new JSONObject();
            json.put("textures", new JSONObject().put("texture", texture));
            json.put("elements", elements);

            List<Square> modelSquares = new ArrayList<>();
            readJsonObject(json, modelSquares);

            if (angle != 0) {
                for (Square square : modelSquares) {
                    square.rotate(angle, 1, 0.5f, 0.5f, 0.5f);
                    square.splitCoords();
                }
            }

            return modelSquares;

        } catch (IOException | JSONException e) {
            Log.e("TileEntity", "Error reading model" + model);
            throw new RuntimeException(e);
        }
    }
}
