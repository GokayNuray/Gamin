package com.example.gamin.Utils;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.example.gamin.Render.MyRenderer;
import com.example.gamin.Render.YourRenderer;

public final class InventoryUtils {


    public static void drawButton(GLSurfaceView glSurfaceView, YourRenderer renderer, String name, ImageButton button) {

        renderer.newSlot = name + ".json";
        glSurfaceView.requestRender();


        while (!MyRenderer.bitvar) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        button.setImageDrawable(new BitmapDrawable(button.getContext().getResources(), MyRenderer.bitmap));
        MyRenderer.bitvar = false;
        button.setBackgroundColor(Color.RED);

    }

    public static void resizeInventory(View view, int i) {


        ViewGroup.LayoutParams params = view.getLayoutParams();

        if (i == 0) {
            params.height = 0;
        } else {
            params.height = view.getContext().getResources().getDimensionPixelSize(i);
        }
        view.setLayoutParams(params);
    }
}
