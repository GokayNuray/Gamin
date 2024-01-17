package com.example.gamin.Render;

import android.content.Context;
import android.util.Log;

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
import java.util.Set;

public class TileEntity {

    //banners, chests, signs, skulls, and mob heads
    public static final Set<Integer> tileEntityIds = new HashSet<>(Arrays.asList(176, 177, 54, 130, 146, 63, 68, 144));
    private static final Map<String, List<Square>> models = new HashMap<>();
    public SlotRenderer slotRenderer;

    public TileEntity(Context context, int id, int metadata, int x, int y, int z) {
        Log.v("TileEntity", metadata + "");
        if (!models.containsKey(id + " " + metadata)) {
            String model = "";
            String texture = "";
            int angle = 0;
            switch (id) {
                case 176:
                    //banner
                    break;
                case 177:
                    //wall banner
                    break;
                case 54:
                    //chest
                    model = "legacy/chest_14";
                    texture = "entity/chest/normal";
                    if (metadata == 3) {
                        angle = 180;
                    } else if (metadata == 4) {
                        angle = 90;
                    } else if (metadata == 5) {
                        angle = 270;
                    }
                    break;
                case 130:
                    //ender chest
                    break;
                case 146:
                    //trapped chest
                    break;
                case 63:
                    //sign
                    break;
                case 68:
                    //wall sign
                    break;
                case 144:
                    //skull
                    break;
            }
            try {
                InputStream is = context.getAssets().open("models/cem/" + model + ".jem");
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
                SlotRenderer.readJsonObject(json, modelSquares);

                if (angle != 0) {
                    for (Square square : modelSquares) {
                        Log.v("TileEntity", angle + "");
                        SlotRenderer.rotateSquare(square, angle, 1, 0.5f, 0.5f, 0.5f);
                        square.splitCoords();
                    }
                }

                TileEntity.models.put(id + " " + metadata, modelSquares);
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }

        List<Square> modelSquares = TileEntity.models.get(id + " " + metadata);
        assert modelSquares != null;

        List<Square> squares = new ArrayList<>();
        for (Square modelSquare : modelSquares) {
            Square square = modelSquare.copy();
            SlotRenderer.addCoordinates(square.coords1, x, y, z);
            SlotRenderer.addCoordinates(square.coords2, x, y, z);
            squares.add(square);
        }
        slotRenderer = new SlotRenderer(squares);
    }

}
