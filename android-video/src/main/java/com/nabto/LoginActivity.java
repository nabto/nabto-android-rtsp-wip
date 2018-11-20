package com.nabto;

import java.util.List;
import java.util.regex.Pattern;

import android.R.anim;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.nabto.api.NabtoClient;
import com.nabto.nabtovideo.R;
import com.nabto.webview.WebViewActivity;

public class LoginActivity extends FragmentActivity implements OnKeyListener {

	// Same pattern as http://developer.android.com/reference/android/util/Patterns.html#EMAIL_ADDRESS
    public static final Pattern EMAIL_ADDRESS = Pattern
            .compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@"
                    + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\."
                    + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+"); 
    private EditText emailField;
    private EditText passwordField;
    private CheckBox autologinBox;
    
    private SharedPreferences sharedPreferences;
    
    private NabtoClient api;
    private AsyncLoginTask asyncLoginTask = null;
    private AsyncSignupTask asyncSignupTask;
    private AsyncResetAccountPasswordTask asyncResetAccountPasswordTask;
    private String startAddress;
    
    private String guestEmail = "guest";
    private String guestPass = "123456";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        NabtoApplication app = (NabtoApplication) getApplication();
        api = app.getNabtoClient();

        sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferences), MODE_PRIVATE);
        
        // Auto login if set by the user and not comming from WebView
        if (!getIntent().hasExtra("autologin") && sharedPreferences.getString(getString(R.string.sharedPreferences_autologin), "").equals("true")) {
        	String email = sharedPreferences.getString(getString(R.string.sharedPreferences_Email), "");
        	String pass = sharedPreferences.getString(getString(R.string.sharedPreferences_Password), "");
        	
        	login(false, email, pass);
        }
        
        emailField = (EditText) findViewById(R.id.emailField);
        passwordField = (EditText) findViewById(R.id.passwordField);
        autologinBox = (CheckBox) findViewById(R.id.autoCheckBox);
        
        passwordField.setOnKeyListener(this);
        
        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                login(true, getEmail(), getPassword());
            }
        });
        
        // Nabto App specific tasks
    	Button guestButton = (Button) findViewById(R.id.guestButton);
        if (getString(R.string.app_name).equalsIgnoreCase("nabto")) {
        	guestButton.setOnClickListener(new OnClickListener() {
        		public void onClick(View view) {
        			login(true, guestEmail, guestPass);
        		}
        	});
        	
        	// Default bookmarks if bookmark list is empty
        	List<Bookmark> bookmarks = Bookmarks.getBookmarks(this);
        	if (bookmarks.size() == 0) {
                Bookmarks.addBookmark(new Bookmark("Nabduino Demo", "nabto://demo1.nabduino.net"), this);
                Bookmarks.addBookmark(new Bookmark("Nabto Weather Station", "nabto://weather.u.nabto.net"), this);
        	}
        }
        else {
        	guestButton.setVisibility(View.GONE);
        }

        Button createButton = (Button) findViewById(R.id.createButton);
        createButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                String email = getEmail();
                String password = getPassword();

                if (!checkValidInput(true, email, password))
                    return;

                asyncSignupTask = new AsyncSignupTask(LoginActivity.this, email, password, api);
                asyncSignupTask.execute();
            }
        });

        Button forgotButton = (Button) findViewById(R.id.forgotButton);
        forgotButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                String email = getEmail();
                if (!checkValidInput(true, email, "someVeryValidPassword"))
                    return;

                asyncResetAccountPasswordTask = new AsyncResetAccountPasswordTask(
                        LoginActivity.this, email, api);
                asyncResetAccountPasswordTask.execute();
            }
        });

        emailField.setText(sharedPreferences.getString(getString(R.string.sharedPreferences_Email), ""));
        passwordField.setText(sharedPreferences.getString(getString(R.string.sharedPreferences_Password), ""));
        
        autologinBox.setChecked(sharedPreferences.getString(getString(R.string.sharedPreferences_autologin), "").equals("true"));
        
        // Check if we were opened because someone clicked a URL we listen on
        Intent callerIntent = getIntent();
        Uri data = callerIntent.getData();
        if (data != null) {
            startAddress = data.toString();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
   
        // Check if the saved login credentials are valid. If so, login immediately
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean(getString(R.string.sharedPreferences_autologin))) {
        	login(false, getEmail(), getPassword());
    	}
    }

    @Override
    protected void onPause() {
        if (asyncLoginTask != null)
            asyncLoginTask.cancel(true);

        if (asyncSignupTask != null)
            asyncSignupTask.cancel(true);

        if (asyncResetAccountPasswordTask != null)
            asyncResetAccountPasswordTask.cancel(true);

        super.onPause();
    }

    private boolean checkValidInput(boolean showError, String email,
            String password) {
        if (email == null || email.length() == 0) {
            if (showError)
                showAlertDialog("Email required",
                        "You need to type in your email.");
            return false;
        }

        if (!isValidEmail(email)) {
            if (showError)
                showAlertDialog("Invalid email",
                        "You need to type in a valid email address.");
            return false;
        }

        if (password == null || password.length() == 0) {
            if (showError)
                showAlertDialog("Password required",
                        "You need to type in your password.");
            return false;
        }
		if (password.length() < 6) {
			if (showError)
				showAlertDialog("Password too short",
						"Your password must be at least 6 characters long.");
			return false;
		}
        return true;
    }

    private void login(boolean showError, String email, String password) {
        if (!email.equals(guestEmail) && !checkValidInput(showError, email, password))
            return;

        Log.d(this.getClass().getSimpleName(), "Logging in...");
		asyncLoginTask = new AsyncLoginTask(this, email, password, getString(R.string.login_text), api);
		asyncLoginTask.execute();
    }

    public void startWebView(String email, String password) {
		// Save login details to shared preferences, for later retrieval - not for guest
    	if (!email.equalsIgnoreCase(guestEmail)) {
    		SharedPreferences shared = getSharedPreferences(
                getString(R.string.sharedPreferences), MODE_PRIVATE);
        	Editor editor = shared.edit();
        	editor.putString(getString(R.string.sharedPreferences_Email), email);
        	editor.putString(getString(R.string.sharedPreferences_Password), password);
        
        	// Set shared preference according to Auto Login button
        	if (autologinBox.isChecked()) {
        		editor.putString(getString(R.string.sharedPreferences_autologin), "true");
        	} else {
        		editor.putString(getString(R.string.sharedPreferences_autologin), "false");
        	}

        	if (!editor.commit()) {
        		throw new IllegalStateException("Commit to shared preferences failed");
        	}
    	}

        Intent intent = new Intent(this, WebViewActivity.class);
        // If startAddress was specified, send it along to the intent
        if (startAddress != null)
            intent.putExtra(WebViewActivity.START_URL_KEY, startAddress);

        startActivity(intent);
        overridePendingTransition(anim.slide_in_left,anim.slide_out_right); 
    }

    public void showAlertDialog(String title, String message) {
        DialogFragment newFragment = AlertDialogFragment.createInstance(title,
                message);
        newFragment.show(getSupportFragmentManager(), "dialog");
    }

    private static boolean isValidEmail(String email) {
        return email != null && EMAIL_ADDRESS.matcher(email).matches();
    }

    public String getEmail() {
        return emailField.getText().toString();
    }

    public String getPassword() {
        return passwordField.getText().toString();
    }

    // Hardware or software Enter will execute login
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (keyCode == EditorInfo.IME_ACTION_DONE ||
				event.getAction() == KeyEvent.ACTION_DOWN &&
				event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			login(true, getEmail(), getPassword());
			return true;
		}

		return false;
	}
}