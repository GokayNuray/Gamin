package com.example.gamin.Render;

import android.content.Context;

import com.example.gamin.Minecraft.Chunk;
import com.example.gamin.Minecraft.Slot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class BlockModelUtils {

    private static final Set<Integer> stairs = new HashSet<>(Arrays.asList(53, 67, 108, 109, 114, -128, -122, -121, -120, -10, -93, -92, -76));

    private BlockModelUtils() {
    }

    static String getMultiStateBlockModel(int id, int metadata, String model) {
        switch (id) {

            //logs
            case 17:
            case 162:
                if ((metadata & 12) == 4) model = model + "amongus" + "angle090";
                break;

            //dispenser and dropper
            case 23:
            case 158:
                if ((metadata & 7) == 0) {
                    model = model.replaceFirst(".json", "_vertical.json") + "amongus" + "flipped";
                } else if ((metadata & 7) == 1) {
                    model = model.replaceFirst(".json", "_vertical.json");
                } else if ((metadata & 7) == 3) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 7) == 4) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 7) == 5) {
                    model = model + "amongus" + "angle270";
                }
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

            //rails TODO add rescaling for raised rails
            case 27:
            case 28:
            case 66:
            case 157:
                String type = "normal_rail_";
                if (id == 27) type = "golden_rail_";
                else if (id == 28) type = "detector_rail_";
                else if (id == 157) type = "activator_rail_";
                if ((metadata & 8) == 8) {
                    if (id == 28) type = type + "powered_";
                    if ((id == 27) || (id == 157)) type = type + "active_";
                }
                if ((metadata & 7) == 0) {
                    model = "models/block/" + type + "flat.json";
                } else if ((metadata & 7) == 1) {
                    model = "models/block/" + type + "flat.json";
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 7) == 2) {
                    model = "models/block/" + type + "raised_ne.json";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 7) == 3) {
                    model = "models/block/" + type + "raised_sw.json";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 7) == 4) {
                    model = "models/block/" + type + "raised_ne.json";
                } else if ((metadata & 7) == 5) {
                    model = "models/block/" + type + "raised_sw.json";
                }
                if (id == 66) {
                    if (metadata == 6) {
                        model = "models/block/normal_rail_curved.json";
                    } else if (metadata == 7) {
                        model = "models/block/normal_rail_curved.json";
                        model = model + "amongus" + "angle090";
                    } else if (metadata == 8) {
                        model = "models/block/normal_rail_curved.json";
                        model = model + "amongus" + "angle180";
                    } else if (metadata == 9) {
                        model = "models/block/normal_rail_curved.json";
                        model = model + "amongus" + "angle270";
                    }
                }
                break;

            //pistons FIXME uv coordinates broken (i changed piston.json to fix one issue but idk what is wrong with this much things)
            case 29:
            case 33:
                if ((metadata & 8) == 8) {
                    model = "models/block/piston_extended_normal.json";
                } else {
                    if (id == 29) model = "models/block/sticky_piston.json";
                    else model = "models/block/piston_normal.json";
                }
                if ((metadata & 7) == 0) {
                    model = model + "amongus" + "down";
                } else if ((metadata & 7) == 1) {
                    model = model + "amongus" + "up";
                } else if ((metadata & 7) == 3) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 7) == 4) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 7) == 5) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //piston head
            case 34:
                if ((metadata & 8) == 0) {
                    model = "models/block/piston_head_normal.json";
                } else {
                    model = "models/block/piston_head_sticky.json";
                }
                if ((metadata & 7) == 0) {
                    model = model + "amongus" + "down";
                } else if ((metadata & 7) == 1) {
                    model = model + "amongus" + "up";
                } else if ((metadata & 7) == 3) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 7) == 4) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 7) == 5) {
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

            //torches
            case 50:
            case 75:
            case 76:
                model = "models/block/normal_torch.json";
                if (id == 75) model = "models/block/unlit_redstone_torch.json";
                else if (id == 76) model = "models/block/lit_redstone_torch.json";
                if ((metadata & 0x07) == 0x01) {
                    model = model.replaceFirst(".json", "_wall.json");
                } else if ((metadata & 0x07) == 0x02) {
                    model = model.replaceFirst(".json", "_wall.json");
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x03) {
                    model = model.replaceFirst(".json", "_wall.json");
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model.replaceFirst(".json", "_wall.json");
                    model = model + "amongus" + "angle90";
                }
                break;

            //wheat
            case 59:
                model = "models/block/wheat_stage" + (metadata & 0x07) + ".json";
                break;

            //farmland
            case 60:
                if ((metadata & 0x07) == 0x07) {
                    model = "models/block/farmland_moist.json";
                } else {
                    model = "models/block/farmland_dry.json";
                }
                break;

            //ladder and furnace
            case 61:
            case 62:
            case 65:
                if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 0x07) == 0x05) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //lever
            case 69:
                if (!((metadata & 0x08) == 0x08)) model = model.replaceFirst(".json", "_off.json");
                if ((metadata & 0x07) == 0x00) {
                    model = model + "amongus" + "flipped";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x07) == 0x01) {
                    model = model + "amongus" + "down";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x07) == 0x02) {
                    model = model + "amongus" + "down";
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "down";
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model + "amongus" + "down";
                } else if ((metadata & 0x07) == 0x06) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 0x07) == 0x07) {
                    model = model + "amongus" + "flipped";
                    model = model + "amongus" + "angle180";
                }
                break;

            //pressure plates
            case 70:
            case 72:
            case 147:
            case 148:
                type = "stone";
                if (id == 72) type = "wooden";
                else if (id == 147) type = "light";
                else if (id == 148) type = "heavy";
                if (metadata == 0) {
                    model = "models/block/" + type + "_pressure_plate_up.json";
                } else {
                    model = "models/block/" + type + "_pressure_plate_down.json";
                }
                break;

            //buttons
            case 77:
            case 143:
                type = "stone";
                if (id == 143) type = "wooden";
                if ((metadata & 0x08) == 0x08) {
                    model = "models/block/" + type + "_button_pressed.json";
                } else {
                    model = "models/block/" + type + "_button.json";
                }
                if ((metadata & 0x07) == 0x00) {
                    model = model + "amongus" + "flipped";
                } else if ((metadata & 0x07) == 0x01) {
                    model = model + "amongus" + "up";
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 0x07) == 0x02) {
                    model = model + "amongus" + "up";
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "up";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model + "amongus" + "up";
                    model = model + "amongus" + "angle180";

                }
                break;

            //pumpkin and jack o lantern
            case 86:
            case 91:
                if ((metadata & 0x07) == 0x00) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x01) {
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //repeaters TODO locked repeaters
            case 93:
            case 94:
                type = "repeater";
                if (id == 94) type = "repeater_on";
                if ((metadata & 12) == 0) {
                    model = "models/block/" + type + "_1tick.json";
                } else if ((metadata & 12) == 4) {
                    model = "models/block/" + type + "_2tick.json";
                } else if ((metadata & 12) == 8) {
                    model = "models/block/" + type + "_3tick.json";
                } else {
                    model = "models/block/" + type + "_4tick.json";
                }
                if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 3) == 0) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle090";
                }
                break;

            //trapdoors
            case 96:
            case 167:
                type = "wooden";
                if (id == 167) type = "iron";
                if ((metadata & 0x04) == 0x04) {
                    model = "models/block/" + type + "_trapdoor_open.json";
                } else if ((metadata & 0x08) == 0x08) {
                    model = "models/block/" + type + "_trapdoor_top.json";
                } else {
                    model = "models/block/" + type + "_trapdoor_bottom.json";
                }
                if ((metadata & 0x03) == 0x01) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x03) == 0x02) {
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 0x03) == 0x03) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //mushroom blocks
            case 99:
            case 100:
                type = "brown_mushroom_block";
                if (id == 100) type = "red_mushroom_block";

                if (metadata == 1) model = "models/block/" + type + "_nw.json";
                else if (metadata == 2) model = "models/block/" + type + "_n.json";
                else if (metadata == 3) model = "models/block/" + type + "_ne.json";
                else if (metadata == 4) model = "models/block/" + type + "_w.json";
                else if (metadata == 5) model = "models/block/" + type + "_c.json";
                else if (metadata == 6) model = "models/block/" + type + "_e.json";
                else if (metadata == 7) model = "models/block/" + type + "_sw.json";
                else if (metadata == 8) model = "models/block/" + type + "_s.json";
                else if (metadata == 9) model = "models/block/" + type + "_se.json";
                else if (metadata == 10) model = "models/block/" + type + "_stem.json";
                else if (metadata == 14) model = "models/block/" + type + "_cap_all.json";
                else if (metadata == 15) model = "models/block/" + type + "_stem_all.json";
                else model = "models/block/" + type + "_inside_all.json";
                break;

            //fence gates TODO connect fences to this
            case 107:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
                type = "oak";
                if (id == 183) type = "spruce";
                else if (id == 184) type = "birch";
                else if (id == 185) type = "jungle";
                else if (id == 186) type = "dark_oak";
                else if (id == 187) type = "acacia";

                if ((metadata & 0x04) == 0x04) {
                    model = "models/block/" + type + "_fence_gate_open.json";
                } else {
                    model = "models/block/" + type + "_fence_gate_closed.json";
                }
                if ((metadata & 0x03) == 0x02) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x03) == 0x01) {
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 0x03) == 0x03) {
                    model = model + "amongus" + "angle090";
                }
                break;

            //nether wart
            case 115:
                int stage3 = 0;
                if (metadata > 0) stage3 = 1;
                if (metadata > 2) stage3 = 2;
                model = "models/block/nether_wart_stage" + stage3 + ".json";
                break;

            //brewing stand
            case 117:
                if (metadata == 0) {
                    model = "models/block/brewing_stand_empty";
                } else {
                    model = "models/block/brewing_stand_bottles_";
                }
                if ((metadata & 1) == 1) model = model + 1;
                if ((metadata & 2) == 2) model = model + 2;
                if ((metadata & 4) == 4) model = model + 3;
                model = model + ".json";
                break;

            //end portal frame
            case 120:
                if ((metadata & 4) == 4) {
                    model = "models/block/end_portal_frame_filled.json";
                } else {
                    model = "models/block/end_portal_frame_empty.json";
                }
                if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle090";
                } else if ((metadata & 3) == 0) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //comparator
            case 149:
            case 150:
                if ((metadata & 8) == 8) {
                    model = "models/block/comparator_lit";
                } else {
                    model = "models/block/comparator_unlit";
                }
                if ((metadata & 4) == 4) {
                    model = model + "_subtract.json";
                } else {
                    model = model + ".json";
                }
                if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 3) == 2) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle090";
                }
                break;

            //carrots
            case 141:
                int stage = 0;
                if (metadata > 1) stage = 1;
                if (metadata > 3) stage = 2;
                if (metadata > 6) stage = 3;
                model = "models/block/carrots_stage" + stage + ".json";
                break;

            //potatoes
            case 142:
                int stage2 = 0;
                if (metadata > 1) stage2 = 1;
                if (metadata > 3) stage2 = 2;
                if (metadata > 6) stage2 = 3;
                model = "models/block/potatoes_stage" + stage2 + ".json";
                break;

            //anvil
            case 145:
                if ((metadata & 12) == 0) {
                    model = "models/block/anvil_undamaged.json";
                } else if ((metadata & 12) == 4) {
                    model = "models/block/anvil_slightly_damaged.json";
                } else if ((metadata & 12) == 8) {
                    model = "models/block/anvil_very_damaged.json";
                }
                if ((metadata & 3) == 1) {
                    model = model + "amongus" + "angle270";
                } else if ((metadata & 3) == 2) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 3) == 3) {
                    model = model + "amongus" + "angle090";
                }
                break;

            //hoppers
            case 154:
                if ((metadata & 0x07) == 0x00) {
                    model = "models/block/hopper_down.json";
                    break;
                } else {
                    model = "models/block/hopper_side.json";
                }
                if ((metadata & 0x07) == 0x03) {
                    model = model + "amongus" + "angle180";
                } else if ((metadata & 0x07) == 0x04) {
                    model = model + "amongus" + "angle90";
                } else if ((metadata & 0x07) == 0x05) {
                    model = model + "amongus" + "angle270";
                }
                break;

            //double plants FIXME fix top block always being double grass
            case 175:
                model = model.substring(model.indexOf("item/") + 5, model.indexOf(".json"));
                model = model.replace("double_", "");
                if ((metadata & 0x08) == 0x08) {
                    model = "models/block/double_" + model + "_top.json";
                } else {
                    model = "models/block/double_" + model + "_bottom.json";
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }
        return model;
    }

    static int getSpecialNumber(int id, int metadata, int x, int y, int z) {
        byte specialNumber = 0;
        switch (id) {
            case 2://0x01 means grass with snow
                if (Chunk.getBlockId(x, y + 1, z) == 78 || Chunk.getBlockId(x, y + 1, z) == 80) {
                    specialNumber = 1;
                }
                break;

            //stairs
            case 53:
            case 67:
            case 108:
            case 109:
            case 114:
            case 128:
            case 134:
            case 135:
            case 136:
            case 156:
            case 163:
            case 164:
            case 180://0x01 means neighbor1 is a stair 0x02 and 0x04 is neighbo1's metadata 0x08 means neighbor2 is a stair 0x10 and 0x20 is neighbo2's metadata

                //if facing north or south
                if ((metadata & 2) == 2) {
                    //block in north
                    short neighbor1 = Chunk.getBlock(x, y, z - 1);
                    int neighbor1Id = Chunk.getBlockId(neighbor1);
                    int neighbor1Meta = Chunk.getBlockMetaData(neighbor1);
                    //block in south
                    short neighbor2 = Chunk.getBlock(x, y, z + 1);
                    int neighbor2Id = Chunk.getBlockId(neighbor2);
                    int neighbor2Meta = Chunk.getBlockMetaData(neighbor2);

                    if (stairs.contains(neighbor1Id)) {
                        specialNumber |= 0x01;
                        specialNumber |= neighbor1Meta << 1;
                    }
                    if (stairs.contains(neighbor2Id)) {
                        specialNumber |= 0x08;
                        specialNumber |= neighbor2Meta << 4;
                    }
                }

                //if facing east or west
                else {
                    //block in west
                    short neighbor1 = Chunk.getBlock(x - 1, y, z);
                    int neighbor1Id = Chunk.getBlockId(neighbor1);
                    int neighbor1Meta = Chunk.getBlockMetaData(neighbor1);
                    //block in east
                    short neighbor2 = Chunk.getBlock(x + 1, y, z);
                    int neighbor2Id = Chunk.getBlockId(neighbor2);
                    int neighbor2Meta = Chunk.getBlockMetaData(neighbor2);
                    if (stairs.contains(neighbor1Id)) {
                        specialNumber |= 0x01;
                        specialNumber |= neighbor1Meta << 1;
                    }
                    if (stairs.contains(neighbor2Id)) {
                        specialNumber |= 0x08;
                        specialNumber |= neighbor2Meta << 4;
                    }
                }
                break;

            //fences
            case 85:
            case 113:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192://0x01 means there is a block in north 0x02 means there is a block in west 0x04 means there is a block in south 0x08 means there is a block in east

                //block in north
                short neighbor1 = Chunk.getBlock(x, y, z - 1);
                int neighbor1Id = Chunk.getBlockId(neighbor1);
                //block in west
                short neighbor2 = Chunk.getBlock(x - 1, y, z);
                int neighbor2Id = Chunk.getBlockId(neighbor2);
                //block in south
                short neighbor3 = Chunk.getBlock(x, y, z + 1);
                int neighbor3Id = Chunk.getBlockId(neighbor3);
                //block in east
                short neighbor4 = Chunk.getBlock(x + 1, y, z);
                int neighbor4Id = Chunk.getBlockId(neighbor4);

                if (neighbor1Id != 0) specialNumber |= 0x01;
                if (neighbor2Id != 0) specialNumber |= 0x02;
                if (neighbor3Id != 0) specialNumber |= 0x04;
                if (neighbor4Id != 0) specialNumber |= 0x08;
                break;

            //doors
            case 64:
            case 71:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197://3 least significant bits specify the other part of the door

                if ((metadata & 8) == 8) {
                    specialNumber |= Chunk.getBlockMetaData(Chunk.getBlock(x, y - 1, z)) & 0x07;
                } else {
                    specialNumber |= Chunk.getBlockMetaData(Chunk.getBlock(x, y + 1, z)) & 0x07;
                }
                break;

            //stems
            case 104://-1 if there is no adjacent pumpkin and 0 1 2 3 specify the angle

                int isPumpkinFruit = -1;
                if (Chunk.getBlockId(x + 1, y, z) == 86) isPumpkinFruit = 2;
                if (Chunk.getBlockId(x - 1, y, z) == 86) isPumpkinFruit = 0;
                if (Chunk.getBlockId(x, y, z + 1) == 86) isPumpkinFruit = 1;
                if (Chunk.getBlockId(x, y, z - 1) == 86) isPumpkinFruit = 3;
                specialNumber = (byte) isPumpkinFruit;
                break;

            case 105://-1 if there is no adjacent melon and 0 1 2 3 specify the angle

                int isMelonFruit = -1;
                if (Chunk.getBlockId(x + 1, y, z) == 103) isMelonFruit = 2;
                if (Chunk.getBlockId(x - 1, y, z) == 103) isMelonFruit = 0;
                if (Chunk.getBlockId(x, y, z + 1) == 103) isMelonFruit = 1;
                if (Chunk.getBlockId(x, y, z - 1) == 103) isMelonFruit = 3;
                specialNumber = (byte) isMelonFruit;
                break;

            //vines
            case 106://0x01 means there is a block above

                if (Chunk.getBlockId(x, y + 1, z) != 0) specialNumber = 1;
                break;

            //tripwire hook
            case 131://0x01 means there is no block below

                if (Chunk.getBlockId(x, y - 1, z) == 0) specialNumber = 1;
                break;

            //tripwire
            case 132://0x01 means there is no block below, 0x02 means there is tripwire in north, 0x04 means there is tripwire in west, 0x08 means there is tripwire in south, 0x10 means there is tripwire in east

                //block below
                if (Chunk.getBlockId(x, y - 1, z) == 0) specialNumber |= 0x01;
                if (Chunk.getBlockId(x, y, z - 1) == -124) specialNumber |= 0x02;
                if (Chunk.getBlockId(x - 1, y, z) == -124) specialNumber |= 0x04;
                if (Chunk.getBlockId(x, y, z + 1) == -124) specialNumber |= 0x08;
                if (Chunk.getBlockId(x + 1, y, z) == -124) specialNumber |= 0x10;
                break;

            //walls
            case 139://0x01 means there is a block in north, 0x02 means there is a block in west, 0x04 means there is a block in south, 0x08 means there is a block in east and 0x10 means there is a block above

                if (Chunk.getBlockId(x, y, z - 1) != 0) specialNumber |= 0x01;
                if (Chunk.getBlockId(x - 1, y, z) != 0) specialNumber |= 0x02;
                if (Chunk.getBlockId(x, y, z + 1) != 0) specialNumber |= 0x04;
                if (Chunk.getBlockId(x + 1, y, z) != 0) specialNumber |= 0x08;
                if (Chunk.getBlockId(x, y + 1, z) != 0) specialNumber |= 0x10;
                break;

        }
        return specialNumber;
    }

    static BlockModel getSpecialBlockModel(Context context, int id, int metadata, int specialNumber) throws JSONException, IOException {
        String type = null;
        int angle = 0;
        boolean upsideDown = false;
        String model = "";
        a:
        switch (id) {
            case 2:
                if (specialNumber == 1) {
                    model = "models/block/grass_snowed.json";
                    break;
                } else {
                    model = "models/block/grass_normal.json";
                    break;
                }

                //stairs
            case 53:
            case 67:
            case 108:
            case 109:
            case 114:
            case 128:
            case 134:
            case 135:
            case 136:
            case 156:
            case 163:
            case 164:
            case 180:
                try {
                    JSONObject block = Objects.requireNonNull(Slot.blocksMap.get(id));
                    if (block.has("itemModel")) {
                        type = block.getString("itemModel");
                    } else {
                        type = block.getString("displayName").trim().toLowerCase().replaceAll(" ", "_");
                    }
                    type = type.replaceFirst("_stairs", "");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //FIXME if angle is 0, it doesn't work(it doesnt call rotateSquare)
                if ((metadata & 3) == 0) angle = 360;
                if ((metadata & 3) == 1) angle = 180;
                if ((metadata & 3) == 2) angle = 270;
                if ((metadata & 3) == 3) angle = 90;
                if ((metadata & 4) == 4) upsideDown = true;
                //if facing north or south
                if ((metadata & 2) == 2) {
                    //block in north
                    //short neighbor1 = Chunk.getBlock(x, y, z - 1);
                    //int neighbor1Id = Chunk.getBlockId(neighbor1);
                    int neighbor1Meta = specialNumber >> 1;
                    //block in south
                    //short neighbor2 = Chunk.getBlock(x, y, z + 1);
                    //int neighbor2Id = Chunk.getBlockId(neighbor2);
                    int neighbor2Meta = specialNumber >> 4;
                    if (((specialNumber & 0x01) == 0x01) && (neighbor1Meta & 0x02) == 0) {
                        if ((metadata & 1) == 1) {
                            if ((neighbor1Meta & 1) == 1) angle += 90;
                            model = "models/block/" + type + "_outer_stairs.json";
                            break;
                        } else {
                            if ((neighbor1Meta & 1) == 0) angle += 90;
                            model = "models/block/" + type + "_inner_stairs.json";
                            break;
                        }
                    }
                    if (((specialNumber & 0x08) == 0x08) && (neighbor2Meta & 0x02) == 0) {
                        if ((metadata & 1) == 1) {
                            if ((neighbor2Meta & 1) == 1) angle += 90;
                            model = "models/block/" + type + "_inner_stairs.json";
                            break;
                        } else {
                            if ((neighbor2Meta & 1) == 0) angle += 90;
                            model = "models/block/" + type + "_outer_stairs.json";
                            break;
                        }
                    }
                }
                //if facing east or west
                else {
                    //block in west
                    //short neighbor1 = Chunk.getBlock(x - 1, y, z);
                    //int neighbor1Id = Chunk.getBlockId(neighbor1);
                    int neighbor1Meta = specialNumber >> 1;
                    //block in east
                    //short neighbor2 = Chunk.getBlock(x + 1, y, z);
                    //int neighbor2Id = Chunk.getBlockId(neighbor2);
                    int neighbor2Meta = specialNumber >> 4;
                    if (((specialNumber & 0x01) == 0x01) && (neighbor1Meta & 2) == 2) {
                        if ((metadata & 1) == 1) {
                            if ((neighbor1Meta & 1) == 0) angle += 90;
                            model = "models/block/" + type + "_outer_stairs.json";
                            break;
                        } else {
                            if ((neighbor1Meta & 1) == 1) angle += 90;
                            model = "models/block/" + type + "_inner_stairs.json";
                            break;
                        }
                    }
                    if (((specialNumber & 0x08) == 0x08) && (neighbor2Meta & 0x02) == 2) {
                        if ((metadata & 1) == 1) {
                            if ((neighbor2Meta & 1) == 0) angle += 90;
                            model = "models/block/" + type + "_inner_stairs.json";
                            break;
                        } else {
                            if ((neighbor2Meta & 1) == 1) angle += 90;
                            model = "models/block/" + type + "_outer_stairs.json";
                            break;
                        }
                    }
                }
                model = "models/block/" + type + "_stairs.json";
                break;

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
                boolean north = (specialNumber & 0x01) == 0x01;
                boolean west = (specialNumber & 0x02) == 0x02;
                boolean south = (specialNumber & 0x04) == 0x04;
                boolean east = (specialNumber & 0x08) == 0x08;
                int[] sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                int sum = sides[0] + sides[1] + sides[2] + sides[3];
                if (sum == 0) {
                    model = "models/block/" + type + "_fence_post.json";
                    break;
                } else if (sum == 1) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            model = "models/block/" + type + "_fence_n.json";
                            break a;
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            model = "models/block/" + type + "_fence_ne.json";
                            break a;
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            model = "models/block/" + type + "_fence_ns.json";
                            break a;
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            model = "models/block/" + type + "_fence_nse.json";
                            break a;
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    model = "models/block/" + type + "_fence_nsew.json";
                    break;
                }

                //stems //TODO add coloring
            case 104:
                //checking 4 sides for pumpkin
                if (specialNumber != -1) {
                    angle = specialNumber * 90;
                    model = "models/block/pumpkin_stem_fruit" + ".json";
                } else {
                    model = "models/block/pumpkin_stem_growth" + metadata + ".json";
                }
                break;
            case 105:
                //checking 4 sides for melon
                if (specialNumber != -1) {
                    model = "models/block/melon_stem_fruit" + ".json";
                } else {
                    model = "models/block/melon_stem_growth" + metadata + ".json";
                }
                break;

            //vines
            case 106:
                boolean up = (specialNumber & 0x01) == 0x01;
                String u = up ? "u" : "";
                north = (metadata & 4) == 4;
                west = (metadata & 2) == 2;
                south = (metadata & 1) == 1;
                east = (metadata & 8) == 8;
                sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                sum = sides[0] + sides[1] + sides[2] + sides[3];
                if (sum == 0) {
                    model = "models/block/vine_u.json";
                    break;
                } else if (sum == 1) {
                    angle = 180;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            model = "models/block/vine_1" + u + ".json";
                            break a;
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            model = "models/block/vine_2" + u + ".json";
                            break a;
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            angle += 90;
                            model = "models/block/vine_2" + u + "_opposite.json";
                            break a;
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            model = "models/block/vine_3" + u + ".json";
                            break a;
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    model = "models/block/vine_4" + u + ".json";
                    break;
                }
                //doors FIXME hitboxes are broken
            case 64:
            case 71:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
                if (id == 64) type = "wooden";
                else if (id == 71) type = "iron";
                else if (id == 193) type = "spruce";
                else if (id == 194) type = "birch";
                else if (id == 195) type = "jungle";
                else if (id == 196) type = "acacia";
                else type = "dark_oak";
                String part;
                int hinge;
                int facing;
                boolean open;
                if ((metadata & 0x08) == 0x08) {
                    part = "top";
                    int bottom = specialNumber & 0x07;
                    hinge = metadata & 1;
                    facing = bottom & 3;
                    open = (bottom & 4) == 4;
                } else {
                    part = "bottom";
                    int top = specialNumber & 0x07;
                    hinge = top & 1;
                    facing = metadata & 3;
                    open = (metadata & 4) == 4;
                }
                if (facing == 0) angle = 360;
                if (facing == 1) angle = 270;
                if (facing == 2) angle = 180;
                if (facing == 3) angle = 90;
                if (open) {
                    angle -= 90;
                    angle += 180 * hinge;
                    hinge = (hinge + 1) % 2;
                }
                if (hinge == 1) {
                    model = "models/block/" + type + "_door_" + part + "_rh.json";
                    break;
                } else {
                    model = "models/block/" + type + "_door_" + part + ".json";
                    break;
                }

                //tripwire hook
            case 131:
                type = "tripwire_hook";
                if ((metadata & 0x04) == 0x04) {
                    type = type + "_attached";
                    if ((specialNumber & 0x01) == 0x01) type = type + "_suspended";
                }
                if ((metadata & 0x08) == 0x08) type = type + "_powered";
                if ((metadata & 0x03) == 0x01) {
                    angle = 90;
                } else if ((metadata & 0x03) == 0x00) {
                    angle = 180;
                } else if ((metadata & 0x03) == 0x03) {
                    angle = 270;
                }
                model = "models/block/" + type + ".json";

                //tripwire FIXME fix this sometime because i dont caare enough to fix it rn
            case 132:
                type = "tripwire";
                if ((metadata & 0x04) == 0x04) type = type + "_attached";
                if ((specialNumber & 0x01) == 0x01) type = type + "_suspended";
                north = (specialNumber & 0x02) == 0x02;
                west = (specialNumber & 0x04) == 0x04;
                south = (specialNumber & 0x08) == 0x08;
                east = (specialNumber & 0x10) == 0x10;
                sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                sum = sides[0] + sides[1] + sides[2] + sides[3];
                if (sum == 0) {
                    model = "models/block/" + type + "_ns.json";
                } else if (sum == 1) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            model = "models/block/" + type + "_n.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            model = "models/block/" + type + "_ne.json";
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            angle += 90;
                            model = "models/block/" + type + "_ns.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            model = "models/block/" + type + "_nse.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    model = "models/block/" + type + "_nsew.json";
                }

                //walls
            case 139:
                type = "cobblestone";
                if (metadata == 1) type = "mossy_cobblestone";
                //similar to fences
                north = (specialNumber & 0x01) == 0x01;
                west = (specialNumber & 0x02) == 0x02;
                south = (specialNumber & 0x04) == 0x04;
                east = (specialNumber & 0x08) == 0x08;
                sides = new int[4];
                if (north) sides[0] = 1;
                if (west) sides[1] = 1;
                if (south) sides[2] = 1;
                if (east) sides[3] = 1;
                sum = sides[0] + sides[1] + sides[2] + sides[3];
                if (sum == 0) {
                    model = "models/block/" + type + "_wall_post.json";
                } else if (sum == 1) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1) {
                            model = "models/block/" + type + "_wall_n.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 2) {
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 1 && sides[(i + 3) % 4] == 1) {
                            model = "models/block/" + type + "_wall_ne.json";
                        }
                        if (sides[i] == 1 && sides[(i + 2) % 4] == 1) {
                            if ((specialNumber & 0x10) != 0) {
                                model = "models/block/" + type + "_wall_ns_above.json";
                                break;
                            }
                            model = "models/block/" + type + "_wall_ns.json";
                        }
                        angle += 90;
                    }
                } else if (sum == 3) {
                    angle = 270;
                    for (int i = 0; i < 4; i++) {
                        if (sides[i] == 0) {
                            model = "models/block/" + type + "_wall_nse.json";
                            break;
                        }
                        angle += 90;
                    }
                } else if (sum == 4) {
                    model = "models/block/" + type + "_wall_nsew.json";
                    break;
                }
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }
        return new BlockModel(context, model, angle, upsideDown);
    }

}
