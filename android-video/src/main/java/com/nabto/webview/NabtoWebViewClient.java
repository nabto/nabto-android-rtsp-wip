package com.nabto.webview;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nabto.api.NabtoClient;
import com.nabto.api.NabtoStatus;
import com.nabto.api.UrlResult;

public class NabtoWebViewClient extends WebViewClient {
    private NabtoClient nabtoClient;
    private WebViewActivity activity;
    private static final String[] loginStrings = new String[] {"//self/show_login","//self/login/form","//self/logout"};

    private Collection<String> prefixes;
    
    private boolean checkNextRequest = false;
    
    public NabtoWebViewClient(NabtoClient nabtoClient, WebViewActivity activity) {

        if (activity == null)
            throw new IllegalArgumentException();

        if (nabtoClient == null)
            throw new IllegalArgumentException();

        this.activity = activity;
        this.nabtoClient = nabtoClient;
        this.prefixes = nabtoClient.getProtocolPrefixes();
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // Some requests does not go trough shouldOverrideUrlLoading
    	
        if (!url.startsWith("http://localhost:") && !activity.getLastUrlAccessed().equals(url)) {
            activity.setLastUrlAccessed(url.replaceAll("\\/\\/+", "//"));
        }
    }

    private boolean urlStartsWithKnownPrefix(String url) {
    	for (String prefix : this.prefixes) {
            if (url.startsWith(prefix + "://")) {
                return true;
            }
        }
        return false;
    }
    
    private boolean urlLoginShow(String url) {
        for (String loginString : loginStrings) {
	        if (url.contains(loginString)) {
	            return true;
	        }
        }
        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
    	// Remove extra slashes introduced in webView 4.4 because of wrong window.location
    	url = url.replaceAll("\\/\\/+", "//");
    	
    	// Add trailing slash in set home page in Danfoss App
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
    			activity.getPrefix().equals("clx://") &&
    			url.contains("set_home_page?")) {
        	int paramIndex = url.indexOf("&callback");
        	url = url.substring(0, paramIndex) + "/" + url.substring(paramIndex);
        	checkNextRequest = true;
        }
    	
        Log.d(this.getClass().getSimpleName(), "Handling: " + url);
        
        if (urlStartsWithKnownPrefix(url)) {
            UrlResult fetchRes = nabtoClient.fetchUrl(url);
            if (fetchRes.getStatus() != NabtoStatus.OK) {
                Log.d(this.getClass().getSimpleName(), "FetchUrl failed " + url);
            } else {
                String charset = "utf-8";
                String mt = fetchRes.getMimeType();
                if (mt.contains(";")) {
                    mt = mt.substring(0, mt.indexOf(";"));
                }
                WebResourceResponse res = new WebResourceResponse(mt, charset,
                        new ByteArrayInputStream(fetchRes.getResult()));
                return res;
            }
        }
        return null;
    }

    public String nullOrStringToString(Object a) {
        if (a == null)
            return "";
        return a.toString();
    }
    
    @Override
    public void onLoadResource(WebView view, String url) {
        Log.d(this.getClass().getSimpleName(), "Loading: " + url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
    	Log.d(this.getClass().getSimpleName(), "ShouldOverrideUrlLoading called with url: " + url);
        
        // Magic url: show login dialog
        if (urlLoginShow(url)) {
        	Log.d(this.getClass().getSimpleName(), "Redirecting to Login because url is: " + url);
        	
        	if (url.contains("?url=")) {
            	activity.showLoginDialog(false, url.substring(url.indexOf("?url=") + 5));
        	}
        	else {
            	activity.showLoginDialog(false, "");
        	}
        	return true;
        }
        else if (urlStartsWithKnownPrefix(url)) {
        	Log.d(this.getClass().getSimpleName(), "Using known prefix: " + activity.getPrefix());
        	
        	// Fix for set home page in Danfoss App. The redirected url was missing a traling slash.
        	if (checkNextRequest) {
            	if (url.indexOf("?") == -1 && !url.substring(8).contains("/")) {
            		checkNextRequest = false;
            		activity.loadUrl(url, true);
            		return true;
            	}
            }
        	
            return false;
        }

        // Using non-nabto url
        return false;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode,
        String description, String failingUrl) {
    	
    	Log.e(this.getClass().getSimpleName(), "Webview error: " + errorCode + " : " + description);
    	
    	// Handle error code -10: The protocol is not supported
    	if (errorCode == -10) {
    		// TODO: Create a more discreet way to login again
    		activity.showLoginDialog(true, "");
    	}
    	else {
            activity.showAlertDialog("WebView error received", description + "\nURL: " + failingUrl + "\nErrorcode: " + errorCode);    		
    	}
    }
}
