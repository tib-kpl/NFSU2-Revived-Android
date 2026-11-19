package io.mininxd.nfsu2;

import android.app.Application;

/** Runs in every process (launcher and :game), so both see the chosen game storage. */
public final class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        GameOptions.loadStorage(this);
    }
}
