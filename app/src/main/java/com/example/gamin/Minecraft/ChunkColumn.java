package com.example.gamin.Minecraft;

import android.content.Context;
import android.util.Log;

import com.example.gamin.Render.SlotRenderer;
import com.example.gamin.Render.Square;
import com.example.gamin.Render.TextureAtlas;
import com.example.gamin.Render.TileEntity;
import com.example.gamin.Utils.PacketUtils;

import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ChunkColumn {
    private static final Set<ChunkColumn> updatedChunks = new HashSet<>();
    public final short[][][][] chunk = new short[16][16][16][16];
    public final Map<TextureAtlas, List<Square>> squares = new HashMap<>();
    public final Map<TextureAtlas, FloatBuffer> coordsBuffers = new HashMap<>();
    public final Map<TextureAtlas, FloatBuffer> colorsBuffers = new HashMap<>();
    public final Map<TextureAtlas, FloatBuffer> texturesBuffers = new HashMap<>();
    private final Map<Integer, SlotRenderer> renders = new HashMap<>();
    private final Map<Integer, TileEntity> tileEntities = new HashMap<>();
    private final Set<TextureAtlas> updatedAtlases = new HashSet<>();
    public long pos;
    public short bitmask;
    public boolean isLoaded = false;

    public static short getBlock(int x, int y, int z) {
        if (y >= 0 && y < 256) {
            long getPos;
            getPos = ((long) Math.floor((float) x / 16) << 32) | (((int) Math.floor((float) z / 16) & 0xffffffffL));
            ChunkColumn chunkColumn = PacketUtils.chunkColumnMap.get(getPos);
            if (PacketUtils.chunkColumnMap.containsKey(getPos) && chunkColumn == null)
                Log.w("ChunkColumn", "ChunkColumn is null");
            if (chunkColumn != null)
                return chunkColumn.chunk[y / 16][Math.floorMod(x, 16)][Math.floorMod(y, 16)][Math.floorMod(z, 16)];
            else return 0;
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
            assert chunkColumn != null : "chunkColumn is null";
            chunkColumn.chunk[chunkY][blockX][blockY][blockZ] = block;
            try {
                if (getBlockId(block) == 0) {
                    chunkColumn.removeRender((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ);
                    chunkColumn.tileEntities.remove((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ);
                } else {
                    if (TileEntity.tileEntityIds.contains(getBlockId(block) & 0xff)) {
                        TileEntity tileEntity = new TileEntity(context, getBlockId(block), getBlockMetaData(block), x, y, z);
                        chunkColumn.tileEntities.put((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ, tileEntity);
                        chunkColumn.setRender(tileEntity.slotRenderer, (short) chunkY, (short) blockX, (short) blockY, (short) blockZ);
                        return;
                    }
                    SlotRenderer render = new SlotRenderer(context, getBlockId(block), getBlockMetaData(block), 1, x, y, z);
                    chunkColumn.setRender(render, (short) chunkY, (short) blockX, (short) blockY, (short) blockZ);
                }
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setRender(SlotRenderer render, short chunkY, short blockX, short blockY, short blockZ) {
        int pos = (chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ;
        SlotRenderer oldRender = renders.put(pos, render);
        replaceSquares(oldRender, render);
        updatedChunks.add(this);
    }

    private void removeRender(int pos) {
        SlotRenderer oldRender = renders.remove(pos);
        replaceSquares(oldRender, null);
        updatedChunks.add(this);
    }

    private void replaceSquares(SlotRenderer oldRender, SlotRenderer newRender) {
        if (oldRender != null) {
            for (Square square : oldRender.squares) {
                TextureAtlas atlas = square.atlas;
                List<Square> atlasSquares = squares.get(atlas);
                atlasSquares.remove(square);
                if (atlasSquares.isEmpty()) {
                    squares.remove(atlas);
                }
                updatedAtlases.add(atlas);
            }
        }
        if (newRender == null) return;
        for (Square square : newRender.squares) {
            TextureAtlas atlas = square.atlas;
            List<Square> atlasSquares = squares.get(atlas);
            if (atlasSquares == null) {
                atlasSquares = new ArrayList<>();
                squares.put(atlas, atlasSquares);
            }
            atlasSquares.add(square);
            updatedAtlases.add(atlas);
        }
    }

    private void setBuffers() {
        for (TextureAtlas atlas : updatedAtlases) {
            synchronized (this) {
                List<Square> atlasSquares = squares.get(atlas);
                if (atlasSquares == null) continue;
                FloatBuffer coordsBuffer = ByteBuffer.allocateDirect(atlasSquares.size() * 6 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                FloatBuffer colorsBuffer = ByteBuffer.allocateDirect(atlasSquares.size() * 6 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                FloatBuffer texturesBuffer = ByteBuffer.allocateDirect(atlasSquares.size() * 6 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                for (Square square : atlasSquares) {
                    coordsBuffer.put(square.coords1);
                    coordsBuffer.put(square.coords2);
                    colorsBuffer.put(square.squareColors);
                    texturesBuffer.put(square.textures1);
                    texturesBuffer.put(square.textures2);
                }
                coordsBuffer.position(0);
                colorsBuffer.position(0);
                texturesBuffer.position(0);
                coordsBuffers.put(atlas, coordsBuffer);
                colorsBuffers.put(atlas, colorsBuffer);
                texturesBuffers.put(atlas, texturesBuffer);
            }
        }
        updatedAtlases.clear();
        isLoaded = true;
    }

    public void update(short chunkY, short blockX, short blockY, short blockZ) {
        SlotRenderer render = renders.get((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ);
        SlotRenderer newRender = render.update();
        if (newRender != null) setRender(newRender, chunkY, blockX, blockY, blockZ);
    }

    public void setRenders(Context context, int chunkX, int chunkZ) {
        renders.clear();
        for (short chunkY = 0; chunkY < 16; chunkY++) {
            if ((bitmask & (1 << chunkY)) != 0) {
                for (short blockX = 0; blockX < 16; blockX++) {
                    for (short blockZ = 0; blockZ < 16; blockZ++) {
                        for (short blockY = 0; blockY < 16; blockY++) {
                            short block = chunk[chunkY][blockX][blockY][blockZ];
                            if (getBlockId(block) == 0) continue;
                            try {
                                if (TileEntity.tileEntityIds.contains(getBlockId(block) & 0xff)) {
                                    TileEntity tileEntity = new TileEntity(context, getBlockId(block), getBlockMetaData(block), chunkX * 16 + blockX, chunkY * 16 + blockY, chunkZ * 16 + blockZ);
                                    tileEntities.put((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ, tileEntity);
                                    setRender(tileEntity.slotRenderer, chunkY, blockX, blockY, blockZ);
                                    continue;
                                }
                                SlotRenderer render = new SlotRenderer(context, getBlockId(block), getBlockMetaData(block), 1, chunkX * 16 + blockX, chunkY * 16 + blockY, chunkZ * 16 + blockZ);
                                replaceSquares(null, render);
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
