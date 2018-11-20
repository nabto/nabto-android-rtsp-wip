package com.nabto;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Created by ug on 01/11/2017.
 */
public class NabtoBaseActivity extends Activity implements AppSuspendListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Application androidApp = getApplication();
        NabtoApplication app = (NabtoApplication) androidApp;
        app.setSuspendListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        NabtoApplication app = (NabtoApplication)getApplication();
        if (app.wasInBackground()) {
            app.clearWasInBackground();
            onAppResumeFromBackground();
        }
    }

    protected void onAppResumeFromBackground() {}

    protected void onAppSuspend() {}

    @Override
    // AppSuspendListener
    public void onSuspend() {
        onAppSuspend();
    }
}
