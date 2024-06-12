package com.example.gamin.Minecraft;

import android.content.Context;
import android.util.Log;

import com.example.gamin.Render.BlockModel;
import com.example.gamin.Render.Entity;
import com.example.gamin.Render.TextureAtlas;
import com.example.gamin.Render.TileEntity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Chunk {
    public static final Map<Long, Chunk[]> chunkColumnMap = new HashMap<>();
    public final short[] blocks;
    public final Map<TextureAtlas, Integer> squares = new HashMap<>();
    public final Map<TextureAtlas, FloatBuffer> coordsBuffers = new HashMap<>();
    public final Map<TextureAtlas, FloatBuffer> colorsBuffers = new HashMap<>();
    public final Map<TextureAtlas, FloatBuffer> texturesBuffers = new HashMap<>();
    public final Map<Integer, Entity> entities = new HashMap<>();
    private final BlockModel[] models = new BlockModel[16 * 16 * 16];
    private final Map<Integer, TileEntity> tileEntities = new HashMap<>();
    private final Set<TextureAtlas> updatedAtlases = new HashSet<>();
    private final int chunkX;
    private final byte chunkY;
    private final int chunkZ;
    public boolean isLoaded = false;
    public boolean isChanged = true;

    public Chunk(short[] blocks, int chunkX, byte chunkY, int chunkZ) {
        this.blocks = blocks;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
    }

    public static short getBlock(int x, int y, int z) {
        if (y >= 0 && y < 256) {
            long getPos = ((long) Math.floor((float) x / 16) << 32) | (((int) Math.floor((float) z / 16) & 0xffffffffL));
            Chunk[] chunkColumn = chunkColumnMap.get(getPos);
            if (chunkColumnMap.containsKey(getPos) && chunkColumn == null)
                Log.w("ChunkColumn", "ChunkColumn is null");
            if (chunkColumn != null && chunkColumn[y / 16] != null)
                //I use Math.floorMod() to get the correct block coordinates in case the coordinates are negative
                return chunkColumn[y / 16].blocks[Math.floorMod(y, 16) * 256 + Math.floorMod(z, 16) * 16 + Math.floorMod(x, 16)];
            else return 0;
        } else {
            return 0;
        }
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
        if (chunkColumnMap.containsKey(getPos)) {
            Chunk[] chunkColumn = chunkColumnMap.get(getPos);
            assert chunkColumn != null : "chunkColumn is null";
            Chunk chunk = chunkColumn[y / 16];
            int blockX = Math.floorMod(x, 16);
            int blockY = Math.floorMod(y, 16);
            int blockZ = Math.floorMod(z, 16);
            int pos = blockY * 256 + blockZ * 16 + blockX;
            if (chunk == null) {
                chunk = new Chunk(new short[16 * 16 * 16], x / 16, (byte) (y / 16), z / 16);
                chunkColumn[y / 16] = chunk;
            }
            chunk.blocks[pos] = block;
            if (getBlockId(block) == 0) {
                chunk.removeRender(pos);
                chunk.tileEntities.remove(pos);
            } else {
                if (TileEntity.tileEntityIds.contains(getBlockId(block) & 0xff)) {
                    TileEntity tileEntity = new TileEntity(context, getBlockId(block), getBlockMetaData(block));
                    chunk.tileEntities.put(pos, tileEntity);
                    chunk.setRender(tileEntity.model, pos);
                    return;
                }
                BlockModel model = BlockModel.getBlockModel(context, block, x, y, z);
                chunk.setRender(model, pos);
            }
        } else {
            Log.w("Chunk", "ChunkColumn is null");
        }
    }

    private void setRender(BlockModel model, int pos) {
        BlockModel oldRender = models[pos];
        updatedAtlases.add(model.textureAtlas);
        squares.put(model.textureAtlas, squares.getOrDefault(model.textureAtlas, 0) + model.squares.size() - (oldRender == null ? 0 : oldRender.squares.size()));
        models[pos] = model;
        isChanged = true;
    }

    private void removeRender(int pos) {
        updatedAtlases.add(models[pos].textureAtlas);
        squares.put(models[pos].textureAtlas, squares.getOrDefault(models[pos].textureAtlas, 0) - models[pos].squares.size());
        models[pos] = null;
        isChanged = true;
    }

    public void setBuffers() {
        //System.gc();
        for (TextureAtlas atlas : updatedAtlases) {
            int atlasSquares = squares.get(atlas);
            if (atlasSquares == 0) continue;
            FloatBuffer coordsBuffer = ByteBuffer.allocateDirect(Math.max(atlasSquares * 6 * 3 * 4, coordsBuffers.getOrDefault(atlas, FloatBuffer.allocate(0)).capacity())).order(ByteOrder.nativeOrder()).asFloatBuffer();
            FloatBuffer colorsBuffer = ByteBuffer.allocateDirect(Math.max(atlasSquares * 6 * 4 * 4, coordsBuffers.getOrDefault(atlas, FloatBuffer.allocate(0)).capacity())).order(ByteOrder.nativeOrder()).asFloatBuffer();
            FloatBuffer texturesBuffer = ByteBuffer.allocateDirect(Math.max(atlasSquares * 6 * 2 * 4, coordsBuffers.getOrDefault(atlas, FloatBuffer.allocate(0)).capacity())).order(ByteOrder.nativeOrder()).asFloatBuffer();
            coordsBuffer.position(0);
            colorsBuffer.position(0);
            texturesBuffer.position(0);
            coordsBuffers.put(atlas, coordsBuffer);
            colorsBuffers.put(atlas, colorsBuffer);
            texturesBuffers.put(atlas, texturesBuffer);
        }
        int x = chunkX * 16;
        int y = chunkY * 16;
        int z = chunkZ * 16;
        for (int i = 0; i < models.length; i++) {
            BlockModel model = models[i];
            if (model == null) continue;
            if (!updatedAtlases.contains(model.textureAtlas)) continue;

            FloatBuffer coordsBuffer = coordsBuffers.get(model.textureAtlas);
            FloatBuffer colorsBuffer = colorsBuffers.get(model.textureAtlas);
            FloatBuffer texturesBuffer = texturesBuffers.get(model.textureAtlas);
            if (coordsBuffer == null || colorsBuffer == null || texturesBuffer == null) {
                Log.w("Chunk", "coordsBuffer, colorsBuffer or texturesBuffer is null");
                continue;
            }
            colorsBuffer.put(model.colors);
            texturesBuffer.put(model.textureCoords);
            //add block coordinates to coordsBuffer
            int blockX = x + i % 16;
            int blockY = y + i / 256;
            int blockZ = z + (i / 16) % 16;
            for (int j = 0; j < model.coords.length; j += 3) {
                coordsBuffer.put(model.coords[j] + blockX);
                coordsBuffer.put(model.coords[j + 1] + blockY);
                coordsBuffer.put(model.coords[j + 2] + blockZ);
            }
        }
        for (TextureAtlas atlas : updatedAtlases) {
            FloatBuffer coordsBuffer = coordsBuffers.get(atlas);
            FloatBuffer colorsBuffer = colorsBuffers.get(atlas);
            FloatBuffer texturesBuffer = texturesBuffers.get(atlas);
            if (coordsBuffer == null || colorsBuffer == null || texturesBuffer == null) {
                Log.w("Chunk", "coordsBuffer, colorsBuffer or texturesBuffer is null");
                continue;
            }
            coordsBuffer.position(0);
            colorsBuffer.position(0);
            texturesBuffer.position(0);
        }
        updatedAtlases.clear();
    }

    /*public void update(short chunkY, short blockX, short blockY, short blockZ) {
        BlockModel model = models.get((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ);
        BlockModel newRender = model.update();
        if (newRender != null) setRender(newRender, chunkY, blockX, blockY, blockZ);
    }*/

    public void setRenders(Context context, int chunkX, int chunkZ) {
        tileEntities.clear();
        //System.gc();
        /*
        for (short chunkY = 0; chunkY < 16; chunkY++) {
            if ((bitmask & (1 << chunkY)) != 0) {
                for (short blockX = 0; blockX < 16; blockX++) {
                    for (short blockZ = 0; blockZ < 16; blockZ++) {
                        for (short blockY = 0; blockY < 16; blockY++) {
                            short block = chunks[chunkY][blockY * 256 + blockZ * 16 + blockX];
                            if (getBlockId(block) == 0) continue;
                            try {
                                if (TileEntity.tileEntityIds.contains(getBlockId(block) & 0xff)) {
                                    TileEntity tileEntity = new TileEntity(context, getBlockId(block), getBlockMetaData(block), chunkX * 16 + blockX, chunkY * 16 + blockY, chunkZ * 16 + blockZ);
                                    tileEntities.put((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ, tileEntity);
                                    setRender(tileEntity.slotRenderer, chunkY, blockX, blockY, blockZ);
                                    continue;
                                }
                                BlockModel model = new BlockModel(context, getBlockId(block), getBlockMetaData(block), 1, chunkX * 16 + blockX, chunkY * 16 + blockY, chunkZ * 16 + blockZ);
                                //replaceSquares(null, model);
                                models.put((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ, model);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }*/
        long time = System.currentTimeMillis();
        for (short i = 0; i < 16 * 16 * 16; i++) {
            short block = blocks[i];
            if (getBlockId(block) == 0) continue;
            try {
                if (TileEntity.tileEntityIds.contains(getBlockId(block) & 0xff)) {
                    TileEntity tileEntity = new TileEntity(context, getBlockId(block), getBlockMetaData(block));
                    tileEntities.put((int) i, tileEntity);
                    setRender(tileEntity.model, i);
                    continue;
                }
                BlockModel model = BlockModel.getBlockModel(context, block, chunkX * 16 + i % 16, chunkY * 16 + i / 256, chunkZ * 16 + (i / 16) % 16);
                //replaceSquares(null, model);
                assert model != null : "model is null";
                setRender(model, i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //Log.v("Chunk", "setting models took " + (System.currentTimeMillis() - time) + "ms");
        /*
        for (short chunkY = 0; chunkY < 16; chunkY++) {
            if ((bitmask & (1 << chunkY)) != 0) {
                for (short blockX = 0; blockX < 16; blockX++) {
                    for (short blockZ = 0; blockZ < 16; blockZ++) {
                        for (short blockY = 0; blockY < 16; blockY++) {
                            short block = chunks[chunkY][blockY * 256 + blockZ * 16 + blockX];
                            if (getBlockId(block) == 0) continue;
                            BlockModel model = models.get((chunkY << 12) | (blockX << 8) | (blockY << 4) | blockZ);

                            //find adjacent block shapes and call BlockModel.removeBlockedFaces() to remove faces that are blocked by adjacent blocks
                            //this is done to prevent modeling faces that are not visible

                            byte[] adjacentBlockShapes = new byte[6];
                            //find adjacent models and get their block shapes

                            if (blockX > 0) {
                                BlockModel adjacentRender = models.get((chunkY << 12) | ((blockX - 1) << 8) | (blockY << 4) | blockZ);
                                if (adjacentRender != null) {
                                    adjacentBlockShapes[0] = adjacentRender.shape;
                                } else {
                                    adjacentBlockShapes[0] = -1;
                                }
                            } else {
                                //TODO: get adjacent block shape from adjacent chunk
                                adjacentBlockShapes[0] = -1;
                            }

                            if (blockX < 15) {
                                BlockModel adjacentRender = models.get((chunkY << 12) | ((blockX + 1) << 8) | (blockY << 4) | blockZ);
                                if (adjacentRender != null) {
                                    adjacentBlockShapes[1] = adjacentRender.shape;
                                } else {
                                    adjacentBlockShapes[1] = -1;
                                }
                            } else {
                                adjacentBlockShapes[1] = -1;
                            }

                            if (blockY > 0) {
                                BlockModel adjacentRender = models.get((chunkY << 12) | (blockX << 8) | ((blockY - 1) << 4) | blockZ);
                                if (adjacentRender != null) {
                                    adjacentBlockShapes[2] = adjacentRender.shape;
                                } else {
                                    adjacentBlockShapes[2] = -1;
                                }
                            } else {
                                adjacentBlockShapes[2] = -1;
                            }

                            if (blockY < 15) {
                                BlockModel adjacentRender = models.get((chunkY << 12) | (blockX << 8) | ((blockY + 1) << 4) | blockZ);
                                if (adjacentRender != null) {
                                    adjacentBlockShapes[3] = adjacentRender.shape;
                                } else {
                                    adjacentBlockShapes[3] = -1;
                                }
                            } else {
                                adjacentBlockShapes[3] = -1;
                            }

                            if (blockZ > 0) {
                                BlockModel adjacentRender = models.get((chunkY << 12) | (blockX << 8) | (blockY << 4) | (blockZ - 1));
                                if (adjacentRender != null) {
                                    adjacentBlockShapes[4] = adjacentRender.shape;
                                } else {
                                    adjacentBlockShapes[4] = -1;
                                }
                            } else {
                                adjacentBlockShapes[4] = -1;
                            }

                            if (blockZ < 15) {
                                BlockModel adjacentRender = models.get((chunkY << 12) | (blockX << 8) | (blockY << 4) | (blockZ + 1));
                                if (adjacentRender != null) {
                                    adjacentBlockShapes[5] = adjacentRender.shape;
                                } else {
                                    adjacentBlockShapes[5] = -1;
                                }
                            } else {
                                adjacentBlockShapes[5] = -1;
                            }

                            if (model != null) {
                                model.removeBlockedFaces(adjacentBlockShapes);
                                replaceSquares(null, model);
                            }
                        }
                    }
                }
            }
        }
         */

        long time2 = System.currentTimeMillis();
        for (int i = 0; i < 16 * 16 * 16; i++) {
            short block = blocks[i];
            if (getBlockId(block) == 0) continue;
            BlockModel model = models[i];

            //find adjacent block shapes and call BlockModel.removeBlockedFaces() to remove faces that are blocked by adjacent blocks
            //this is done to prevent modeling faces that are not visible

            byte[] adjacentBlockShapes = new byte[6];
            //find adjacent models and get their block shapes

            if (i % 16 > 0) {
                BlockModel adjacentRender = models[i - 1];
                if (adjacentRender != null) {
                    adjacentBlockShapes[0] = adjacentRender.shape;
                } else {
                    adjacentBlockShapes[0] = -1;
                }
            } else {
                adjacentBlockShapes[0] = -1;
            }

            if (i % 16 < 15) {
                BlockModel adjacentRender = models[i + 1];
                if (adjacentRender != null) {
                    adjacentBlockShapes[1] = adjacentRender.shape;
                } else {
                    adjacentBlockShapes[1] = -1;
                }
            } else {
                adjacentBlockShapes[1] = -1;
            }

            if (i / 256 > 0) {
                BlockModel adjacentRender = models[i - 256];
                if (adjacentRender != null) {
                    adjacentBlockShapes[2] = adjacentRender.shape;
                } else {
                    adjacentBlockShapes[2] = -1;
                }
            } else {
                adjacentBlockShapes[2] = -1;
            }

            if (i / 256 < 15) {
                BlockModel adjacentRender = models[i + 256];
                if (adjacentRender != null) {
                    adjacentBlockShapes[3] = adjacentRender.shape;
                } else {
                    adjacentBlockShapes[3] = -1;
                }
            } else {
                adjacentBlockShapes[3] = -1;
            }

            if ((i / 16) % 16 > 0) {
                BlockModel adjacentRender = models[i - 16];
                if (adjacentRender != null) {
                    adjacentBlockShapes[4] = adjacentRender.shape;
                } else {
                    adjacentBlockShapes[4] = -1;
                }
            } else {
                adjacentBlockShapes[4] = -1;
            }

            if ((i / 16) % 16 < 15) {
                BlockModel adjacentRender = models[i + 16];
                if (adjacentRender != null) {
                    adjacentBlockShapes[5] = adjacentRender.shape;
                } else {
                    adjacentBlockShapes[5] = -1;
                }
            } else {
                adjacentBlockShapes[5] = -1;
            }

            if (model != null) {
                setRender(model.removeBlockedFaces(adjacentBlockShapes), i);
            }
        }
        //Log.v("Chunk", "removing blocked faces took " + (System.currentTimeMillis() - time2) + "ms");

        isLoaded = true;
    }
}
