package com.example.gamin.Render;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.example.gamin.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OpenGLUtils {
    static int loadShader(int type, String shaderCode) {
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

    static int loadTexture(Context context, int resId) {
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

    static int loadTexture(Bitmap bitmap) {
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

    static int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
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
}
