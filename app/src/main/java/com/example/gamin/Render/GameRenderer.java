package com.example.gamin.Render;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.example.gamin.Minecraft.Inventory;
import com.example.gamin.Minecraft.Slot;
import com.example.gamin.R;
import com.example.gamin.Utils.PacketUtils;

import org.intellij.lang.annotations.Language;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {
    public static int fps = 0;
    private static int mProgram;
    private static int vPMatrixHandle;
    private static int positionHandle;
    private static int colorHandle;
    private static int mTextureCoordinateHandle;
    private static int whiteSquareHandle;
    private final float[] projectionMatrix = new float[16];
    private final float[] GuiProjectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] GuiViewMatrix = new float[16];
    private final Context context;
    public Inventory currentInventory = null;
    public float ratio;
    private long startTime = System.nanoTime();
    private int frames = 0;

    public GameRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.66f, 0.66f, 0.66f, 0.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDepthMask(true);
        //face culling should improve performance but it drops fps from 40 to 35 but why? it should improve performance not drop it :(
        //enable face culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        //specify which faces to not draw
        GLES20.glCullFace(GLES20.GL_BACK);
        //specify the front face
        GLES20.glFrontFace(GLES20.GL_CCW);
        @Language("GLSL")
        String vertexShaderCode = "uniform mat4 u_MVPMatrix;" +
                "attribute vec4 a_Position;" +
                "attribute vec4 a_Color;" +
                "attribute vec2 a_TexCoordinate;" +
                "varying vec4 v_Color;" +
                "varying vec2 v_TexCoordinate;" +
                "void main()" +
                "{" +
                "v_Color = a_Color;" +
                "v_TexCoordinate = a_TexCoordinate;" +
                "gl_Position = u_MVPMatrix * a_Position;" +
                "}";
        int vertexShader = OpenGLUtils.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        @Language("GLSL")
        String fragmentShaderCode = "precision mediump float;" +
                "uniform sampler2D u_Texture;" +
                "varying vec4 v_Color;" +
                "varying vec2 v_TexCoordinate;" +
                "void main() {" +
                "vec4 val = v_Color * texture2D(u_Texture, v_TexCoordinate);" +
                "if(val.a < 0.25){ discard; }" +
                "gl_FragColor = val;" +
                "}";
        int fragShader = OpenGLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = OpenGLUtils.createAndLinkProgram(vertexShader, fragShader, new String[]{"a_Position", "a_Color", "a_TexCoordinate"});

        GLES20.glUseProgram(GameRenderer.mProgram);

        vPMatrixHandle = GLES20.glGetUniformLocation(GameRenderer.mProgram, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(GameRenderer.mProgram, "a_Position");
        colorHandle = GLES20.glGetAttribLocation(GameRenderer.mProgram, "a_Color");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(GameRenderer.mProgram, "a_TexCoordinate");

        whiteSquareHandle = OpenGLUtils.loadTexture(context, R.drawable.white_square);

        TextureAtlas.atlases.values().forEach(atlas -> {
            Bitmap bitmap = atlas.bitmap;
            atlas.textureHandle = OpenGLUtils.loadTexture(bitmap);
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        ratio = (float) height / width;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.orthoM(GuiProjectionMatrix, 0, -1, 1, -ratio, ratio, 1, 10);
        Matrix.perspectiveM(projectionMatrix, 0, 110, ratio, 0.15f, 64);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {//TODO add chunk unloading, setblock backets and broken special block models
        float[] worldVPMatrix = new float[16];
        float[] GuiVPMatrix = new float[16];

        {
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glDepthMask(true);

            double yaw = PacketUtils.x_rot * Math.PI / 180;
            double pitch = PacketUtils.y_rot * Math.PI / 180;
            double upVectorPitch = -PacketUtils.y_rot * Math.PI / 180 + Math.PI / 2;
            // Set the camera position (View matrix) in the world
            Matrix.setLookAtM(viewMatrix, 0, (float) PacketUtils.x, (float) (PacketUtils.y + 1.62), (float) PacketUtils.z,
                    (float) (PacketUtils.x - Math.cos(pitch) * Math.sin(yaw)), (float) (PacketUtils.y + 1.62 - Math.sin(pitch)), (float) (PacketUtils.z + Math.cos(pitch) * Math.cos(yaw)),
                    (float) (-Math.cos(upVectorPitch) * Math.sin(yaw)), (float) (1.62 - Math.sin(upVectorPitch)), (float) (Math.cos(upVectorPitch) * Math.cos(yaw)));

            // Calculate the projection and view transformation
            Matrix.multiplyMM(worldVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            GLES20.glUniformMatrix4fv(GameRenderer.vPMatrixHandle, 1, false, worldVPMatrix, 0);

            // Create gui view matrix which is independent of the camera position
            Matrix.setLookAtM(GuiViewMatrix, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0);
            Matrix.multiplyMM(GuiVPMatrix, 0, GuiProjectionMatrix, 0, GuiViewMatrix, 0);
        }//Clear the screen and set the camera position

        /*
        {
            //Load a random chunk every frame to speed up loading
            //ChunkColumn[] chunks = PacketUtils.chunkColumnMap.values().toArray(new ChunkColumn[0]);
            //ChunkColumn randomMap = PacketUtils.chunkColumnMap.values().stream().filter(chunk -> chunk.isLoaded).findFirst().orElse(null);
            //randomMap.setRenders(context, 0, 0);

            //get 9 chunks near player and load them if they are not loaded
            ArrayList<Chunk> chunks = new ArrayList<>();
            long time = System.nanoTime();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    long chunkX = (long) (Math.floor(PacketUtils.x / 16) + x);
                    long chunkZ = (long) (Math.floor(PacketUtils.z / 16) + z);
                    long chunkPos = (chunkX << 32) | (chunkZ & 0xffffffffL);
                    Chunk[] chunkColumn = Chunk.chunkColumnMap.get(chunkPos);
                    if (chunkColumn == null) continue;
                    for (Chunk chunk : chunkColumn) {
                        if (chunk == null) continue;
                        if (!chunk.isLoaded) chunk.setRenders(context, (int) chunkX, (int) chunkZ);
                        chunks.add(chunk);
                    }
                }
            }
            if ((System.nanoTime() - time) > 1000000)
                Log.v("ChunkLoading", "time: " + (System.nanoTime() - time) / 1000000 + "ms");

            TextureAtlas blocks = TextureAtlas.atlases.get("blocks");
            TextureAtlas items = TextureAtlas.atlases.get("items");
            TextureAtlas entity = TextureAtlas.atlases.get("entity");
            assert blocks != null;
            assert items != null;
            assert entity != null;
            blocks.setBuffers(chunks);
            items.setBuffers(chunks);
            entity.setBuffers(chunks);
            //entity.setEntityBuffers(false);
            chunks.forEach(chunk -> chunk.isChanged = false);
            renderAtlas(blocks);
            renderAtlas(items);
            renderAtlas(entity);
            //rendsera(entity.entityBuffers, entity.entityBufferCapacity);
        }//Load chunks and render them
         */
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glDepthMask(false);

        {
            if (PacketUtils.targetCoords != null) {
                //render targeted object
                float[] targetCoords = PacketUtils.targetCoords;
                float[] colors = new float[]{
                        0, 1, 0, 0.5f,
                        0, 1, 0, 0.5f,
                        0, 1, 0, 0.5f,
                        0, 1, 0, 0.5f,
                        0, 1, 0, 0.5f,
                        0, 1, 0, 0.5f
                };
                float[] textureCoords = new float[]{
                        0, 0,
                        0, 1,
                        1, 1,
                        0, 0,
                        1, 1,
                        1, 0
                };
                renderTriangles(targetCoords, colors, textureCoords, whiteSquareHandle);
            }
        }//Render targeted block

        {
            List<Float> hitboxCoords;
            List<Float> hitboxColors;
            List<Float> hitboxTextureCoords;

            //Entity.entityChunks.keySet().forEach(System.out::println);
            hitboxCoords = new ArrayList<>();
            hitboxColors = new ArrayList<>();
            hitboxTextureCoords = new ArrayList<>();
            synchronized ("entity") {
                long x = (long) PacketUtils.x / 8;
                long z = (long) PacketUtils.z / 8;
                long y = (long) PacketUtils.y / 8;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            long entityChunkPos = ((x + dx) << 35) | ((z + dz) << 6) | (y + dy);
                            List<Entity> entities = Entity.entityChunks.get(entityChunkPos);
                            if (entities == null) continue;
                            for (Entity v : entities) {
                                float[] hitbox = v.getHitbox();
                                if (hitbox == null) continue;
                                float[][] rectangularPrism = Square.getRectangularPrism(new float[]{hitbox[0], hitbox[1], hitbox[2]}, new float[]{hitbox[3], hitbox[4], hitbox[5]});
                                float[] coords = new float[6 * 6 * 3 * 3];
                                for (int i = 0; i < rectangularPrism.length; i++) {
                                    float[] rect = Square.fourCoordsToSix(rectangularPrism[i]);
                                    System.arraycopy(rect, 0, coords, i * 18, 18);
                                }
                                float[] colors = new float[6 * 6 * 3 * 4];
                                for (int i = 0; i < 6 * 6 * 3 * 4; i += 4) {
                                    colors[i] = 0.5f;
                                    colors[i + 1] = 0.5f;
                                    colors[i + 2] = 1;
                                    colors[i + 3] = 0.5f;
                                    if (v == PacketUtils.targetEntity) {
                                        colors[i] = 1;
                                        colors[i + 1] = 0.25f;
                                        colors[i + 2] = 0.25f;
                                        colors[i + 3] = 0.5f;
                                    }
                                }
                                float[] textureCoords = new float[6 * 6 * 3 * 2];

                                for (float f : coords) {
                                    hitboxCoords.add(f);
                                }

                                for (float f : colors) {
                                    hitboxColors.add(f);
                                }

                                for (float f : textureCoords) {
                                    hitboxTextureCoords.add(f);
                                }
                            }

                        }
                    }
                }
            }


            float[] hitboxCoordsArray = new float[hitboxCoords.size()];
            float[] hitboxColorsArray = new float[hitboxColors.size()];
            float[] hitboxTextureCoordsArray = new float[hitboxTextureCoords.size()];

            for (int i = 0; i < hitboxCoords.size(); i++)
                hitboxCoordsArray[i] = hitboxCoords.get(i);
            for (int i = 0; i < hitboxColors.size(); i++)
                hitboxColorsArray[i] = hitboxColors.get(i);
            for (int i = 0; i < hitboxTextureCoords.size(); i++)
                hitboxTextureCoordsArray[i] = hitboxTextureCoords.get(i);

            renderTriangles(hitboxCoordsArray, hitboxColorsArray, hitboxTextureCoordsArray, whiteSquareHandle);
        }//render entity hitboxes

        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, GuiVPMatrix, 0);

        {
            float[] jumpButtonUpperTriangle = new float[]{
                    -0.8f, 0.4f - ratio, -1,
                    -0.6f, 0.2f - ratio, -1,
                    -0.6f, 0.4f - ratio, -1,
            };
            float[] whiteTriangleColor = new float[]{
                    1.0f, 1.0f, 1.0f, 0.50f,
                    1.0f, 1.0f, 1.0f, 0.50f,
                    1.0f, 1.0f, 1.0f, 0.50f
            };
            float[] defaultTextureCoords = new float[]{
                    0.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f
            };
            float[] jumpButtonLowerTriangle = new float[]{
                    -0.8f, 0.4f - ratio, -1,
                    -0.8f, 0.2f - ratio, -1,
                    -0.6f, 0.2f - ratio, -1,
            };
            float[] sneakButtonCoords = new float[]{
                    0.8f, 0.3f - ratio, -1,
                    0.7f, 0.3f - ratio, -1,
                    0.7f, 0.2f - ratio, -1,
            };
            float[] crosshairCoords = new float[]{
                    -0.1f, 0.1f, -1,
                    -0.1f, -0.1f, -1,
                    0.1f, -0.1f, -1,
                    -0.1f, 0.1f, -1,
                    0.1f, -0.1f, -1,
                    0.1f, 0.1f, -1
            };
            float[] crosshairColors = new float[]{
                    1, 1, 1, 0.5f,
                    1, 1, 1, 0.5f,
                    1, 1, 1, 0.5f,
                    1, 1, 1, 0.5f,
                    1, 1, 1, 0.5f,
                    1, 1, 1, 0.5f
            };
            float[] crosshairTextureCoords = new float[]{
                    0, 0,
                    0, 1,
                    1, 1,
                    0, 0,
                    1, 1,
                    1, 0
            };
            float[] hotbarCoords = new float[]{
                    -0.9f, 0.2f - ratio, -1,
                    -0.9f, -ratio, -1,
                    0.9f, -ratio, -1,
                    -0.9f, 0.2f - ratio, -1,
                    0.9f, -ratio, -1,
                    0.9f, 0.2f - ratio, -1
            };
            float[] hotbarColors = new float[]{
                    1, 1, 1, 1,
                    1, 1, 1, 1,
                    1, 1, 1, 1,
                    1, 1, 1, 1,
                    1, 1, 1, 1,
                    1, 1, 1, 1
            };
            float[] hotbarTextureCoords = new float[]{
                    1.0f / 256, 1.0f / 256,
                    1.0f / 256, 21.0f / 256,
                    181.0f / 256, 21.0f / 256,
                    1.0f / 256, 1.0f / 256,
                    181.0f / 256, 21.0f / 256,
                    181.0f / 256, 1.0f / 256,
            };
            int hotbarHandle = OpenGLUtils.loadTexture(context, R.drawable.widgets);
            int crosshairHandle = OpenGLUtils.loadTexture(context, R.drawable.icons);
            int testInventoryHandle = OpenGLUtils.loadTexture(context, R.drawable.generic_54);
            renderTriangles(jumpButtonLowerTriangle, whiteTriangleColor, defaultTextureCoords, whiteSquareHandle);
            renderTriangles(jumpButtonUpperTriangle, whiteTriangleColor, defaultTextureCoords, whiteSquareHandle);
            renderTriangles(sneakButtonCoords, whiteTriangleColor, defaultTextureCoords, whiteSquareHandle);
            renderTriangles(crosshairCoords, crosshairColors, crosshairTextureCoords, crosshairHandle);
            renderTriangles(hotbarCoords, hotbarColors, hotbarTextureCoords, hotbarHandle);
        }//render GUI

        if (currentInventory != null) {
            float[] inventoryCoords = currentInventory.getCoords(ratio);
            float[] inventoryTexCoords = currentInventory.getTexCoords();
            float[] inventoryColors = currentInventory.getColors();
            int currentInventoryHandle = OpenGLUtils.loadTexture(context, currentInventory.resId);
            renderTriangles(inventoryCoords, inventoryColors, inventoryTexCoords, currentInventoryHandle);

            GLES20.glDepthMask(true);

            for (int i = 0; i < currentInventory.contents.length; i++) {
                Slot slot = currentInventory.contents[i];
                if (slot == null) continue;
                float[] slotCoords = currentInventory.itemModelCoords[i];
                float[] slotTexCoords = slot.itemModel.textureCoords;
                float[] slotColors = slot.itemModel.colors;
                renderTriangles(slotCoords, slotColors, slotTexCoords, slot.itemModel.textureAtlas.textureHandle);
            }
        }

        Inventory playerInventory = Inventory.inventoryMap.get((byte) 0);
        if (playerInventory != null) {
            //render hotbar items
            for (int i = 36; i < 45; i++) {
                Slot slot = playerInventory.contents[i];
                if (slot == null) continue;
                float pixelSize = 1.8f / 180;
                float[] slotCoords = slot.itemModel.getCoordinatesInInventory(-0.9f + 0.2f * (i - 36) + 2 * pixelSize, -ratio + 2 * pixelSize, pixelSize);
                float[] slotTexCoords = slot.itemModel.textureCoords;
                float[] slotColors = slot.itemModel.colors;
                renderTriangles(slotCoords, slotColors, slotTexCoords, slot.itemModel.textureAtlas.textureHandle);
            }
        }

        {
            if (System.nanoTime() - startTime > 1000000000) {
                Log.d("FPS", "fps: " + frames);
                fps = frames;
                frames = 0;
                startTime = System.nanoTime();
            } else {
                frames++;
            }
        } //FPS logging
    }

    private void renderTriangles(float[] targetCoords, float[] colors, float[] textureCoords, int textureHandle) {
        FloatBuffer targetCoordsBuffer = ByteBuffer.allocateDirect(targetCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        targetCoordsBuffer.put(targetCoords).position(0);

        FloatBuffer colorsBuffer = ByteBuffer.allocateDirect(colors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorsBuffer.put(colors).position(0);

        FloatBuffer textureCoordsBuffer = ByteBuffer.allocateDirect(textureCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureCoordsBuffer.put(textureCoords).position(0);

        GLES20.glEnableVertexAttribArray(GameRenderer.colorHandle);
        GLES20.glVertexAttribPointer(GameRenderer.colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorsBuffer);
        GLES20.glEnableVertexAttribArray(GameRenderer.positionHandle);
        GLES20.glVertexAttribPointer(GameRenderer.positionHandle, 3, GLES20.GL_FLOAT, false, 0, targetCoordsBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        GLES20.glEnableVertexAttribArray(GameRenderer.mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(GameRenderer.mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureCoordsBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, targetCoords.length / 3);
    }

    private void renderAtlas(TextureAtlas atlas) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlas.textureHandle);

        renderBuffers(atlas.buffers, atlas.squareCount);
    }

    private void renderBuffers(int[] buffers, int squareCount) {

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glEnableVertexAttribArray(GameRenderer.positionHandle);
        GLES20.glVertexAttribPointer(GameRenderer.positionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
        GLES20.glEnableVertexAttribArray(GameRenderer.colorHandle);
        GLES20.glVertexAttribPointer(GameRenderer.colorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2]);
        GLES20.glEnableVertexAttribArray(GameRenderer.mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(GameRenderer.mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, squareCount * 6);

    }

    public int clickInventory(float xRatio, float yRatio) {
        if (currentInventory == null) return -2;
        float widthInRenderer = xRatio * 2 - 1;
        float heightInRenderer = ((1 - yRatio) * 2 - 1) * ratio;
        return currentInventory.getClickedSlotIndex(widthInRenderer, heightInRenderer, ratio);
    }
}
