package com.nabto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

public class Bookmarks {
    private static final String BOOKMARKS_PREF_NAME = "bookmarks";

    @SuppressWarnings("unchecked")
    public synchronized static List<Bookmark> getBookmarks(Context context) {
        SharedPreferences preferences = getSharedPreferences(context);
        Map<String, String> all = (Map<String, String>) preferences.getAll();

        ArrayList<Bookmark> bookmarks = new ArrayList<Bookmark>();

        for (String title : all.keySet()) {
            String url = all.get(title);
            bookmarks.add(new Bookmark(title, url));
        }

        return bookmarks;
    }

    public synchronized static void addBookmark(Bookmark bookmark,
            Context context) {
    	
        // TODO check if valid URL and title
        // TODO check if there is already a bookmark with that title
    	
        SharedPreferences preferences = getSharedPreferences(context);
        preferences.edit().putString(bookmark.title, bookmark.url).commit();
    }

    public synchronized static void removeBookmark(Bookmark bookmark,
            Context context) {
        SharedPreferences preferences = getSharedPreferences(context);
        preferences.edit().remove(bookmark.title).commit();
    }

    private synchronized static SharedPreferences getSharedPreferences(
            Context context) {
        return context.getSharedPreferences(BOOKMARKS_PREF_NAME,
                Context.MODE_PRIVATE);
    }
}
