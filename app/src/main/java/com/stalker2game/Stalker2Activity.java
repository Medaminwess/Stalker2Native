package com.stalker2game;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.microemu.android.MicroEmulatorActivity;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.DisplayAccess;

public class Stalker2Activity extends MicroEmulatorActivity {

    private NativeGamepad gamepad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        startGame();
    }

    private void startGame() {
        new Thread(() -> {
            try {
                File jar = new File(getFilesDir(), "game.jar");
                if (!jar.exists()) {
                    try (InputStream in = getAssets().open("game.jar");
                         FileOutputStream out = new FileOutputStream(jar)) {
                        byte[] b = new byte[8192]; int n;
                        while ((n = in.read(b)) > 0) out.write(b, 0, n);
                    }
                }
                openMidlet(jar.toURI().toURL());
                // Attach gamepad after MIDlet loads
                Thread.sleep(2000);
                runOnUiThread(this::attachGamepad);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: "+e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void attachGamepad() {
        gamepad = new NativeGamepad(this);
        FrameLayout root = (FrameLayout) getWindow().getDecorView();
        root.addView(gamepad, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        // Wire to canvas
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma != null) {
            DisplayAccess da = ma.getDisplayAccess();
            if (da != null) {
                Displayable d = da.getCurrent();
                if (d instanceof Canvas) gamepad.setCanvas((Canvas) d);
            }
        }
    }
}
