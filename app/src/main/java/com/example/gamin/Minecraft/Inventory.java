package com.example.gamin.Minecraft;

import android.opengl.GLSurfaceView;
import android.widget.ImageButton;

import com.example.gamin.Render.YourRenderer;
import com.example.gamin.Utils.InventoryUtils;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    public static Map<Byte, Inventory> inventoryMap = new HashMap<>();
    public Slot[] contents;
    String name;

    public Inventory(byte id, String name, int size) {
        this.name = name;
        this.contents = new Slot[size];
        inventoryMap.put(id, this);
    }

    public void update(GLSurfaceView glSurfaceView, YourRenderer renderer, ImageButton[] imageButtons) {
        if (name.equals("playerInventory")) {
            for (int i = 9; i < 45; i++) {
                if (contents[i] != null) {
                    InventoryUtils.drawButton(glSurfaceView, renderer, contents[i].itemModel, imageButtons[i + 45]);
                }
            }
        }
    }


}
