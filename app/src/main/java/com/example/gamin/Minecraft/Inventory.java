package com.example.gamin.Minecraft;

import com.example.gamin.R;
import com.example.gamin.Render.ItemModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inventory {
    public static final Map<Byte, Inventory> inventoryMap = new HashMap<>();
    private final String name;
    private final List<InventorySection> sections = new ArrayList<>();
    public Slot[] contents;
    public float[][] itemModelCoords;
    public int resId;
    private int size;
    private int width;
    private int height;

    public Inventory(byte id, String name, String type, int size) {
        this.name = name;
        this.size = size;
        createContents(type);
        inventoryMap.put(id, this);
    }

    private void createContents(String type) {
        switch (type) {
            case "inventory": {
                contents = new Slot[45];
                resId = R.drawable.inventory;
                width = 176;
                height = 166;
                InventorySection craftOutputSection = new InventorySection(144, -51, 1, 1);
                InventorySection craftSection = new InventorySection(88, -41, 2, 2);
                InventorySection armorSection = new InventorySection(8, -23, 1, 4);
                InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                sections.addAll(List.of(craftOutputSection, craftSection, armorSection, playerSection, hotbarSection));
                break;
            }
            case "minecraft:chest":
                if (size == 27) {
                    contents = new Slot[63];
                    resId = R.drawable.generic_27;
                    width = 176;
                    height = 168;
                    InventorySection chestSection = new InventorySection(8, -33, 9, 3);
                    InventorySection playerSection = new InventorySection(8, -101, 9, 3);
                    InventorySection hotbarSection = new InventorySection(8, -159, 9, 1);
                    sections.addAll(List.of(chestSection, playerSection, hotbarSection));
                } else if (size == 54) {
                    contents = new Slot[90];
                    resId = R.drawable.generic_54;
                    width = 176;
                    height = 222;
                    InventorySection chestSection = new InventorySection(8, -33, 9, 6);
                    InventorySection playerSection = new InventorySection(8, -155, 9, 3);
                    InventorySection hotbarSection = new InventorySection(8, -213, 9, 1);
                    sections.addAll(List.of(chestSection, playerSection, hotbarSection));
                }
                break;
            case "minecraft:crafting_table": {
                contents = new Slot[46];
                resId = R.drawable.crafting_table;
                width = 176;
                height = 166;
                InventorySection craftingResultSection = new InventorySection(124, -50, 1, 1);
                InventorySection craftingSection = new InventorySection(30, -32, 3, 3);
                InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                sections.addAll(List.of(craftingResultSection, craftingSection, playerSection, hotbarSection));
                break;
            }
            case "minecraft:furnace": {
                contents = new Slot[39];
                resId = R.drawable.furnace;
                width = 176;
                height = 166;
                InventorySection ingredientSection = new InventorySection(56, -32, 1, 1);
                InventorySection fuelSection = new InventorySection(56, -68, 1, 1);
                InventorySection resultSection = new InventorySection(116, -50, 1, 1);
                InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                sections.addAll(List.of(ingredientSection, fuelSection, resultSection, playerSection, hotbarSection));
                break;
            }
            case "minecraft:dispenser":
            case "minecraft:dropper": {
                contents = new Slot[45];
                resId = R.drawable.dispenser;
                width = 176;
                height = 166;
                InventorySection dispenserSection = new InventorySection(62, -32, 3, 3);
                InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                sections.addAll(List.of(dispenserSection, playerSection, hotbarSection));
                break;
            }
            case "minecraft:enchanting_table": {
                contents = new Slot[38];
                resId = R.drawable.enchanting_table;
                width = 176;
                height = 166;
                InventorySection itemSection = new InventorySection(15, -62, 1, 1);
                InventorySection lapisSection = new InventorySection(35, -62, 1, 1);
                InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                sections.addAll(List.of(itemSection, lapisSection, playerSection, hotbarSection));
                break;
            }
            case "minecraft:brewing_stand": {
                contents = new Slot[40];
                resId = R.drawable.brewing_stand_gui;
                width = 176;
                height = 166;
                InventorySection potion1Section = new InventorySection(56, -61, 1, 1);
                InventorySection potion2Section = new InventorySection(79, -68, 1, 1);
                InventorySection potion3Section = new InventorySection(102, -61, 1, 1);
                InventorySection ingredientSection = new InventorySection(79, -32, 1, 1);
                InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                sections.addAll(List.of(potion1Section, potion2Section, potion3Section, ingredientSection, playerSection, hotbarSection));
            }
            break;
            case "minecraft:villager": {
                contents = new Slot[39];
                resId = R.drawable.villager;
                width = 176;
                height = 166;
                InventorySection input1Section = new InventorySection(36, -68, 1, 1);
                InventorySection input2Section = new InventorySection(62, -68, 1, 1);
                InventorySection resultSection = new InventorySection(120, -68, 1, 1);
                InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                sections.addAll(List.of(input1Section, input2Section, resultSection, playerSection, hotbarSection));
                break;
            }
            case "minecraft:beacon": {
                contents = new Slot[37];
                resId = R.drawable.beacon_gui;
                width = 230;
                height = 219;
                InventorySection paymentSection = new InventorySection(136, -125, 1, 1);
                InventorySection playerSection = new InventorySection(36, -152, 9, 3);
                InventorySection hotbarSection = new InventorySection(36, -210, 9, 1);
                sections.addAll(List.of(paymentSection, playerSection, hotbarSection));
                break;
            }
            case "minecraft:anvil": {
                contents = new Slot[39];
                resId = R.drawable.anvil;
                width = 176;
                height = 166;
                InventorySection inputSection = new InventorySection(27, -62, 1, 1);
                InventorySection materialSection = new InventorySection(76, -62, 1, 1);
                InventorySection resultSection = new InventorySection(134, -62, 1, 1);
                InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                sections.addAll(List.of(inputSection, materialSection, resultSection, playerSection, hotbarSection));
                break;
            }
            case "minecraft:hopper": {
                contents = new Slot[41];
                resId = R.drawable.hopper_gui;
                width = 176;
                height = 133;
                InventorySection hopperSection = new InventorySection(44, -35, 5, 1);
                InventorySection playerSection = new InventorySection(8, -66, 9, 3);
                InventorySection hotbarSection = new InventorySection(8, -124, 9, 1);
                sections.addAll(List.of(hopperSection, playerSection, hotbarSection));
                break;
            }
            case "EntityHorse": //TODO: not sure if this is correct
                if (size == 2) {
                    contents = new Slot[38];
                    resId = R.drawable.horse;
                    width = 176;
                    height = 166;
                    InventorySection horseSection = new InventorySection(8, -33, 1, 2);
                    InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                    InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                    sections.addAll(List.of(horseSection, playerSection, hotbarSection));
                } else if (size == 17) {
                    contents = new Slot[53];
                    resId = R.drawable.horse;
                    width = 176;
                    height = 166;
                    InventorySection horseSection = new InventorySection(8, -33, 1, 2);
                    InventorySection chestSection = new InventorySection(80, -33, 5, 3);
                    InventorySection playerSection = new InventorySection(8, -99, 9, 3);
                    InventorySection hotbarSection = new InventorySection(8, -157, 9, 1);
                    sections.addAll(List.of(horseSection, chestSection, playerSection, hotbarSection));
                }
                break;
        }
        if (contents == null) {
            throw new IllegalArgumentException("Unknown inventory type: " + type + " with size " + size);
        }
        itemModelCoords = new float[contents.length][];
    }

    public float[] getCoords(float ratio) {
        float heightInRenderer = ratio * 0.9f;
        float widthInRenderer = heightInRenderer * width / height;
        return new float[]{
                -widthInRenderer, heightInRenderer, -1,
                -widthInRenderer, -heightInRenderer, -1,
                widthInRenderer, -heightInRenderer, -1,
                -widthInRenderer, heightInRenderer, -1,
                widthInRenderer, -heightInRenderer, -1,
                widthInRenderer, heightInRenderer, -1
        };
    }

    public float[] getTexCoords() {
        float widthRatio = width / 256f;
        float heightRatio = height / 256f;
        return new float[]{
                0, 0,
                0, heightRatio,
                widthRatio, heightRatio,
                0, 0,
                widthRatio, heightRatio,
                widthRatio, 0
        };
    }

    public float[] getColors() {
        return new float[]{
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 1, 1, 1
        };
    }

    public void insertItem(int index, Slot item, float ratio) {
        contents[index] = item;
        int sectionIndex = index;
        for (InventorySection section : sections) {
            if (sectionIndex >= section.xSlots * section.ySlots) {
                sectionIndex -= section.xSlots * section.ySlots;
                continue;
            }
            itemModelCoords[index] = section.insertItem(sectionIndex, item.itemModel, ratio);
            return;
        }
        throw new IllegalArgumentException("Slot " + index + " is out of bounds for inventory " + name);
    }

    public int getClickedSlotIndex(float rendererX, float rendererY, float ratio) {

        //get pixel coordinates of the clicked point relative to the middle of the Inventory
        float pixel = 0.9f * ratio / (height / 2.0f);
        float pixelX = rendererX / pixel;
        float pixelY = rendererY / pixel;

        int index = 0;
        for (InventorySection inventorySection : sections) {
            int slotIndex = inventorySection.getClickedSlotIndex(pixelX + (float) width / 2, pixelY + (float) height / 2);
            if (slotIndex != -1) {
                return index + slotIndex;
            }
            index += inventorySection.xSlots * inventorySection.ySlots;
        }
        return -1;
    }

    private class InventorySection {
        private final int startX;
        private final int startY;
        private final int xSlots;
        private final int ySlots;

        private InventorySection(int startX, int startY, int xSlots, int ySlots) {
            this.startX = startX;
            this.startY = height + startY;
            this.xSlots = xSlots;
            this.ySlots = ySlots;
        }

        private float[] insertItem(int slot, ItemModel itemModel, float ratio) {
            int slotX = slot % xSlots;
            int slotY = slot / xSlots;

            //calculate relative pixel coordinates to middle of the Inventory
            int middleX = width / 2;
            int middleY = height / 2;
            int relativeX = startX + slotX * 18 - middleX;
            int relativeY = startY - slotY * 18 - middleY;

            //calculate one pixel in the screen
            float pixel = 0.9f * ratio / (height / 2.0f);

            //return the coordinates of the bottom left corner of the slot
            return itemModel.getCoordinatesInInventory(relativeX * pixel, relativeY * pixel, pixel);
        }

        private int getClickedSlotIndex(float pixelX, float pixelY) {
            int xIndex = (int) Math.floor((pixelX - startX) / 18);
            int yIndex = (int) Math.floor((startY + 18 - pixelY) / 18);
            if (xIndex < 0 || xIndex >= xSlots || yIndex < 0 || yIndex >= ySlots) {
                return -1;
            }
            return yIndex * xSlots + xIndex;
        }
    }

}
