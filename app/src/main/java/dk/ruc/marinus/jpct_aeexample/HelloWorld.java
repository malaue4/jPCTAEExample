package dk.ruc.marinus.jpct_aeexample;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

/**
 * @author Darai
 *
 */
public class HelloWorld extends Activity {

    private static HelloWorld master = null;

    private GLSurfaceView mGLView;
    private MyRenderer renderer = null;
    private FrameBuffer fb = null;
    private World world = null;

    private int fps = 0;

    private Light sun = null;

    private GL10 lastGl = null;

    private RGBColor back = new RGBColor(0, 0, 0);


    private Random rand = new Random();
    public static int countSquares = 0;
    public Object3D obj;
    public Object3D obj2;
    public Texture texture;

    float startX, startY;
    private Object3D android;

    protected void onCreate(Bundle savedInstanceState) {

        Logger.log("onCreate");

        if (master != null) {
            copy(master);
        }

        super.onCreate(savedInstanceState);

        mGLView = new GLSurfaceView(getApplication());
        mGLView.setEGLContextClientVersion(2);
        mGLView.setPreserveEGLContextOnPause(true);

        renderer = new MyRenderer();
        mGLView.setRenderer(renderer);
        ConstraintLayout cl = new ConstraintLayout(this);
        cl.addView(mGLView);
        Button button = new Button(this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderer.addAndroid();
                view.setClickable(false);
            }
        });
        cl.addView(button);
        setContentView(cl);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.exit(0);
    }

    private void copy(Object src) {
        try {
            Logger.log("Copying data from master Activity!");
            Field[] fs = src.getClass().getDeclaredFields();
            for (Field f : fs) {
                f.setAccessible(true);
                f.set(this, f.get(src));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean onTouchEvent(MotionEvent me) {

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            startX = me.getRawX();
            startY = me.getRawY();
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_UP) {
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_MOVE) {
            float relativeX=me.getRawX()-startX;
            float relativeY=me.getRawY()-startY;
            startX = me.getRawX();
            startY = me.getRawY();
            float rotY = relativeX/fb.getWidth();
            obj.rotateY(rotY);

            return true;
        }

        try {
            Thread.sleep(15);
        } catch (Exception e) {
            // No need for this...
        }

        return super.onTouchEvent(me);
    }

    class MyRenderer implements GLSurfaceView.Renderer {

        private long time = System.currentTimeMillis();

        public MyRenderer() {
        }

        public void onSurfaceChanged(GL10 gl, int w, int h) {

            // Renew the frame buffer
            if (lastGl != gl) {
                Log.i("HelloWorld", "Init buffer");
                if (fb != null) {
                    fb.dispose();
                }
                fb = new FrameBuffer(w, h);
                fb.setVirtualDimensions(fb.getWidth(), fb.getHeight());
                lastGl = gl;
            } else {
                fb.resize(w, h);
                fb.setVirtualDimensions(w, h);
            }

            // Create the world if not yet created
            if (master == null) {
                world = new World();
                world.setAmbientLight(20, 20, 20);

                sun = new Light(world);
                sun.setIntensity(250, 250, 250);

                // Create the texture we will use in the blitting
                texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.icon)), 256, 256));
                TextureManager.getInstance().addTexture("texture", texture);

                // Create the object
                obj = Primitives.getPlane(1, 20f);
                world.addObject(obj);
                obj.translate(0, 0, 4);
                obj.setTexture("texture");
                obj.build();


/*
                obj2 = Primitives.getSphere(32, 3f);
                world.addObject(obj2);
                obj2.translate(0, 0, 0);
                obj2.calcTextureWrapSpherical();
                obj2.setTexture("texture");
                obj2.build();
*/
                Camera cam = world.getCamera();
                cam.moveCamera(Camera.CAMERA_MOVEOUT, 15);
                cam.lookAt(SimpleVector.ORIGIN);

                SimpleVector sv = new SimpleVector();
                sv.set(SimpleVector.ORIGIN);
                sv.y -= 100;
                sv.z -= 100;
                sun.setPosition(sv);
                MemoryHelper.compact();

                if (master == null) {
                    Logger.log("Saving master Activity!");
                    master = HelloWorld.this;
                }
            }
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        }

        public void onDrawFrame(GL10 gl) {

            // Draw the main screen
            fb.clear(back);
            world.renderScene(fb);
            world.draw(fb);
            fb.display();

            obj.rotateZ(0.01f);

            if (System.currentTimeMillis() - time >= 1000) {
                Logger.log(fps + "fps");
                fps = 0;
                time = System.currentTimeMillis();
            }
            fps++;
        }

        public void addAndroid(){
            try {
                InputStream is = getResources().getAssets().open("android.3ds");
                Object3D[] model = Loader.load3DS(is, 3);
                android = Object3D.mergeAll(model);
                android.build();
            } catch (IOException e) {
                e.printStackTrace();
            }
            android.rotateX((float) (-Math.PI/2));
            world.addObject(android);
        }
    }

    public static HelloWorld getApp() {
        return master;
    }
}
