package com.nabto;

import java.util.ArrayList;

import android.os.Bundle;

public class History {
    private ArrayList<String> urlHistory = new ArrayList<String>();
    private int currentIndex = -1;

    private static final String HISTORY_URL_LIST_KEY = "HISTORY_URL_LIST_KEY";
    private static final String HISTORY_CURRENT_INDEX_KEY = "HISTORY_CURRENT_INDEX_KEY";

    public void load(Bundle savedInstance) {
        urlHistory = savedInstance.getStringArrayList(HISTORY_URL_LIST_KEY);
        currentIndex = savedInstance.getInt(HISTORY_CURRENT_INDEX_KEY);
    }

    public void save(Bundle savedInstance) {
        savedInstance.putStringArrayList(HISTORY_URL_LIST_KEY, urlHistory);
        savedInstance.putInt(HISTORY_CURRENT_INDEX_KEY, currentIndex);
    }

    public void navigateTo(String url) {
        // If the same URL is fetched, no need to update history
        if (currentIndex != -1 && urlHistory.get(currentIndex).equals(url)) {
            return;
        }

        // Delete forward history
        currentIndex++;
        while (currentIndex < urlHistory.size()) {
            urlHistory.remove(urlHistory.size() - 1);
        }
        urlHistory.add(url);
    }

    public String goBack() {
        currentIndex--;
        return urlHistory.get(currentIndex);

    }

    public boolean canGoBack() {
        return currentIndex - 1 >= 0;
    }

    public String goForward() {
        currentIndex++;
        return urlHistory.get(currentIndex);
    }

    public boolean canGoForward() {
        return currentIndex + 1 < urlHistory.size();
    }
}
