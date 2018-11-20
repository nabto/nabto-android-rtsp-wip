package com.nabto;

/* This is to allow usage of the R class wherever it is generated */

import com.nabto.nabtovideo.R;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AddBookmarkActivity extends FragmentActivity {
    // public static FakeDanfoss unused_danfoss;
    // public static FakeNabtoBrowser unused_nabto_browser;
    public static String INPUT_URL = "input-url";
    public static String INPUT_TITLE = "input-title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.addbookmarkdialog);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);
        
        final EditText titleInput = (EditText) findViewById(R.id.titleInput);
        final EditText urlInput = (EditText) findViewById(R.id.urlInput);
        Button cancelButtol = (Button) findViewById(R.id.cancelButton);
        Button saveButtol = (Button) findViewById(R.id.saveButton);

        String titlePassed = getIntent().getExtras().getString(INPUT_TITLE);
        String urlPassed = getIntent().getExtras().getString(INPUT_URL);

        titleInput.setText(titlePassed);
        urlInput.setText(urlPassed);

        cancelButtol.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        saveButtol.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	String title = titleInput.getText().toString();
                String url = urlInput.getText().toString();
                Bookmarks.addBookmark(new Bookmark(title, url), AddBookmarkActivity.this);
                finish();
            }
        });
    }
}
