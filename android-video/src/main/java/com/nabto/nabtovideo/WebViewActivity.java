package com.nabto.nabtovideo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nabto.nabtovideo.util.VideoDevice;
import com.nabto.nabtovideo.R;

@SuppressLint("SetJavaScriptEnabled")
public class WebViewActivity extends Activity {
	
	private WebView webView;
    private ProgressDialog progress;
    private Activity mContext;
    private String url;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_view);

        mContext = this;

        progress = new ProgressDialog(this);
        progress.setMessage("Loading...");
        progress.show();
		
		webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDisplayZoomControls(true);

        webView.setWebViewClient(new WebViewClient() {
        	@Override
        	public void onLoadResource(WebView view, String url) {
				Log.d(this.getClass().getSimpleName(), "WebView onLoadRessource: " + url);
        	}
        	
			@Override
			public void onPageFinished(WebView view, String url) {
				Log.d(this.getClass().getSimpleName(), "WebView onPageFinished: " + url);
                progress.dismiss();
			}

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(this.getClass().getSimpleName(), "WebView onReceivedError: " + description);
            }

            @Override
            public void onReceivedHttpAuthRequest (WebView view, HttpAuthHandler handler, String host, String realm) {
                Log.d(this.getClass().getSimpleName(), "WebView onReceivedHttpAuthRequest");

                showHttpAuthDialog(handler, host, realm, null, null, null);
            }
		});
		
		Intent intent = getIntent();
		url = intent.getStringExtra(VideoDevice.DEVICEURL);
        webView.loadUrl(url);
	}

    private void showHttpAuthDialog( final HttpAuthHandler handler, final String host, final String realm, final String title, final String name, final String password ) {
        LinearLayout llayout = new LinearLayout(mContext);
        final TextView textview1 = new TextView(mContext);
        final EditText edittext1 = new EditText(mContext);
        final TextView textview2 = new TextView(mContext);
        final EditText edittext2 = new EditText(mContext);

        textview1.setText(R.string.http_username);
        textview2.setText(R.string.http_password);
        edittext1.setSingleLine();
        edittext2.setSingleLine();

        llayout.setOrientation(LinearLayout.VERTICAL);
        llayout.addView(textview1);
        llayout.addView(edittext1);
        llayout.addView(textview2);
        llayout.addView(edittext2);

        final AlertDialog.Builder mHttpAuthDialog = new AlertDialog.Builder(mContext);
        mHttpAuthDialog.setTitle(R.string.http_auth)
            .setView(llayout)
            .setCancelable(false)
            .setPositiveButton(R.string.http_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String userName = edittext1.getText().toString();
                    String userPass = edittext2.getText().toString();

                    webView.setHttpAuthUsernamePassword(host, realm, name, password);
                    progress.show();
                    handler.proceed(userName, userPass);
                }
            })
            .setNegativeButton(R.string.http_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    handler.cancel();
                    progress.show();
                    webView.loadUrl(url);
                }
            })
            .create().show();
    }
	
	@Override
	public void onBackPressed() {
	    if (webView.canGoBack()) {
	        webView.goBack();
	        return;
	    }

	    super.onBackPressed();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == android.R.id.home) {
    		finish();
    	}
    	return true;
    }
}
