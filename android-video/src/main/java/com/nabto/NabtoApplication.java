package com.nabto;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;

import com.nabto.api.NabtoClient;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class MemoryBoss implements ComponentCallbacks2 {
    private boolean wasInBackground;
    private AppSuspendListener suspendListener;

    public boolean wasInBackground() {
        return wasInBackground;
    }

    public void clearBackgroundFlag() {
        wasInBackground = false;
    }

    @Override
    public void onTrimMemory(final int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            if (suspendListener != null) {
                suspendListener.onSuspend();
            }
            wasInBackground = true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    public void setSuspendListener(AppSuspendListener suspendListener) {
        this.suspendListener = suspendListener;
    }
}

public class NabtoApplication extends android.app.Application {
    private NabtoClient nabtoClient;

    private MemoryBoss memoryBoss;

    public void setSuspendListener(AppSuspendListener listener) {
        memoryBoss.setSuspendListener(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            memoryBoss = new MemoryBoss();
            registerComponentCallbacks(memoryBoss);
        }
        nabtoClient = new NabtoClient(this);
    }

    public NabtoClient getNabtoClient() {
        return nabtoClient;
    }

    public boolean wasInBackground() {
        return memoryBoss.wasInBackground();
    }

    public void clearWasInBackground() {
        memoryBoss.clearBackgroundFlag();
    }

}