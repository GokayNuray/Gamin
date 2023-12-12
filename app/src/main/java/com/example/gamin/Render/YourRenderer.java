package com.example.gamin.Render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.example.gamin.Minecraft.ChunkColumn;
import com.example.gamin.Minecraft.Slot;
import com.example.gamin.R;
import com.example.gamin.Utils.PacketUtils;

import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public static Map<Integer, List<float[]>> coords = new HashMap<>();
    public static Map<Integer, List<float[]>> textures = new HashMap<>();
    public static Map<Integer, List<float[]>> colors = new HashMap<>();
    private final float[] vPMatrix = new float[16];
    private final float[] vPMatrix2 = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] projectionMatrix2 = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewMatrix2 = new float[16];
    private final float[] rotationMatrix = new float[16];
    private final float[] rotationMatrix2 = new float[16];
    public String newSlot = "crafting_table.json";
    Context context;
    float[] color = {1.0f, 1.0f, 1.0f, 1.0f};
    private float ratio;

    public YourRenderer(Context context) {
        this.context = context;
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
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
        //GLES20.glEnable(GLES20.GL_BLEND);
        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        /*GLES20.glDepthFunc( GLES20.GL_LEQUAL )
        GLES20.glDepthMask( true )*/
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


        /*if (PacketUtils.y < -10) Matrix.setLookAtM(viewMatrix, 0, 0, 0, 0, 0, 1, 0 ,0 ,1, 0);
        else*/
        Matrix.setLookAtM(viewMatrix, 0, (float) PacketUtils.x, (float) (PacketUtils.y + 1.62), (float) PacketUtils.z,
                (float) (PacketUtils.x - Math.cos(pitch) * Math.sin(yaw)), (float) (PacketUtils.y + 1.62 - Math.sin(pitch)), (float) (PacketUtils.z + Math.cos(pitch) * Math.cos(yaw)),
                (float) (-Math.cos(pitch2) * Math.sin(yaw)), (float) (1.62 - Math.sin(pitch2)), (float) (Math.cos(pitch2) * Math.cos(yaw)));

        //0.01f, 1.0f, 0.01f);
        //(float) PacketUtils.x + 0, (float) (PacketUtils.y - 10), (float) PacketUtils.z + 0);
        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        //long time = SystemClock.uptimeMillis() % 4000L;
        float angle = 0;//0.090f * ((int) time);
        Matrix.setRotateM(rotationMatrix, 0, angle, 1.0f, 1.0f, 1.0f);
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0);
        GLES20.glUniformMatrix4fv(YourRenderer.vPMatrixHandle, 1, false, scratch, 0);

        Matrix.setLookAtM(viewMatrix2, 0, 0, 0, 0, 0, 0, -1, 0, 10, 0);
        Matrix.multiplyMM(vPMatrix2, 0, projectionMatrix2, 0, viewMatrix2, 0);
        Matrix.setRotateM(rotationMatrix2, 0, 0, 1.0f, 1.0f, 1.0f);
        Matrix.multiplyMM(scratch2, 0, vPMatrix2, 0, rotationMatrix2, 0);

        //get all blocks in a 5 block radius around the player and draw them on screen using SlotRenderer class
        /*for (int x = (int) Math.floor(PacketUtils.x) - 5; x < (int) Math.floor(PacketUtils.x) + 5; x++) {
            for (int y = (int) Math.floor(PacketUtils.y) - 5; y < (int) Math.floor(PacketUtils.y) + 5; y++) {
                for (int z = (int) Math.floor(PacketUtils.z) - 5; z < (int) Math.floor(PacketUtils.z) + 5; z++) {
                    short block = ChunkColumn.getBlock(x, y, z);
                    if (block != 0) {
                        try {
                            new SlotRenderer(context, color, newSlot, x, y, z);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } copilot*/

        for (int dx = -15; dx < 15; dx++) {
            for (int dy = -45; dy < 45; dy++) {
                for (int dz = -15; dz < 15; dz++) {
                    short block = ChunkColumn.getBlock((int) PacketUtils.x + dx, (int) PacketUtils.y + dy, (int) PacketUtils.z + dz);
                    if (block != 0/* && (int) PacketUtils.x != 616*/) {
                        try {
                            //TODO fix incorrect model names and add variation support
                            Slot slot = new Slot((short) ChunkColumn.getBlockId(block), (byte) 1, (byte) 0, ChunkColumn.getBlockMetaData(block), null);
                            new SlotRenderer(context, color, slot.itemModel + ".json", (int) PacketUtils.x + dx, (int) PacketUtils.y + dy, (int) PacketUtils.z + dz);
                        } catch (IOException | JSONException e) {
                            System.out.println("crush " + ChunkColumn.getBlockId(block) + " " + ChunkColumn.getBlockMetaData(block));
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
            /*try {
                for (int i = 0; i < 256; i++) {
                    new SlotRenderer(context,color, newSlot,i/16,0,i%16);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }*/
        coords.forEach((k, v) -> {

            FloatBuffer colorBuffer = ByteBuffer.allocateDirect(v.size() * 48).order(ByteOrder.nativeOrder()).asFloatBuffer();
            FloatBuffer coordsBuffer = ByteBuffer.allocateDirect(v.size() * 36).order(ByteOrder.nativeOrder()).asFloatBuffer();
            FloatBuffer textureBuffer = ByteBuffer.allocateDirect(v.size() * 24).order(ByteOrder.nativeOrder()).asFloatBuffer();

            for (float[] b : v) {
                coordsBuffer.put(b);
            }
            coordsBuffer.position(0);

            for (float[] b : Objects.requireNonNull(textures.get(k))) {
                textureBuffer.put(b);
            }
            textureBuffer.position(0);

            for (float[] b : Objects.requireNonNull(colors.get(k))) {
                colorBuffer.put(b);
            }
            colorBuffer.position(0);

            GLES20.glEnableVertexAttribArray(YourRenderer.colorHandle);
            GLES20.glVertexAttribPointer(YourRenderer.colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
            //GLES20.glUniform4fv(YourRenderer.colorHandle, 1, new float[] {1.0f, 1.0f, 1.0f, 1.0f}, 0);

            GLES20.glEnableVertexAttribArray(YourRenderer.positionHandle);
            GLES20.glVertexAttribPointer(YourRenderer.positionHandle, 3, GLES20.GL_FLOAT, false, 0, coordsBuffer);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, k);
            //mCubeTextureCoordinates.position(0);
            GLES20.glEnableVertexAttribArray(YourRenderer.mTextureCoordinateHandle);
            GLES20.glVertexAttribPointer(YourRenderer.mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3 * v.size());
        });
        //System.out.println(System.currentTimeMillis()-timer + "ms");

        colors = new HashMap<>();
        coords = new HashMap<>();
        textures = new HashMap<>();


        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, scratch2, 0);
        //System.out.println("ratio: " + ratio);//0.625

        //tringul is a triangle with coords of 3 vertices of a triangle in 3d space and tringul2 is a color of each vertex and tringul3 is texture coords
        //complete the tringul to make a square
        float[] tringul = new float[] {
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
        FloatBuffer textureBuffer = ByteBuffer.allocateDirect(24 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(tringul3).put(tringul3).put(tringul3).position(0);

        GLES20.glEnableVertexAttribArray(YourRenderer.colorHandle);
        GLES20.glVertexAttribPointer(YourRenderer.colorHandle, 4, GLES20.GL_FLOAT, false, 0, tringulBuffer2);
        GLES20.glEnableVertexAttribArray(YourRenderer.positionHandle);
        GLES20.glVertexAttribPointer(YourRenderer.positionHandle, 3, GLES20.GL_FLOAT, false, 0, tringulBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, loadTexture(context, R.drawable.white_square));

        //WHY??
        GLES20.glEnableVertexAttribArray(YourRenderer.mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(YourRenderer.mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3 * 3);
    }
}
