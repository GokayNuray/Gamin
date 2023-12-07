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
    static Map<Integer,JSONObject> itemsMap;

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

        try {
            JSONObject item;
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
            assert item != null;
            if (metaData != 0) {
                item = item.getJSONArray("variations").getJSONObject(metaData);
            }
            this.displayName = item.getString("displayName");
            if (item.has("itemModel")) {
                this.itemModel = item.getString("itemModel");
            }
            else {
                this.itemModel = displayName.trim().toLowerCase().replaceAll(" ","_");
            }

        } catch (JSONException e) {
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
