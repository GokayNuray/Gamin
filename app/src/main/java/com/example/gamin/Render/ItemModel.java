package com.example.gamin.Render;

import android.content.Context;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemModel extends Model {

    private static final Map<Short, ItemModel> itemModels = new HashMap<>();

    public ItemModel(List<Square> squares, TextureAtlas textureAtlas) {
        this.squares.addAll(squares);
        this.textureAtlas = textureAtlas;
        setBuffers();
    }

    public static ItemModel getItemModel(Context context, short id, byte metadata) {
        short key = (short) (id << 8 | metadata);
        if (!itemModels.containsKey(key)) {
            try {
                ItemModel itemModel = ModelLoader.loadItemModel(context, id, metadata);
                itemModels.put(key, itemModel);
            } catch (JSONException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return itemModels.get(key);
    }
}
