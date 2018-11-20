package com.nabto.webview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class ZoomableWebView extends WebView {
    // Kudos to
    // http://stackoverflow.com/questions/5125851/enable-disable-zoom-in-android-webview
    // for allowing zoom

    private static final boolean multiTouchZoom = true;
    private static final boolean buttonsZoom = false;

    public ZoomableWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ZoomableWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("deprecation")
	@Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN
                || ev.getAction() == MotionEvent.ACTION_POINTER_DOWN
                || ev.getAction() == MotionEvent.ACTION_POINTER_1_DOWN
                || ev.getAction() == MotionEvent.ACTION_POINTER_2_DOWN
                || ev.getAction() == MotionEvent.ACTION_POINTER_3_DOWN) {
            if (multiTouchZoom && !buttonsZoom) {
                if (ev.getPointerCount() > 1) {
                    getSettings().setBuiltInZoomControls(true);
                    getSettings().setSupportZoom(true);
                } else {
                    getSettings().setBuiltInZoomControls(false);
                    getSettings().setSupportZoom(false);
                }
            }
        }

        /*
         * if (!multiTouchZoom && buttonsZoom) { if (ev.getPointerCount() > 1) {
         * return true; } }
         */

        return super.onTouchEvent(ev);
    }
}