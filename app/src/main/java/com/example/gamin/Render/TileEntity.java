package com.example.gamin.Render;

import android.content.Context;

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
    public BlockModel model;

    public TileEntity(Context context, int id, int metadata) {
        if (id < 0) id += 256;
        if (!models.containsKey(id + " " + metadata)) {
            String model = "bat.jem";
            String texture = "entity/bat";
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
                    model = "legacy/chest_14.jem";
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
                    model = "head_skeleton.jem";
                    texture = "entity/skeleton/skeleton";
                    if (metadata == 1) {
                        texture = "entity/skeleton/wither_skeleton";
                    } else if (metadata == 2) {
                        texture = "entity/zombie/zombie";
                    } else if (metadata == 3) {
                        texture = "entity/steve";
                    } else if (metadata == 4) {
                        texture = "entity/creeper/creeper";
                    }
                    break;
            }
            List<Square> modelSquares = ModelLoader.readJemModel(context, model, texture, angle);
            TileEntity.models.put(id + " " + metadata, modelSquares);
        }

        List<Square> modelSquares = TileEntity.models.get(id + " " + metadata);
        assert modelSquares != null;

        List<Square> squares = new ArrayList<>();
        for (Square modelSquare : modelSquares) {
            Square square = modelSquare.copy();
            squares.add(square);
        }
        this.model = new BlockModel(squares, TextureAtlas.atlases.get("entity"), (byte) -1);
    }

}
