package com.stalker2game;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import dalvik.system.DexClassLoader;
import java.io.*;
import java.util.zip.*;
import org.microemu.android.MicroEmulatorActivity;
import org.microemu.android.device.*;
import org.microemu.android.util.*;
import org.microemu.app.Common;
import org.microemu.EmulatorContext;
import org.microemu.DisplayComponent;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.DisplayAccess;
import org.microemu.device.DeviceDisplay;
import org.microemu.device.FontManager;
import org.microemu.device.InputMethod;
import org.microemu.log.Logger;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

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
            File jar = extractAsset("game.jar");
            File dex = extractAsset("game.dex");

            Logger.removeAllAppenders();
            Logger.setLocationEnabled(false);
            Logger.addAppender(new AndroidLoggerAppender());

            android.view.Display disp = getWindowManager().getDefaultDisplay();
            AndroidDeviceDisplay dd = (AndroidDeviceDisplay) emulatorContext.getDeviceDisplay();
            dd.displayRectangleWidth  = disp.getWidth();
            dd.displayRectangleHeight = disp.getHeight() - 25;

            common = new Common(emulatorContext);
            common.setRecordStoreManager(new AndroidRecordStoreManager(this));
            common.setDevice(new AndroidDevice(emulatorContext, this));

            // Load game using Android's DexClassLoader (proper DEX format)
            File dexOpt = new File(getCacheDir(), "dex-opt");
            dexOpt.mkdirs();
           DexClassLoader loader = new DexClassLoader(
    dex.getAbsolutePath() + ":" + jar.getAbsolutePath(),
    dexOpt.getAbsolutePath(),
    null,
    getClassLoader()
);

            String midletClass = getMidletClass(jar);
            if (midletClass == null) midletClass = "Container.Stalker_2";

            Class<?> clazz = loader.loadClass(midletClass);
            MIDlet midlet  = (MIDlet) clazz.newInstance();
            MIDletBridge.setCurrentMIDlet(midlet);
            MIDletBridge.getMIDletAccess().startApp();

            Thread.sleep(2500);
            runOnUiThread(this::attachGamepad);
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this,
                "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private File extractAsset(String name) throws IOException {
        File out = new File(getFilesDir(), name);
        if (!out.exists()) {
            try (InputStream in = getAssets().open(name);
                 OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            }
        }
        return out;
    }

    private String getMidletClass(File jarFile) {
        try (ZipFile zf = new ZipFile(jarFile)) {
            ZipEntry mf = zf.getEntry("META-INF/MANIFEST.MF");
            if (mf == null) return null;
            BufferedReader br = new BufferedReader(
                new InputStreamReader(zf.getInputStream(mf)));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MIDlet-1:")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3) return parts[2].trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
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
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma != null) ma.pauseApp();
    }
    @Override protected void onResume() {
        super.onResume();
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma != null) try { ma.startApp(); } catch (Exception ignored) {}
    }
}
