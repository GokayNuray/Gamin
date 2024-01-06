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

import com.example.gamin.Minecraft.ChunkColumn;
import com.example.gamin.R;
import com.example.gamin.Utils.PacketUtils;

import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//TODO use VBOs to improve performance
public class YourRenderer implements GLSurfaceView.Renderer {
    public static int mProgram;
    public static int vPMatrixHandle;
    public static int positionHandle;
    public static int colorHandle;
    public static int mTextureUniformHandle;
    public static int mTextureCoordinateHandle;
    public static List<Float> blockCoords = new ArrayList<>();
    public static List<Float> blockTextures = new ArrayList<>();
    public static List<Float> blockColors = new ArrayList<>();
    public static List<Float> itemCoords = new ArrayList<>();
    public static List<Float> itemTextures = new ArrayList<>();
    public static List<Float> itemColors = new ArrayList<>();
    private final float[] projectionMatrix = new float[16];
    private final float[] projectionMatrix2 = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewMatrix2 = new float[16];
    public String newSlot = "crafting_table.json";
    Context context;
    float[] color = {1.0f, 1.0f, 1.0f, 1.0f};
    long startTime = System.nanoTime();
    int frames = 0;
    private float ratio;
    public static TextureAtlas blocksAtlas;
    public static TextureAtlas itemsAtlas;
    public static int blocksTextureHandle;
    public static int itemsTextureHandle;

    public YourRenderer(Context context) {
        this.context = context;
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static void loadTextures(Context context) throws IOException {
        blocksAtlas = compressTextures(context, "textures/blocks/");
        itemsAtlas = compressTextures(context, "textures/items/");
    }

    public static TextureAtlas compressTextures(Context context, String path) throws IOException {
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

    public static int loadTexture(Context context, int resId) {
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

    public static int loadTexture(Context context, Bitmap bitmap) {
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

    public static int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
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
        mTextureUniformHandle = GLES20.glGetUniformLocation(YourRenderer.mProgram, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(YourRenderer.mProgram, "a_TexCoordinate");

        Bitmap bitmap = blocksAtlas.bitmap;
        blocksTextureHandle = loadTexture(context, bitmap);
        Bitmap bitmap2 = itemsAtlas.bitmap;
        itemsTextureHandle = loadTexture(context, bitmap2);
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
    public void onDrawFrame(GL10 gl10) {
        float[] scratch = new float[16];
        float[] scratch2 = new float[16];

        double yaw = PacketUtils.x_rot * Math.PI / 180;
        double pitch = PacketUtils.y_rot * Math.PI / 180;
        double pitch2 = -PacketUtils.y_rot * Math.PI / 180 + Math.PI / 2;

        newSlot = "grass.json";
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.setLookAtM(viewMatrix, 0, (float) PacketUtils.x, (float) (PacketUtils.y + 1.62), (float) PacketUtils.z,
                (float) (PacketUtils.x - Math.cos(pitch) * Math.sin(yaw)), (float) (PacketUtils.y + 1.62 - Math.sin(pitch)), (float) (PacketUtils.z + Math.cos(pitch) * Math.cos(yaw)),
                (float) (-Math.cos(pitch2) * Math.sin(yaw)), (float) (1.62 - Math.sin(pitch2)), (float) (Math.cos(pitch2) * Math.cos(yaw)));

        // Calculate the projection and view transformation
        Matrix.multiplyMM(scratch, 0, projectionMatrix, 0, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(YourRenderer.vPMatrixHandle, 1, false, scratch, 0);

        Matrix.setLookAtM(viewMatrix2, 0, 0, 0, 0, 0, 0, -1, 0, 10, 0);
        Matrix.multiplyMM(scratch2, 0, projectionMatrix2, 0, viewMatrix2, 0);

        for (int dx = -15; dx < 15; dx++) {
            for (int dy = -45; dy < 45; dy++) {
                for (int dz = -15; dz < 15; dz++) {
                    short block = ChunkColumn.getBlock((int) PacketUtils.x + dx, (int) PacketUtils.y + dy, (int) PacketUtils.z + dz);
                    if (block != 0/* && (int) PacketUtils.x != 616*/) {
                        try {
                            new SlotRenderer(context, color, ChunkColumn.getBlockId(block), ChunkColumn.getBlockMetaData(block), 1, (int) PacketUtils.x + dx, (int) PacketUtils.y + dy, (int) PacketUtils.z + dz);
                        } catch (IOException | JSONException e) {
                            System.out.println("crush " + ChunkColumn.getBlockId(block) + " " + ChunkColumn.getBlockMetaData(block));
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        rendsera(context, blockCoords, blockTextures, blockColors, blocksTextureHandle);
        rendsera(context, itemCoords, itemTextures, itemColors, itemsTextureHandle);

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
            frames = 0;
            startTime = System.nanoTime();
        } else {
            frames++;
        }
    }

    public static void rendsera(Context context, List<Float> coords, List<Float> textures, List<Float> colors, int textureHandle) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        FloatBuffer colorBuffer = ByteBuffer.allocateDirect(colors.size() * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        FloatBuffer coordsBuffer = ByteBuffer.allocateDirect(coords.size() * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        FloatBuffer textureBuffer = ByteBuffer.allocateDirect(textures.size() * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

        for (float f : coords) {
            coordsBuffer.put(f);
        }
        coordsBuffer.position(0);

        for (float f : textures) {
            textureBuffer.put(f);
        }
        textureBuffer.position(0);

        for (float f : colors) {
            colorBuffer.put(f);
        }
        colorBuffer.position(0);

        GLES20.glEnableVertexAttribArray(YourRenderer.colorHandle);
        GLES20.glVertexAttribPointer(YourRenderer.colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        GLES20.glEnableVertexAttribArray(YourRenderer.positionHandle);
        GLES20.glVertexAttribPointer(YourRenderer.positionHandle, 3, GLES20.GL_FLOAT, false, 0, coordsBuffer);

        GLES20.glEnableVertexAttribArray(YourRenderer.mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(YourRenderer.mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, coords.size() / 3);

        coords.clear();
        textures.clear();
        colors.clear();

    }
}
