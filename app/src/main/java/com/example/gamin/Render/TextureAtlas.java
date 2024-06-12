package com.example.gamin.Render;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import com.example.gamin.Minecraft.Chunk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextureAtlas {
    static final Map<String, TextureAtlas> atlases = new HashMap<>();
    public final int width;
    public final int height;
    final Bitmap bitmap;
    final Map<String, Float> offsets = new HashMap<>();
    private final Map<Chunk, Integer> chunkOffsets = new HashMap<>();
    private final Map<Chunk, Integer> chunkCapacities = new HashMap<>();
    private final Map<Entity, Integer> entityOffsets = new HashMap<>();
    int entityBufferCapacity = 0;
    int squareCount = 0;
    int[] buffers = null;
    int[] entityBuffers = null;
    int textureHandle;
    private int entitySquareCount;

    TextureAtlas(Bitmap bitmap, Map<String, Integer> offsets) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        offsets.forEach((key, value) -> this.offsets.put(key, value / (float) width));
    }

    void setBuffers(List<Chunk> chunks) {
        boolean isCapacityEnough = true;
        for (Chunk chunk : chunks) {
            if (chunkCapacities.get(chunk) == null || chunkCapacities.get(chunk) < chunk.squares.get(this)) {
                isCapacityEnough = false;
                break;
            }
        }
        if (buffers == null || !isCapacityEnough) {
            if (buffers != null) GLES20.glDeleteBuffers(3, buffers, 0);
            squareCount = 0;
            buffers = new int[3];
            GLES20.glGenBuffers(3, buffers, 0);
            for (Chunk chunk : chunks) {
                if (chunk.squares.get(this) == null) continue;
                int capacity = chunk.squares.get(this) * 2;
                chunkOffsets.put(chunk, squareCount);
                chunkCapacities.put(chunk, capacity);
                squareCount += capacity;
            }

            FloatBuffer coordsBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            FloatBuffer colorsBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            FloatBuffer texturesBuffer = ByteBuffer.allocateDirect(squareCount * 6 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, coordsBuffer.capacity() * 4, coordsBuffer, GLES20.GL_DYNAMIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, colorsBuffer.capacity() * 4, colorsBuffer, GLES20.GL_DYNAMIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texturesBuffer.capacity() * 4, texturesBuffer, GLES20.GL_DYNAMIC_DRAW);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }
        if (squareCount == 0) return;

        for (Chunk chunk : chunks) {
            if (!chunk.isChanged && isCapacityEnough) continue;
            synchronized (chunk) {
                chunk.setBuffers();
                FloatBuffer chunkCoordsBuffer = chunk.coordsBuffers.get(this);
                FloatBuffer chunkColorsBuffer = chunk.colorsBuffers.get(this);
                FloatBuffer chunkTexturesBuffer = chunk.texturesBuffers.get(this);
                if (chunkCoordsBuffer == null) continue;

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
                GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, chunkOffsets.get(chunk) * 6 * 3 * 4, chunkCoordsBuffer.capacity() * 4, chunkCoordsBuffer);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
                GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, chunkOffsets.get(chunk) * 6 * 4 * 4, chunkColorsBuffer.capacity() * 4, chunkColorsBuffer);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2]);
                GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, chunkOffsets.get(chunk) * 6 * 2 * 4, chunkTexturesBuffer.capacity() * 4, chunkTexturesBuffer);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

                chunkCoordsBuffer.position(0);
                chunkColorsBuffer.position(0);
                chunkTexturesBuffer.position(0);
            }
        }
    }

    void setEntityBuffers(boolean needMoreCapacity) {
        synchronized ("entity") {
            if (entityBuffers == null || needMoreCapacity) {
                if (entityBuffers != null) {
                    Log.d("TextureAtlas", "doubling entity buffer capacity");
                    GLES20.glDeleteBuffers(3, entityBuffers, 0);
                }
                entitySquareCount = 0;

                for (Entity entity : Entity.entities.values()) {
                    int capacity = entity.squares.size();
                    entityOffsets.put(entity, entitySquareCount);
                    entitySquareCount += capacity;
                }
                entityBufferCapacity = entitySquareCount * 2;

                entityBuffers = new int[3];
                GLES20.glGenBuffers(3, entityBuffers, 0);

                FloatBuffer coordsBuffer = ByteBuffer.allocateDirect(entitySquareCount * 2 * 6 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                FloatBuffer colorsBuffer = ByteBuffer.allocateDirect(entitySquareCount * 2 * 6 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                FloatBuffer texturesBuffer = ByteBuffer.allocateDirect(entitySquareCount * 2 * 6 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entityBuffers[0]);
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, coordsBuffer.capacity() * 4, coordsBuffer, GLES20.GL_DYNAMIC_DRAW);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entityBuffers[1]);
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, colorsBuffer.capacity() * 4, colorsBuffer, GLES20.GL_DYNAMIC_DRAW);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entityBuffers[2]);
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texturesBuffer.capacity() * 4, texturesBuffer, GLES20.GL_DYNAMIC_DRAW);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            }

            for (Entity entity : Entity.entities.values()) {
                if (!entityOffsets.containsKey(entity)) {
                    if (entitySquareCount + entity.squares.size() > entityBufferCapacity) {
                        setEntityBuffers(true);
                        return;
                    }
                    int capacity = entity.squares.size();
                    entityOffsets.put(entity, entitySquareCount);
                    entitySquareCount += capacity;
                }

                if (entity.setBuffers()) {
                    FloatBuffer entityCoordsBuffer = entity.coordsBuffer;
                    FloatBuffer entityColorsBuffer = entity.colorsBuffer;
                    FloatBuffer entityTexturesBuffer = entity.texturesBuffer;

                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entityBuffers[0]);
                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, entityOffsets.get(entity) * 6 * 3 * 4, entityCoordsBuffer.capacity() * 4, entityCoordsBuffer);

                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entityBuffers[1]);
                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, entityOffsets.get(entity) * 6 * 4 * 4, entityColorsBuffer.capacity() * 4, entityColorsBuffer);

                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entityBuffers[2]);
                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, entityOffsets.get(entity) * 6 * 2 * 4, entityTexturesBuffer.capacity() * 4, entityTexturesBuffer);

                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

                    entityCoordsBuffer.position(0);
                    entityColorsBuffer.position(0);
                    entityTexturesBuffer.position(0);
                }
            }
        }
    }

}
