package com.example.gamin.Render;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Square {
    public int mTextureDataHandle;
    float[] color;
    float[] squareCoords;
    float[] textureCoords;
    int resId;
    Context context;
    private static final Map<Integer, Integer> textureMap = new HashMap<>();
    public Square(Context context, float[] squareCoords, float[] color, float[] textureCoords,int resId) {
        this.color = color;
        this.squareCoords = squareCoords;
        this.textureCoords = textureCoords;
        this.resId = resId;
        this.context = context;
    }
    public void render() {

        float[] squareColors = new float[24];
        for (int i = 0; i < 6; i++) {
            System.arraycopy(color, 0, squareColors, i*4, 4);
        }

        float[] coords1 = Arrays.copyOfRange(squareCoords,0,9);
        float[] coords2 = new float[9];
        System.arraycopy(Arrays.copyOfRange(squareCoords,6,12),0,coords2,0,6);
        System.arraycopy(Arrays.copyOfRange(squareCoords,0,3),0,coords2,6,3);

        float[] textures1 = Arrays.copyOfRange(textureCoords,0,6);
        float[] textures2 = new float[6];
        System.arraycopy(Arrays.copyOfRange(textureCoords,4,8),0,textures2,0,4);
        System.arraycopy(Arrays.copyOfRange(textureCoords,0,2),0,textures2,4,2);


        if (textureMap.containsKey(resId)) {
            //noinspection DataFlowIssue
            mTextureDataHandle = textureMap.get(resId);
        }
        else {
            mTextureDataHandle = YourRenderer.loadTexture(context,resId);
            textureMap.put(resId,mTextureDataHandle);
        }

        List<float[]> list;
        List<float[]> list2;
        List<float[]> list3;

        if (YourRenderer.textures.containsKey(mTextureDataHandle)) {
            list = YourRenderer.coords.get(mTextureDataHandle);
            System.out.println();
            assert list != null;
            list.add(coords1);
            list.add(coords2);

            list2 = YourRenderer.textures.get(mTextureDataHandle);
            assert list2 != null;
            list2.add(textures1);
            list2.add(textures2);

            list3 = YourRenderer.colors.get(mTextureDataHandle);
            assert list3 != null;
            list3.add(squareColors);
        }
        else {
            list = new ArrayList<>();
            list.add(coords1);
            list.add(coords2);
            YourRenderer.coords.put(mTextureDataHandle, list);

            list2 = new ArrayList<>();
            list2.add(textures1);
            list2.add(textures2);
            YourRenderer.textures.put(mTextureDataHandle, list2);

            list3 = new ArrayList<>();
            list3.add(squareColors);
            YourRenderer.colors.put(mTextureDataHandle, list3);
        }
    }
}
