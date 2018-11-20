package com.nabto;

import java.util.List;

import com.nabto.nabtovideo.R;

/* This is to allow usage of the R class wherever it is generated */

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class BookmarksActivity extends FragmentActivity {
    // public static FakeDanfoss unused_danfoss;
    // public static FakeNabtoBrowser unused_nabto_browser;

    public static final String RESULT_NAME = "result";
    private static final String DELETE_BOOKMARK = "Delete";
    private ListView bookmarksList;
    private List<Bookmark> bookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookmarks);

        bookmarksList = (ListView) findViewById(R.id.bookmarksList);

        bookmarksList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                finishWithResult(bookmarks.get(position).url);
            }
        });

        registerForContextMenu(bookmarksList);
        updateBookmarksDisplay();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == android.R.id.home) {
    		this.finish();
    	}
    	return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("Bookmark options");
        menu.add(DELETE_BOOKMARK);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        if (item.getTitle().equals(DELETE_BOOKMARK)) {
            Bookmarks.removeBookmark(bookmarks.get(position), this);
            updateBookmarksDisplay();
            return true;
        }

        throw new IllegalStateException("Does not know about function: "
                + item.getTitle());
    }

    private void finishWithResult(String url) {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_NAME, url);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void updateBookmarksDisplay() {
        bookmarks = Bookmarks.getBookmarks(this);
        bookmarksList.setAdapter(new ArrayAdapter<Bookmark>(this,
                R.layout.bookmarkitem, bookmarks) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                            .inflate(R.layout.bookmarkitem, null);
                } else {
                    row = convertView;
                }

                TextView tv = (TextView) row.findViewById(R.id.bookmarkTitle);
                tv.setText(getItem(position).title);

                return row;
            }
        });
    }
}
