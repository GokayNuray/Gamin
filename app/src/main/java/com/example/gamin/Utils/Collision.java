package com.example.gamin.Utils;

import android.content.Context;

import com.example.gamin.Minecraft.ChunkColumn;
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
                }
                else {
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
                    short block = ChunkColumn.getBlock((int) Math.floor(x1) + ix, (int) Math.floor(y1) + iy, (int) Math.floor(z1) + iz);
                    JSONArray collisionData = getCollisionData(ChunkColumn.getBlockId(block), ChunkColumn.getBlockMetaData(block));
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
}
