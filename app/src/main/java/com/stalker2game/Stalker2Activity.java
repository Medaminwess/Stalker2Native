package com.stalker2game;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;

import org.microemu.android.MicroEmulatorActivity;
import org.microemu.android.device.*;
import org.microemu.android.util.*;
import org.microemu.app.Common;
import org.microemu.EmulatorContext;
import org.microemu.DisplayComponent;
import org.microemu.device.DeviceDisplay;
import org.microemu.device.FontManager;
import org.microemu.device.InputMethod;
import org.microemu.log.Logger;

public class Stalker2Activity extends MicroEmulatorActivity {

    private Common common;
    private NativeGamepad gamepad;

    private final EmulatorContext emulatorContext = new EmulatorContext() {
        private final InputMethod   im = new AndroidInputMethod();
        private final DeviceDisplay dd = new AndroidDeviceDisplay(this);
        private final FontManager   fm = new AndroidFontManager();
        public DisplayComponent getDisplayComponent()  { return null; }
        public InputMethod      getDeviceInputMethod() { return im; }
        public DeviceDisplay    getDeviceDisplay()     { return dd; }
        public FontManager      getDeviceFontManager() { return fm; }
        public InputStream getResourceAsStream(String name) {
            try { return getAssets().open(name.startsWith("/") ? name.substring(1) : name); }
            catch (IOException e) { return null; }
        }
        public boolean platformRequest(String url) { return false; }
    };

    @Override
    public void onCreate(Bundle icicle) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(icicle);
        new Thread(this::launchGame).start();
    }

    private void launchGame() {
        try {
            // Extract game JAR from assets to internal storage
            File jar = new File(getFilesDir(), "game.jar");
            if (!jar.exists()) {
                try (InputStream in  = getAssets().open("game.jar");
                     OutputStream out = new FileOutputStream(jar)) {
                    byte[] buf = new byte[8192]; int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
            }

            // Setup logging
            Logger.removeAllAppenders();
            Logger.setLocationEnabled(false);
            Logger.addAppender(new AndroidLoggerAppender());

            // Setup display size
            android.view.Display disp = getWindowManager().getDefaultDisplay();
            AndroidDeviceDisplay dd = (AndroidDeviceDisplay) emulatorContext.getDeviceDisplay();
            dd.displayRectangleWidth  = disp.getWidth();
            dd.displayRectangleHeight = disp.getHeight() - 25;

            // Boot MicroEmulator
            common = new Common(emulatorContext);
            common.setRecordStoreManager(new AndroidRecordStoreManager(this));
            common.setDevice(new AndroidDevice(emulatorContext, this));

            // Launch game from JAR
            Common.openMIDletUrlSafe(jar.toURI().toString());

            // Attach gamepad after MIDlet starts
            Thread.sleep(3000);
            runOnUiThread(this::attachGamepad);

        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this,
                "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void attachGamepad() {
        gamepad = new NativeGamepad(this);
        FrameLayout root = (FrameLayout) getWindow().getDecorView();
        root.addView(gamepad, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
    }

    @Override protected void onPause() {
        super.onPause();
        org.microemu.MIDletAccess ma = org.microemu.MIDletBridge.getMIDletAccess();
        if (ma != null) ma.pauseApp();
    }
    @Override protected void onResume() {
        super.onResume();
        org.microemu.MIDletAccess ma = org.microemu.MIDletBridge.getMIDletAccess();
        if (ma != null) try { ma.startApp(); } catch (Exception ignored) {}
    }
}
