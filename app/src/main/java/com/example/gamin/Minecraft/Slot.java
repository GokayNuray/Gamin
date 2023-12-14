package com.example.gamin.Minecraft;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** @noinspection ResultOfMethodCallIgnored*/
public class Slot {
    public static Map<Integer,JSONObject> blocksMap;
    public static Map<Integer,JSONObject> itemsMap;

    byte count;
    byte damage;
    byte metaData;
    JSONObject nbt;
    public String displayName;
    public String textId;
    public String itemModel;

    public Slot(Short id, byte count, byte damage, byte metaData,JSONObject nbt) {

        this.count = count;
        this.damage = damage;
        this.metaData = metaData;
        this.nbt = nbt;
        JSONObject item = new JSONObject();

        try {
            if (id<0) id = (short)(id + 256);
            if (id < 255) {
                item = blocksMap.get(Integer.valueOf(id));
            }
            else {
                item = itemsMap.get(Integer.valueOf(id));
            }
            if (item != null) {
                this.textId = item.getString("name");
            }
            else {
                System.out.println(id);
                System.out.println("AAAAAAAAAAA");
            }
            if (item == null) System.out.println(id + " is null");
            assert item != null;
            if (item.has("variations")) {
                JSONArray variations = item.getJSONArray("variations");
                //get the variation with the correct metadata
                for (int i = 0; i < variations.length(); i++) {
                    JSONObject variation = variations.getJSONObject(i);
                    if (variation.getInt("metadata") == metaData) {
                        item = variation;
                    }
                }
            }
            this.displayName = item.getString("displayName");
            if (item.has("itemModel")) {
                this.itemModel = item.getString("itemModel");
            }
            else {
                this.itemModel = displayName.trim().toLowerCase().replaceAll(" ","_");
            }

        } catch (JSONException e) {
            System.out.println(item);
            throw new RuntimeException(e);
        }


    }
    public static void loadAssetData(Context context) throws IOException, JSONException {
        InputStream is = context.getAssets().open("data/blocks.json");
        byte[] b = new byte[is.available()];
        is.read(b);
        JSONArray blocksJson = new JSONArray(new String(b));
        InputStream is2 = context.getAssets().open("data/items.json");
        byte[] b2 = new byte[is2.available()];
        is2.read(b2);
        JSONArray itemsJson = new JSONArray(new String(b2));

        is.close();
        is2.close();
        blocksMap = new HashMap<>();
        itemsMap = new HashMap<>();
        for (int i = 0; i < blocksJson.length(); i++) {
            JSONObject jsonObject = blocksJson.getJSONObject(i);
            blocksMap.put(jsonObject.getInt("id"),jsonObject);
        }
        for (int i = 0; i < itemsJson.length(); i++) {
            JSONObject jsonObject = itemsJson.getJSONObject(i);
            itemsMap.put(jsonObject.getInt("id"),jsonObject);
        }
    }
}
