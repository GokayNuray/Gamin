package com.example.gamin.Utils;

import android.content.Context;

import com.example.gamin.Minecraft.Chunk;
import com.example.gamin.Minecraft.Slot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * @noinspection ResultOfMethodCallIgnored
 */
public final class Collision {
    private static JSONObject blockCollisionShapesJSON;

    /**
     * @noinspection ComparatorResultComparison
     */

    public static double[] calculateMovement(double x, double y, double z, int direction, double amount) throws JSONException {
        BigDecimal oneEightieth = BigDecimal.valueOf(0.0125);
        BigDecimal pointThree = BigDecimal.valueOf(0.3);
        BigDecimal bigAmount = BigDecimal.valueOf(amount);
        double epsilon = 0.0000000129;
        switch (direction) {
            case 0:
                BigDecimal bigX = BigDecimal.valueOf(x);
                if (amount >= 0) {
                    for (BigDecimal xx = bigX.subtract(bigX.remainder(oneEightieth)); xx.compareTo(bigX.add(bigAmount)) != 1; xx = xx.add(oneEightieth)) {
                        if (isCubeFull(xx.add(pointThree).doubleValue(), y, z - 0.3, xx.add(pointThree).doubleValue(), y + 1.8, z + 0.3, direction)) {
                            return new double[]{xx.doubleValue() - epsilon, y, z};
                        }
                    }
                } else {
                    for (BigDecimal xx = bigX.subtract(bigX.remainder(oneEightieth)); xx.compareTo(bigX.add(bigAmount)) != -1; xx = xx.subtract(oneEightieth)) {
                        if (isCubeFull(xx.subtract(pointThree).doubleValue(), y, z - 0.3, xx.subtract(pointThree).doubleValue(), y + 1.8, z + 0.3, direction)) {
                            return new double[]{xx.doubleValue() + epsilon, y, z};
                        }
                    }
                }
                return new double[]{bigX.add(bigAmount).doubleValue(), y, z};

            case 1:
                BigDecimal bigY = BigDecimal.valueOf(y);
                if (amount > 0) {
                    for (BigDecimal yy = bigY.subtract(bigY.remainder(oneEightieth)); yy.compareTo(bigY.add(bigAmount)) != 1; yy = yy.add(oneEightieth)) {
                        if (isCubeFull(x - 0.3, yy.add(BigDecimal.valueOf(1.8)).doubleValue(), z - 0.3, x + 0.3, yy.add(BigDecimal.valueOf(1.8)).doubleValue(), z + 0.3, direction)) {
                            return new double[]{x, yy.doubleValue(), z, 1.0};
                        }
                    }
                } else {
                    for (BigDecimal yy = bigY.subtract(bigY.remainder(oneEightieth)); yy.compareTo(bigY.add(bigAmount)) != -1; yy = yy.subtract(oneEightieth)) {
                        if (isCubeFull(x - 0.3, yy.doubleValue(), z - 0.3, x + 0.3, yy.doubleValue(), z + 0.3, direction)) {
                            return new double[]{x, yy.doubleValue(), z, 1.0};
                        }
                    }
                }
                return new double[]{x, bigY.add(bigAmount).doubleValue(), z, 0.0};

            case 2:
                BigDecimal bigZ = BigDecimal.valueOf(z);
                if (amount >= 0) {
                    for (BigDecimal zz = bigZ.subtract(bigZ.remainder(oneEightieth)); zz.compareTo(bigZ.add(bigAmount)) != 1; zz = zz.add(oneEightieth)) {
                        if (isCubeFull(x - 0.3, y, zz.doubleValue(), x + 0.3, y + 1.8, zz.add(pointThree).doubleValue(), direction)) {
                            return new double[]{x, y, zz.doubleValue() - epsilon};
                        }
                    }
                } else {
                    for (BigDecimal zz = bigZ.subtract(bigZ.remainder(oneEightieth)); zz.compareTo(bigZ.add(bigAmount)) != -1; zz = zz.subtract(oneEightieth)) {
                        if (isCubeFull(x - 0.3, y, zz.subtract(pointThree).doubleValue(), x + 0.3, y + 1.8, zz.doubleValue(), direction)) {
                            return new double[]{x, y, zz.doubleValue() + epsilon};
                        }
                    }
                }
                return new double[]{x, y, bigZ.add(bigAmount).doubleValue()};
            default:
                return new double[0];
        }
    }


    private static boolean isCubeFull(double x1, double y1, double z1, double x2, double y2, double z2, int direction) throws JSONException {

        for (int ix = -1; ix < (int) (Math.floor(x2) - Math.floor(x1) + 1); ix++) {
            for (int iy = -1; iy < (int) (Math.floor(y2) - Math.floor(y1) + 1); iy++) {
                for (int iz = -1; iz < (int) (Math.floor(z2) - Math.floor(z1) + 1); iz++) {
                    short block = Chunk.getBlock((int) Math.floor(x1) + ix, (int) Math.floor(y1) + iy, (int) Math.floor(z1) + iz);
                    JSONArray collisionData = getCollisionData(Chunk.getBlockId(block), Chunk.getBlockMetaData(block));
                    for (int i = 0; i < collisionData.length(); i++) {
                        double bx1 = collisionData.getJSONArray(i).getDouble(0) + (int) Math.floor(x1) + ix;
                        double by1 = collisionData.getJSONArray(i).getDouble(1) + (int) Math.floor(y1) + iy;
                        double bz1 = collisionData.getJSONArray(i).getDouble(2) + (int) Math.floor(z1) + iz;
                        double bx2 = collisionData.getJSONArray(i).getDouble(3) + (int) Math.floor(x1) + ix;
                        double by2 = collisionData.getJSONArray(i).getDouble(4) + (int) Math.floor(y1) + iy;
                        double bz2 = collisionData.getJSONArray(i).getDouble(5) + (int) Math.floor(z1) + iz;

                        if (direction == 0) {
                            if (doesCubesCollide(x1, y1, z1, x2, y2, z2, bx1, by1, bz1, bx1, by2, bz2, direction)) {
                                PacketUtils.didHorizontalCollide = true;
                                return true;
                            }
                            if (doesCubesCollide(x1, y1, z1, x2, y2, z2, bx2, by1, bz1, bx2, by2, bz2, direction)) {
                                PacketUtils.didHorizontalCollide = true;
                                return true;
                            }
                        }
                        if (direction == 1) {
                            if (doesCubesCollide(x1, y1, z1, x2, y2, z2, bx1, by1, bz1, bx2, by1, bz2, direction))
                                return true;
                            if (doesCubesCollide(x1, y1, z1, x2, y2, z2, bx1, by2, bz1, bx2, by2, bz2, direction))
                                return true;
                        }
                        if (direction == 2) {
                            if (doesCubesCollide(x1, y1, z1, x2, y2, z2, bx1, by1, bz1, bx2, by2, bz1, direction)) {
                                PacketUtils.didHorizontalCollide = true;
                                return true;
                            }
                            if (doesCubesCollide(x1, y1, z1, x2, y2, z2, bx1, by1, bz2, bx2, by2, bz2, direction)) {
                                PacketUtils.didHorizontalCollide = true;
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    //TODO: 20.11.2023 check by direction to phase through blocks we are already in
    private static boolean doesCubesCollide(double ax1, double ay1, double az1, double ax2, double ay2, double az2,
                                            double bx1, double by1, double bz1, double bx2, double by2, double bz2, int direction) {
        if (direction == 1)
            return Math.min(ax2, bx2) >= Math.max(ax1, bx1) && Math.min(ay2, by2) >= Math.max(ay1, by1) && Math.min(az2, bz2) >= Math.max(az1, bz1);
        if (direction == 0)
            return Math.min(ax2, bx2) >= Math.max(ax1, bx1) && Math.min(ay2, by2) > Math.max(ay1, by1) && Math.min(az2, bz2) > Math.max(az1, bz1);
        if (direction == 2)
            return Math.min(ax2, bx2) > Math.max(ax1, bx1) && Math.min(ay2, by2) > Math.max(ay1, by1) && Math.min(az2, bz2) >= Math.max(az1, bz1);
        else return Boolean.TRUE;
    }

    private static JSONArray getCollisionData(byte id, byte metadata) throws JSONException {
        String name = Objects.requireNonNull(Slot.blocksMap.get(Byte.toUnsignedInt(id))).getString("name");
        Object block = blockCollisionShapesJSON.getJSONObject("blocks").get(name);
        int collisionId;
        if (block instanceof JSONArray) {
            collisionId = ((JSONArray) block).getInt(metadata);
        } else {
            collisionId = (Integer) block;
        }
        return blockCollisionShapesJSON.getJSONObject("shapes").getJSONArray(String.valueOf(collisionId));
    }


    public static void loadCollisionData(Context context) throws JSONException, IOException {
        InputStream is = context.getAssets().open("data/blockCollisionShapes.json");
        byte[] b = new byte[is.available()];
        is.read(b);
        blockCollisionShapesJSON = new JSONObject(new String(b));
        is.close();
    }

    public static float[][] getHitbox(short block) {
        byte id = Chunk.getBlockId(block);
        byte metadata = Chunk.getBlockMetaData(block);
        float[][] hitboxes = new float[1][6];
        float[] hitbox = hitboxes[0];

        switch (id) {//blocks that have different hitbox than collision box
            //sapling and grass
            case 6:
            case 31:
                hitbox[0] = 0.1f;
                hitbox[1] = 0.0f;
                hitbox[2] = 0.1f;
                hitbox[3] = 0.9f;
                hitbox[4] = 0.8f;
                hitbox[5] = 0.9f;
                return hitboxes;

            //torch and redstone torch
            case 50:
            case 76:
                if (metadata == 5) {//face up
                    hitbox[0] = 0.4f;
                    hitbox[1] = 0.0f;
                    hitbox[2] = 0.4f;
                    hitbox[3] = 0.6f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 0.6f;
                } else if (metadata == 1) {//facing east
                    hitbox[0] = 0.0f;
                    hitbox[1] = 0.2f;
                    hitbox[2] = 0.35f;
                    hitbox[3] = 0.3f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 0.65f;
                } else if (metadata == 2) {//facing west
                    hitbox[0] = 0.7f;
                    hitbox[1] = 0.2f;
                    hitbox[2] = 0.35f;
                    hitbox[3] = 1.0f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 0.65f;
                } else if (metadata == 3) {//facing south
                    hitbox[0] = 0.35f;
                    hitbox[1] = 0.2f;
                    hitbox[2] = 0.0f;
                    hitbox[3] = 0.65f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 0.3f;
                } else if (metadata == 4) {//facing north
                    hitbox[0] = 0.35f;
                    hitbox[1] = 0.2f;
                    hitbox[2] = 0.7f;
                    hitbox[3] = 0.65f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 1.0f;
                }
                return hitboxes;

            //flowers
            case 37:
            case 38:
                hitbox[0] = 0.3f;
                hitbox[1] = 0.0f;
                hitbox[2] = 0.3f;
                hitbox[3] = 0.7f;
                hitbox[4] = 0.6f;
                hitbox[5] = 0.7f;
                return hitboxes;

            //lever
            case 69:
                if (metadata == 0) {//facing down
                    hitbox[0] = 0.25f;
                    hitbox[1] = 0.4f;
                    hitbox[2] = 0.25f;
                    hitbox[3] = 0.75f;
                    hitbox[4] = 1.0f;
                    hitbox[5] = 0.75f;
                } else if (metadata == 1) {//facing east
                    hitbox[0] = 0.0f;
                    hitbox[1] = 0.2f;
                    hitbox[2] = 0.31f;
                    hitbox[3] = 0.25f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 0.69f;
                } else if (metadata == 2) {//facing west
                    hitbox[0] = 0.75f;
                    hitbox[1] = 0.2f;
                    hitbox[2] = 0.31f;
                    hitbox[3] = 1.0f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 0.69f;
                } else if (metadata == 3) {//facing south
                    hitbox[0] = 0.31f;
                    hitbox[1] = 0.2f;
                    hitbox[2] = 0.0f;
                    hitbox[3] = 0.69f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 0.25f;
                } else if (metadata == 4) {//facing north
                    hitbox[0] = 0.31f;
                    hitbox[1] = 0.2f;
                    hitbox[2] = 0.75f;
                    hitbox[3] = 0.69f;
                    hitbox[4] = 0.8f;
                    hitbox[5] = 1.0f;
                }
                return hitboxes;

            //button
            case 77:
            case -123:
                if (metadata == 0) {//facing down
                    hitbox[0] = 0.375f;
                    hitbox[1] = 0.875f;
                    hitbox[2] = 0.325f;
                    hitbox[3] = 0.625f;
                    hitbox[4] = 1.0f;
                    hitbox[5] = 0.675f;
                } else if (metadata == 1) {//facing east
                    hitbox[0] = 0.0f;
                    hitbox[1] = 0.375f;
                    hitbox[2] = 0.325f;
                    hitbox[3] = 0.25f;
                    hitbox[4] = 0.625f;
                    hitbox[5] = 0.675f;
                } else if (metadata == 2) {//facing west
                    hitbox[0] = 0.625f;
                    hitbox[1] = 0.375f;
                    hitbox[2] = 0.325f;
                    hitbox[3] = 1.0f;
                    hitbox[4] = 0.625f;
                    hitbox[5] = 0.675f;
                } else if (metadata == 3) {//facing south
                    hitbox[0] = 0.325f;
                    hitbox[1] = 0.375f;
                    hitbox[2] = 0.0f;
                    hitbox[3] = 0.675f;
                    hitbox[4] = 0.625f;
                    hitbox[5] = 0.25f;
                } else if (metadata == 4) {//facing north
                    hitbox[0] = 0.325f;
                    hitbox[1] = 0.375f;
                    hitbox[2] = 0.625f;
                    hitbox[3] = 0.675f;
                    hitbox[4] = 0.625f;
                    hitbox[5] = 1.0f;
                }
                return hitboxes;

            //crops(potatoes, carrots, wheat)
            case 59:
            case -125:
            case -124:
                hitbox[0] = 0.0f;
                hitbox[1] = 0.0f;
                hitbox[2] = 0.0f;
                hitbox[3] = 1.0f;
                hitbox[4] = 0.25f;
                hitbox[5] = 1.0f;
                return hitboxes;
        }

        //blocks that have same hitbox as collision box
        try {
            JSONArray collisionData = getCollisionData(id, metadata);
            hitboxes = new float[collisionData.length()][6];

            for (int i = 0; i < collisionData.length(); i++) {
                hitbox = hitboxes[i];
                hitbox[0] = (float) collisionData.getJSONArray(i).getDouble(0);
                hitbox[1] = (float) collisionData.getJSONArray(i).getDouble(1);
                hitbox[2] = (float) collisionData.getJSONArray(i).getDouble(2);
                hitbox[3] = (float) collisionData.getJSONArray(i).getDouble(3);
                hitbox[4] = (float) collisionData.getJSONArray(i).getDouble(4);
                hitbox[5] = (float) collisionData.getJSONArray(i).getDouble(5);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hitboxes;

    }
}
