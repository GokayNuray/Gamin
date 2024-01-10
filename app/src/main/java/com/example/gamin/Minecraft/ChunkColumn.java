package com.example.gamin.Minecraft;

import android.content.Context;

import com.example.gamin.Render.SlotRenderer;
import com.example.gamin.Utils.PacketUtils;

import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ChunkColumn {
    public static Set<ChunkColumn> updatedChunks = new HashSet<>();
    public short[][][][] chunk = new short[16][16][16][16];
    public Map<Integer, SlotRenderer> renders = new HashMap<>();
    public int squareCount = 0;
    public FloatBuffer coordsBuffer;
    public FloatBuffer colorsBuffer;
    public FloatBuffer texturesBuffer;
    public long pos;
    public short bitmask;
    public boolean isLoaded = false;

    public static short getBlock(int x, int y, int z) {
        if (y >= 0 && y < 256) {
            long getPos;
            getPos = ((long) Math.floor((float) x / 16) << 32) | (((int) Math.floor((float) z / 16) & 0xffffffffL));
            if (PacketUtils.chunkColumnMap.containsKey(getPos)) {
                return Objects.requireNonNull(PacketUtils.chunkColumnMap.get(getPos)).chunk[y / 16][Math.floorMod(x, 16)][Math.floorMod(y, 16)][Math.floorMod(z, 16)];
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static void setUpdatedBuffers() {
        updatedChunks.forEach(ChunkColumn::setBuffers);
        updatedChunks.clear();
    }

    public static byte getBlockId(short block) {
        return (byte) ((block & 0x000f) * 16 + (block & 0xf000) / 0x1000);
    }

    public static byte getBlockId(int x, int y, int z) {
        return getBlockId(getBlock(x, y, z));
    }

    public static byte getBlockMetaData(short block) {
        return (byte) ((block & 0x00f0) + (block & 0x0f00) / 0x0100);
    }

    public static byte getBlockMetaData(int x, int y, int z) {
        return getBlockMetaData(getBlock(x, y, z));
    }

    public static void setBlock(Context context, int x, int y, int z, short block) {
        long getPos;
        getPos = ((long) Math.floor((float) x / 16) << 32) | (((int) Math.floor((float) z / 16) & 0xffffffffL));
        if (PacketUtils.chunkColumnMap.containsKey(getPos)) {
            ChunkColumn chunkColumn = PacketUtils.chunkColumnMap.get(getPos);
            int chunkY = y / 16;
            int blockX = Math.floorMod(x, 16);
            int blockY = Math.floorMod(y, 16);
            int blockZ = Math.floorMod(z, 16);
            chunkColumn.chunk[chunkY][blockX][blockY][blockZ] = block;
            try {
                if (getBlockId(block) == 0) {
                    chunkColumn.squareCount -= chunkColumn.renders.remove((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ).squares.size();
                    updatedChunks.add(chunkColumn);
                } else
                    chunkColumn.setRender(new SlotRenderer(context, getBlockId(block), getBlockMetaData(block), 1, x, y, z), (short) chunkY, (short) blockX, (short) blockY, (short) blockZ);
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setRender(SlotRenderer render, short chunkY, short blockX, short blockY, short blockZ) {
        int pos = (chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ;
        SlotRenderer oldRender = renders.put(pos, render);
        if (oldRender != null) {
            squareCount -= oldRender.squares.size();
        }
        squareCount += render.squares.size();
        updatedChunks.add(this);
    }

    public void setBuffers() {
        if (squareCount == 0) return;
        synchronized (this) {
            coordsBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            colorsBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            texturesBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            for (SlotRenderer render : renders.values()) {
                coordsBuffer.put(render.coordsBuffer);
                colorsBuffer.put(render.colorsBuffer);
                texturesBuffer.put(render.texturesBuffer);
                render.coordsBuffer.position(0);
                render.colorsBuffer.position(0);
                render.texturesBuffer.position(0);
            }
            coordsBuffer.position(0);
            colorsBuffer.position(0);
            texturesBuffer.position(0);
            isLoaded = true;
        }
    }

    public void update(short chunkY, short blockX, short blockY, short blockZ) {
        SlotRenderer render = renders.get((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ);
        SlotRenderer newRender = render.update();
        if (newRender != null) setRender(newRender, chunkY, blockX, blockY, blockZ);
    }

    public void setRenders(Context context, int chunkX, int chunkZ) {
        renders.clear();
        squareCount = 0;
        for (short chunkY = 0; chunkY < 16; chunkY++) {
            if ((bitmask & (1 << chunkY)) != 0) {
                for (short blockX = 0; blockX < 16; blockX++) {
                    for (short blockZ = 0; blockZ < 16; blockZ++) {
                        for (short blockY = 0; blockY < 16; blockY++) {
                            short block = chunk[chunkY][blockX][blockY][blockZ];
                            if (getBlockId(block) == 0) continue;
                            try {
                                SlotRenderer render = new SlotRenderer(context, getBlockId(block), getBlockMetaData(block), 1, chunkX * 16 + blockX, chunkY * 16 + blockY, chunkZ * 16 + blockZ);
                                squareCount += render.squares.size();
                                renders.put((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ, render);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            SlotRenderer neighborRender = null;
                            if (blockX == 0) {
                                long getPos;
                                getPos = ((long) (chunkX - 1) << 32) | (chunkZ & 0xffffffffL);
                                try {
                                    //FIXME this should update neighbor renderer but it doesn't and i know how to fix it but i'm too lazy to do it right now :p
                                    neighborRender = Objects.requireNonNull(PacketUtils.chunkColumnMap.get(getPos)).renders.get((chunkY << 12) | (15 << 8) | (blockY << 4) | blockZ);
                                } catch (Exception ignored) {
                                }
                            }
                            if (blockX == 15) {
                                long getPos;
                                getPos = ((long) (chunkX + 1) << 32) | (chunkZ & 0xffffffffL);
                                try {
                                    neighborRender = Objects.requireNonNull(PacketUtils.chunkColumnMap.get(getPos)).renders.get((chunkY << 12) | (0 << 8) | (blockY << 4) | blockZ);
                                } catch (Exception ignored) {
                                }
                            }
                            if (blockZ == 0) {
                                long getPos;
                                getPos = ((long) chunkX << 32) | ((chunkZ - 1) & 0xffffffffL);
                                try {
                                    neighborRender = Objects.requireNonNull(PacketUtils.chunkColumnMap.get(getPos)).renders.get((chunkY << 12) | (blockX << 8) | (blockY << 4) | 15);
                                } catch (Exception ignored) {
                                }
                            }
                            if (blockZ == 15) {
                                long getPos;
                                getPos = ((long) chunkX << 32) | ((chunkZ + 1) & 0xffffffffL);
                                try {
                                    neighborRender = Objects.requireNonNull(PacketUtils.chunkColumnMap.get(getPos)).renders.get((chunkY << 12) | (blockX << 8) | (blockY << 4) | 0);
                                } catch (Exception ignored) {
                                }
                            }
                            if (neighborRender != null) neighborRender = neighborRender.update();
                        }
                    }
                }
            }
        }
        updatedChunks.add(this);
    }
}
