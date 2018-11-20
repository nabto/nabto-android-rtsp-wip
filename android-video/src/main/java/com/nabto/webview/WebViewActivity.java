package com.nabto.webview;

import java.util.regex.Pattern;

import android.R.anim;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.ConsoleMessage;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.nabto.AddBookmarkActivity;
import com.nabto.AlertDialogFragment;
import com.nabto.BookmarksActivity;
import com.nabto.History;
import com.nabto.LoginActivity;
import com.nabto.NabtoApplication;
import com.nabto.api.NabtoClient;
import com.nabto.nabtovideo.R;
import com.nabto.qr.*;

@SuppressLint("SetJavaScriptEnabled")
public class WebViewActivity extends FragmentActivity {

    private NabtoClient nabtoClient;
    private WebView webView;
    private WebViewClient webViewClient;

    private String lastUrlAccessed;
    private long previousClickTimeStamp = 0;
    private long minimumClickInterval = 500;
    private boolean persistNabtoApi = false;

    private EditText urlInput;
    private ImageButton refreshButton;
    private ProgressBar progressIcon;
    private ProgressBar progressBar;
    private History history = new History();
    private Pattern startsWithNabtoOrHttp;

    private static final String LAST_URL_KEY = "LAST_URL";
    private static final String HOMEPAGE_URL_KEY = "HOMEPAGE";
    private static final String HOMEPAGE_SHARED_PREFERENCES = "HOMEPAGE_PREFERENCES";
    public static final String START_URL_KEY = "START_URL";
    
    private int getWebView() {
        return isNabtoBrowser() ? R.layout.nabtowebview : R.layout.webview;
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getWebView());
        webView = (WebView) findViewById(R.id.webview);
        progressIcon = (ProgressBar) findViewById(R.id.progressIcon);

        if (isNabtoBrowser()) {
        	this.getActionBar().setDisplayHomeAsUpEnabled(true);
        	
            urlInput = (EditText) findViewById(R.id.urlInput);
            progressBar = (ProgressBar) findViewById(R.id.progressBar);
            refreshButton = (ImageButton) findViewById(R.id.refreshButton);
            toggleUrlBar(false);
            
            // Allow zoom
            webView.getSettings().setSupportZoom(true);
            webView.getSettings().setBuiltInZoomControls(false);

            startsWithNabtoOrHttp = Pattern.compile("^(nabto|http)://.*",
                    Pattern.CASE_INSENSITIVE);
            
            urlInput.setOnEditorActionListener(new OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId,
                        KeyEvent event) {
                    if ((actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN)
                            || actionId == EditorInfo.IME_ACTION_NEXT)
                    {
                        String url = urlInput.getText().toString();

                        // Validate URL and alter it if not valid
                        if (!startsWithNabtoOrHttp.matcher(url).matches()) {
                            url = "nabto://" + url;
                        }

                        webView.requestFocus();
                        loadUrl(url, false);
                        return true;
                    }
                    return false;
                }
            });

            refreshButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    loadUrl(lastUrlAccessed, false);
                }
            });
        }

        lastUrlAccessed = getHomePage();
        
        // If LoginActivity specified what URL to open - get it here
        if (getIntent().hasExtra(START_URL_KEY)) {
            lastUrlAccessed = getIntent().getExtras().getString(START_URL_KEY);
        }
        
        NabtoApplication app = (NabtoApplication) getApplication();
        nabtoClient = app.getNabtoClient();

        Log.d(this.getClass().getSimpleName(), "Initializing web view settings");
        
        webViewClient = new NabtoWebViewClient(nabtoClient, this);
        webView.setWebViewClient(webViewClient);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setPluginState(PluginState.ON);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        
        //webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.getSettings().setRenderPriority(RenderPriority.HIGH);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setAppCacheEnabled(false);
        webView.clearCache(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.setWebChromeClient(new WebChromeClient() {
        	// Handle console.log (console.dir is not supported)
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.i("HTML_DD", cm.message() + " -- line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }
        });

        // Only nabtoBrowser has refresh buttons
        if (isNabtoBrowser()) {
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    refreshButton.setVisibility(newProgress == 100 ? View.VISIBLE : View.GONE);
                    progressBar.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);
                }
            });
        }
        
        CookieSyncManager.createInstance(getApplicationContext());
        
    	loadUrl(lastUrlAccessed, true);
    }

    public void loadUrl(String url, boolean appendKey) {
    	// Force lower case url
    	url = url.toLowerCase();
    	
    	if (appendKey) {
    		url = appendSessionToken(url);
    	}
    	
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    		// Make sure the url is RFC 3986 valid with ending slash
        	int paramIndex = url.indexOf("?");
        	if (paramIndex == -1 && !url.substring(8).contains("/")) {
        		url = url + "/";
        	}
        	else if (paramIndex != -1 && url.charAt(paramIndex-1) != '/') {
        		url = url.substring(0, paramIndex) + "/" + url.substring(paramIndex);
        	}
        }
    	Log.d(this.getClass().getSimpleName(), "LoadUrl: " + url);

    	webView.stopLoading();
        history.navigateTo(url);
        setLastUrlAccessed(url);
        webView.loadUrl(url);
    }

    protected void onPause() {
        Log.d(this.getClass().getSimpleName(), "Pausing");
        webView.pauseTimers();
        CookieSyncManager.getInstance().stopSync();

        // Do not shutdown when just opening a subview
        if (!persistNabtoApi) {
            nabtoClient.pause();
        }
        super.onPause();
    }
    
    protected void onResume() {
        super.onResume();

        if (!persistNabtoApi) {
            nabtoClient.resume();
        }
        persistNabtoApi = false;

        // Legacy HTML-DD, so no refreshing in Danfoss app
        if (!getPrefix().equals("clx://")) {
            loadUrl(lastUrlAccessed, true);
        }

        CookieSyncManager.getInstance().startSync();
        webView.resumeTimers();
        Log.d(this.getClass().getSimpleName(), "Resuming");
    }
    
    protected void onDestroy() {
        Log.d(this.getClass().getSimpleName(), "Destroy");
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        // Save last accessed URL
        outState.putString(LAST_URL_KEY, lastUrlAccessed);

        // Save history
        history.save(outState);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
    	super.onRestoreInstanceState(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show action bar in nabtoBrowser
        if (isNabtoBrowser()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.nabtowebview, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == android.R.id.home) {
    		loadUrl("nabto://self/discover", true);
    	}
    	else if (item.getItemId() == R.id.set_as_home_page) {
            setHomePage(lastUrlAccessed);
        }
    	else if (item.getItemId() == R.id.change_credentials) {
    		showLoginDialog(false, "");
        }
        else if (item.getItemId() == R.id.browser_back) {
        	if (history.canGoBack()) {
        		loadUrl(history.goBack(), false);
        	}
        }
        else if (item.getItemId() == R.id.browser_forward) {
        	if (history.canGoForward()) {
        		loadUrl(history.goForward(), false);
        	}
        }
        else if (item.getItemId() == R.id.bookmark_add) {
            webView.stopLoading();
            persistNabtoApi = true;
            Intent intent = new Intent(WebViewActivity.this,AddBookmarkActivity.class);
            intent.putExtra(AddBookmarkActivity.INPUT_TITLE,webView.getTitle());
            intent.putExtra(AddBookmarkActivity.INPUT_URL,lastUrlAccessed);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.bookmark_list) {
            webView.stopLoading();
            persistNabtoApi = true;
            Intent intent = new Intent(WebViewActivity.this, BookmarksActivity.class);
            startActivityForResult(intent, 0);
        }
        else if (item.getItemId() == R.id.toggle_url_bar) {
        	toggleUrlBar(true);
        }
        else if (item.getItemId() == R.id.qr_scan) {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.initiateScan();
        }
    	
    	return true;
    }
    
    private void toggleUrlBar(boolean toggle) {
    	SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferences), MODE_PRIVATE);
    	String state = sharedPreferences.getString(getString(R.string.sharedPreferences_showUrlBar), "1");
    	
    	if (toggle) {
    		Editor editor = sharedPreferences.edit();
    		state = state.equalsIgnoreCase("1")?"0":"1";
    		editor.putString(getString(R.string.sharedPreferences_showUrlBar), state);
        	editor.commit();
    	}
    	
    	LinearLayout urlBar = (LinearLayout) findViewById(R.id.linearLayout1);
    	urlBar.setVisibility(state.equalsIgnoreCase("1")?View.VISIBLE:View.GONE);
    }
    
    public void showLoginDialog(boolean autologin, String destination) {
        Intent loginActivity = new Intent(this, LoginActivity.class);
        loginActivity.putExtra("autologin", autologin);
        
        if (!destination.equals("")) {
        	if (!destination.contains("://")) {
        		destination = getPrefix() + destination;
        	}
        	loginActivity.setData(Uri.parse(destination));
        }

        startActivity(loginActivity);
        overridePendingTransition(anim.slide_in_left,anim.slide_out_right); 
        finish();
    }

    public void showAlertDialog(String title, String message) {
        DialogFragment newFragment = AlertDialogFragment.createInstance(title, message);
        newFragment.show(getSupportFragmentManager(), "dialog");
    }

    public void setLastUrlAccessed(String url) {
    	// Remove session key from history url if present
    	if (url.contains("session_key=")) {
    		lastUrlAccessed = url.substring(0, url.indexOf("session_key=") - 1);
    	}
    	else {
    		lastUrlAccessed = url;
    	}
    	
        history.navigateTo(lastUrlAccessed);

        if (isNabtoBrowser()) {
            urlInput.setText(url);
        }
    }

    public String getLastUrlAccessed() {
        return lastUrlAccessed;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            lastUrlAccessed = scanResult.getContents();
        }
        else if (resultCode == RESULT_OK) {
            lastUrlAccessed = intent.getStringExtra(BookmarksActivity.RESULT_NAME);
        }
    }

    private String getHomePage() {
        SharedPreferences preferences = getSharedPreferences(HOMEPAGE_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getString(HOMEPAGE_URL_KEY, getPrefix() + "self/show_home_page");
    }

    private void setHomePage(String newHomePage) {
        SharedPreferences preferences = getSharedPreferences(HOMEPAGE_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString(HOMEPAGE_URL_KEY, newHomePage);
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        long currentTimeStamp = SystemClock.uptimeMillis();

        if (webView.isFocused() && webView.canGoBack()) {
            webView.goBack();
        }
        
        // Avoid rapid back clicks out of the app
        else if (previousClickTimeStamp == 0 || (currentTimeStamp - previousClickTimeStamp > minimumClickInterval)) {
        	webView.stopLoading();
        	super.onBackPressed();
        }
        previousClickTimeStamp = currentTimeStamp;
    }
    
    private String appendSessionToken(String url) {
    	String token = nabtoClient.getSessionToken();
        if (token == null) {
        	Log.e(this.getClass().getSimpleName(), "Token is null");
        }
        Log.i(this.getClass().getSimpleName(), "Received token: " + token);
        
        if (url.contains("?")) {
        	return url + "&session_key=" + token;
        }
        return url + "?session_key=" + token;
    }
    
    private boolean isNabtoBrowser() {
    	return getString(R.string.app_name).equalsIgnoreCase("nabto");
    }
    
    public String getPrefix() {
    	String appName = getString(R.string.app_name);
    	
    	if (appName.equalsIgnoreCase("danfoss")) {
    		return "clx://";
    	}
    	else if (appName.equalsIgnoreCase("pps")) {
    		return "pre://";
    	}
    	return "nabto://";
    }
    
    public void showProgressIcon() {
    	if (isNabtoBrowser()) {
    		progressIcon.setVisibility(View.VISIBLE);
    	}
    }
    
    public void hideProgressIcon() {
    	if (isNabtoBrowser()) {
    		progressIcon.setVisibility(View.GONE);
    	}
    }
}