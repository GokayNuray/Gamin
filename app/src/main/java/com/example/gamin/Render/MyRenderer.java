package com.example.gamin.Render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {
    Context context;
    float[] color = {1.0f, 1.0f, 1.0f, 1.0f};

    public String newSlot = "bedrock.json";
    public static boolean bitvar = false;
    public static Bitmap bitmap;

    public static int mProgram;
    public static int vPMatrixHandle;
    public static int positionHandle;
    public static int colorHandle;
    public static int mTextureUniformHandle;
    public static int mTextureCoordinateHandle;
    public MyRenderer(Context context) {
        this.context = context;
    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.66f,0.66f,0.66f,0.0f);
        String vertexShaderCode = "uniform mat4 u_MVPMatrix;" +
                "attribute vec4 a_Position;" +
                "uniform vec4 u_Color;" +
                "attribute vec2 a_TexCoordinate;" +
                "varying vec4 v_Color;" +
                "varying vec2 v_TexCoordinate;" +
                "void main()" +
                "{" +
                "v_Color = u_Color;" +
                "v_TexCoordinate = a_TexCoordinate;" +
                "gl_Position = u_MVPMatrix * a_Position;" +
                "}";
        int vertexShader = MyRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        String fragmentShaderCode = "precision mediump float;" +
                "uniform sampler2D u_Texture;" +
                "varying vec4 v_Color;" +
                "varying vec2 v_TexCoordinate;" +
                "void main()" +
                "{gl_FragColor = (v_Color * texture2D(u_Texture, v_TexCoordinate));" +
                "}";
        int fragShader = MyRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = MyRenderer.createAndLinkProgram(vertexShader,fragShader, new String[] {"a_Position","a_Color", "a_TexCoordinate"});


        vPMatrixHandle = GLES20.glGetUniformLocation(MyRenderer.mProgram, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(MyRenderer.mProgram, "a_Position");
        colorHandle = GLES20.glGetUniformLocation(MyRenderer.mProgram, "u_Color");
        mTextureUniformHandle = GLES20.glGetUniformLocation(MyRenderer.mProgram, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(MyRenderer.mProgram, "a_TexCoordinate");
    }

    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private int mWidth;
    private int mHeight;
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        mWidth = width;
        mHeight = height;
        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 4, 6);

    }

    private final float[] rotationMatrix = new float[16];
    @Override
    public void onDrawFrame(GL10 gl10) {
        float[] scratch = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        float angle=0;
        Matrix.setRotateM(rotationMatrix, 0, angle, 1.0f, 1.0f, 1.0f);


        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0);
            /*try {
                ByteBuffer pixelBuf = ByteBuffer.allocate(mWidth*mHeight*4).order(ByteOrder.LITTLE_ENDIAN);
                //BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/storage/emulated/0/deneme.png"));
                SlotRenderer slotRenderer = new SlotRenderer(context, color, newSlot,0,0,0);
                //slotRenderer.draw();
                GLES20.glReadPixels(0,0,mWidth,mHeight,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,pixelBuf);
                bitmap = Bitmap.createBitmap(mWidth,mHeight, Bitmap.Config.ARGB_8888);
                pixelBuf.rewind();
                bitmap.copyPixelsFromBuffer(pixelBuf);
                //bmp.compress(Bitmap.CompressFormat.PNG,90,bos);
                bitvar = true;
                //bos.close();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }*/
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader,shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
    public static int loadTexture(Context context,int resId) {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),resId,options);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0);
        bitmap.recycle();
        return textureHandle[0];
    }
    public static int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes)
    {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null)
            {
                final int size = attributes.length;
                for (int i = 0; i < size; i++)
                {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }
            GLES20.glLinkProgram(programHandle);
        }

        if (programHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }
}
