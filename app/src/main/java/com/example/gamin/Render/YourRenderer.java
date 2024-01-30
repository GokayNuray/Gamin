package com.example.gamin.Render;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.example.gamin.Minecraft.Chunk;
import com.example.gamin.R;
import com.example.gamin.Utils.PacketUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class YourRenderer implements GLSurfaceView.Renderer {
    public static int fps = 0;
    private static int mProgram;
    private static int vPMatrixHandle;
    private static int positionHandle;
    private static int colorHandle;
    private static int mTextureCoordinateHandle;
    private final float[] projectionMatrix = new float[16];
    private final float[] projectionMatrix2 = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewMatrix2 = new float[16];
    private final Context context;
    private long startTime = System.nanoTime();
    private int frames = 0;
    private float ratio;

    public YourRenderer(Context context) {
        this.context = context;
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static void loadTextures(Context context) throws IOException {
        String[] paths = {"blocks", "items", "entity"};
        for (String path : paths) {
            TextureAtlas.atlases.put(path, compressTextures(context, "textures/" + path + "/"));
        }
    }

    private static TextureAtlas compressTextures(Context context, String path) throws IOException {
        AssetManager assetManager = context.getAssets();
        String[] names = assetManager.list(path);

        Map<String, Integer> textureOffsets = new HashMap<>();
        ArrayList<Bitmap> textures = new ArrayList<>();

        int height = 0;
        int offset = 0;
        assert names != null : "invalid path: " + path;
        for (String texture : names) {
            if (texture.endsWith(".png")) {
                Bitmap bitmap = BitmapFactory.decodeStream(assetManager.open(path + texture));
                textures.add(bitmap);
                textureOffsets.put(texture, offset);
                offset += bitmap.getWidth();
                height = Math.max(height, bitmap.getHeight());
            } else {
                for (String s : assetManager.list(path + texture + "/")) {
                    if (!s.endsWith(".png")) {
                        Log.w("TextureLoading", "invalid file: " + path + texture + "/" + s);
                        continue;
                    }
                    Bitmap bitmap = BitmapFactory.decodeStream(assetManager.open(path + texture + "/" + s));
                    textures.add(bitmap);
                    textureOffsets.put(texture + "/" + s, offset);
                    offset += bitmap.getWidth();
                    height = Math.max(height, bitmap.getHeight());
                }
            }
        }
        Bitmap blockAtlas = Bitmap.createBitmap(offset, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(blockAtlas);
        offset = 0;
        for (Bitmap texture : textures) {
            canvas.drawBitmap(texture, offset, 0, null);
            offset += texture.getWidth();
        }
        textures.forEach(Bitmap::recycle);
        return new TextureAtlas(blockAtlas, textureOffsets);
    }

    private static int loadTexture(Context context, int resId) {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        //FIXME why can resId be 0?
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId == 0 ? R.drawable.apple : resId, options);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        if (bitmap == null) throw new RuntimeException(resId + " is null");
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        return textureHandle[0];
    }

    private static int loadTexture(Bitmap bitmap) {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        if (bitmap == null) throw new RuntimeException("bitmap is null");
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        return textureHandle[0];
    }

    private static int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }
            GLES20.glLinkProgram(programHandle);
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
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
        int vertexShader = YourRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        String fragmentShaderCode = "precision mediump float;" +
                "uniform sampler2D u_Texture;" +
                "varying vec4 v_Color;" +
                "varying vec2 v_TexCoordinate;" +
                "void main() {" +
                "vec4 val = v_Color * texture2D(u_Texture, v_TexCoordinate);" +
                "if(val.a < 0.5){ discard; }" +
                "gl_FragColor = val;" +
                "}";
        int fragShader = YourRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = YourRenderer.createAndLinkProgram(vertexShader, fragShader, new String[]{"a_Position", "a_Color", "a_TexCoordinate"});

        GLES20.glUseProgram(YourRenderer.mProgram);

        vPMatrixHandle = GLES20.glGetUniformLocation(YourRenderer.mProgram, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(YourRenderer.mProgram, "a_Position");
        colorHandle = GLES20.glGetAttribLocation(YourRenderer.mProgram, "a_Color");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(YourRenderer.mProgram, "a_TexCoordinate");

        TextureAtlas.atlases.values().forEach(atlas -> {
            Bitmap bitmap = atlas.bitmap;
            atlas.textureHandle = loadTexture(bitmap);
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        ratio = (float) height / width;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix2, 0, -1, 1, -ratio, ratio, 1, 2);
        Matrix.perspectiveM(projectionMatrix, 0, 110, ratio, 0.15f, 64);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {//TODO add chunk unloading, setblock backets and broken special block models
        float[] scratch = new float[16];
        float[] scratch2 = new float[16];

        double yaw = PacketUtils.x_rot * Math.PI / 180;
        double pitch = PacketUtils.y_rot * Math.PI / 180;
        double pitch2 = -PacketUtils.y_rot * Math.PI / 180 + Math.PI / 2;

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.setLookAtM(viewMatrix, 0, (float) PacketUtils.x, (float) (PacketUtils.y + 1.62), (float) PacketUtils.z,
                (float) (PacketUtils.x - Math.cos(pitch) * Math.sin(yaw)), (float) (PacketUtils.y + 1.62 - Math.sin(pitch)), (float) (PacketUtils.z + Math.cos(pitch) * Math.cos(yaw)),
                (float) (-Math.cos(pitch2) * Math.sin(yaw)), (float) (1.62 - Math.sin(pitch2)), (float) (Math.cos(pitch2) * Math.cos(yaw)));

        // Calculate the projection and view transformation
        Matrix.multiplyMM(scratch, 0, projectionMatrix, 0, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(YourRenderer.vPMatrixHandle, 1, false, scratch, 0);

        Matrix.setLookAtM(viewMatrix2, 0, 0, 0, 0, 0, 0, -1, 0, 10, 0);
        Matrix.multiplyMM(scratch2, 0, projectionMatrix2, 0, viewMatrix2, 0);


        //ChunkColumn[] chunks = PacketUtils.chunkColumnMap.values().toArray(new ChunkColumn[0]);
        //ChunkColumn randomMap = PacketUtils.chunkColumnMap.values().stream().filter(chunk -> chunk.isLoaded).findFirst().orElse(null);
        //randomMap.setRenders(context, 0, 0);
        //get 9 chunks near player
        ArrayList<Chunk> chunks = new ArrayList<>();
        long time = System.nanoTime();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
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
        chunks.forEach(chunk -> chunk.isChanged = false);
        rendsera(blocks);
        rendsera(items);
        rendsera(entity);

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

            FloatBuffer targetCoordsBuffer = ByteBuffer.allocateDirect(targetCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            targetCoordsBuffer.put(targetCoords).position(0);

            FloatBuffer colorsBuffer = ByteBuffer.allocateDirect(colors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            colorsBuffer.put(colors).position(0);

            FloatBuffer textureCoordsBuffer = ByteBuffer.allocateDirect(textureCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            textureCoordsBuffer.put(textureCoords).position(0);

            GLES20.glEnableVertexAttribArray(YourRenderer.colorHandle);
            GLES20.glVertexAttribPointer(YourRenderer.colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorsBuffer);
            GLES20.glEnableVertexAttribArray(YourRenderer.positionHandle);
            GLES20.glVertexAttribPointer(YourRenderer.positionHandle, 3, GLES20.GL_FLOAT, false, 0, targetCoordsBuffer);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, loadTexture(context, R.drawable.white_square));

            GLES20.glEnableVertexAttribArray(YourRenderer.mTextureCoordinateHandle);
            GLES20.glVertexAttribPointer(YourRenderer.mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureCoordsBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }


        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, scratch2, 0);
        //System.out.println("ratio: " + ratio);//0.625

        //tringul is a triangle with coords of 3 vertices of a triangle in 3d space and tringul2 is a color of each vertex and tringul3 is texture coords
        //complete the tringul to make a square
        float[] tringul = new float[]{
                -0.8f, 0.4f - ratio, -1,
                -0.6f, 0.4f - ratio, -1,
                -0.6f, 0.2f - ratio, -1,
        };
        float[] tringul2 = new float[]{
                1.0f, 1.0f, 1.0f, 0.50f,
                1.0f, 1.0f, 1.0f, 0.50f,
                1.0f, 1.0f, 1.0f, 0.50f
        }; //color of each vertex
        float[] tringul3 = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
        }; //texture coords
        //second triangle to complete the square
        float[] tringul4 = new float[]{
                -0.8f, 0.4f - ratio, -1,
                -0.8f, 0.2f - ratio, -1,
                -0.6f, 0.2f - ratio, -1,
        };//coords of 3 vertices of a triangle in 3d space

        //triangle in bottom right corner
        float[] tringul5 = new float[]{
                0.8f, 0.3f - ratio, -1,
                0.7f, 0.3f - ratio, -1,
                0.7f, 0.2f - ratio, -1,
        }; //coords of 3 vertices of a triangle in 3d space

        //WHY?? code crashes when there are no blocks in screen without this code(texture coords)
        //but works fine otherwise
        // I guess it used texture coords of the previous triangle and crashed when there is none
        FloatBuffer tringulBuffer = ByteBuffer.allocateDirect(36 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        tringulBuffer.put(tringul).put(tringul4).put(tringul5).position(0);

        FloatBuffer tringulBuffer2 = ByteBuffer.allocateDirect(48 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        tringulBuffer2.put(tringul2).put(tringul2).put(tringul2).position(0);

        //WHY??
        FloatBuffer tringulTextureBuffer = ByteBuffer.allocateDirect(24 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        tringulTextureBuffer.put(tringul3).put(tringul3).put(tringul3).position(0);

        GLES20.glEnableVertexAttribArray(YourRenderer.colorHandle);
        GLES20.glVertexAttribPointer(YourRenderer.colorHandle, 4, GLES20.GL_FLOAT, false, 0, tringulBuffer2);
        GLES20.glEnableVertexAttribArray(YourRenderer.positionHandle);
        GLES20.glVertexAttribPointer(YourRenderer.positionHandle, 3, GLES20.GL_FLOAT, false, 0, tringulBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, loadTexture(context, R.drawable.white_square));

        //WHY??
        GLES20.glEnableVertexAttribArray(YourRenderer.mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(YourRenderer.mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, tringulTextureBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3 * 3);

        if (System.nanoTime() - startTime > 1000000000) {
            Log.d("FPS", "fps: " + frames);
            fps = frames;
            frames = 0;
            startTime = System.nanoTime();
        } else {
            frames++;
        }
    }

    private void rendsera(TextureAtlas atlas) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlas.textureHandle);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, atlas.buffers[0]);
        GLES20.glEnableVertexAttribArray(YourRenderer.positionHandle);
        GLES20.glVertexAttribPointer(YourRenderer.positionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, atlas.buffers[1]);
        GLES20.glEnableVertexAttribArray(YourRenderer.colorHandle);
        GLES20.glVertexAttribPointer(YourRenderer.colorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, atlas.buffers[2]);
        GLES20.glEnableVertexAttribArray(YourRenderer.mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(YourRenderer.mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, atlas.squareCount * 6);

    }

}
