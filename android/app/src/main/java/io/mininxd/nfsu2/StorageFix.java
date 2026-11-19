package io.mininxd.nfsu2;

import android.util.Log;
import java.io.File;

/**
 * Points the game process at the player's game folder. The prebuilt native runtime reads its data
 * relative to the working directory (see scripts/patch_game_root.py), so any folder works.
 */
final class StorageFix {
    private static boolean loaded;

    private StorageFix() {}

    private static native boolean nativeChdir(String path);

    /** Must run before the native activity starts. Returns false when the folder is missing or the helper is absent. */
    static boolean enter(File gameRoot) {
        try {
            if (!loaded) { System.loadLibrary("nfsu2_pathfix"); loaded = true; }
            boolean ok = nativeChdir(gameRoot.getAbsolutePath());
            if (!ok) Log.w("NFSU2", "chdir failed for " + gameRoot);
            return ok;
        } catch (Throwable t) {
            Log.w("NFSU2", "Game folder helper unavailable: " + t.getMessage());
            return false;
        }
    }
}
