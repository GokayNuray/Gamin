package com.example.gamin.Minecraft;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.gamin.Render.ItemModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @noinspection ResultOfMethodCallIgnored
 */
public class Slot {
    public static final Map<Integer, JSONObject> blocksMap = new HashMap<>();
    public static final Map<Integer, JSONObject> itemsMap = new HashMap<>();

    private final byte count;
    private final byte damage;
    private final byte metaData;
    private final JSONObject nbt;
    public ItemModel itemModel;
    public String displayName;
    private String textId;

    public Slot(Context context, Short id, byte count, byte damage, byte metaData, JSONObject nbt) {

        this.count = count;
        this.damage = damage;
        this.metaData = metaData;
        this.nbt = nbt;
        Log.v("Slot nbt", nbt.toString());
        JSONObject item = new JSONObject();

        try {
            if (id < 0) id = (short) (id + 256);
            if (id < 255) {
                item = blocksMap.get(Integer.valueOf(id));
            } else {
                item = itemsMap.get(Integer.valueOf(id));
            }
            if (item != null) {
                this.textId = item.getString("name");
            } else {
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
            if (nbt.has("display")) {
                JSONObject display = nbt.getJSONObject("display");
                if (display.has("Name")) {
                    this.displayName = display.getString("Name");
                }
            }

            itemModel = ItemModel.getItemModel(context, id, metaData);

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
        for (int i = 0; i < blocksJson.length(); i++) {
            JSONObject jsonObject = blocksJson.getJSONObject(i);
            blocksMap.put(jsonObject.getInt("id"), jsonObject);
        }
        for (int i = 0; i < itemsJson.length(); i++) {
            JSONObject jsonObject = itemsJson.getJSONObject(i);
            itemsMap.put(jsonObject.getInt("id"), jsonObject);
        }
    }

    @NonNull
    @Override
    public String toString() {
        String lore = "";
        if (nbt.has("display")) {
            try {
                if (nbt.getJSONObject("display").has("Lore")) {
                    JSONArray loreArray = nbt.getJSONObject("display").getJSONArray("Lore");
                    for (int i = 0; i < loreArray.length(); i++) {
                        lore += loreArray.getString(i) + "\n";
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return displayName + "\n" + lore;
    }
}
