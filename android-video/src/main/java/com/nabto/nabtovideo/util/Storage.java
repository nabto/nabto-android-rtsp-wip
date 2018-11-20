package com.nabto.nabtovideo.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;

// Inspired by http://androidopentutorials.com/android-how-to-store-list-of-values-in-sharedpreferences/

public class Storage {
	
	public static final String PREFS_NAME = "NABTO_VIDEO";
	public static final String FAVORITES = "VIDEO_FAVORITES";

    public Storage() {
		super();
	}

	public void saveFavorites(Context context, List<VideoDevice> favorites) {
		SharedPreferences settings;
		Editor editor;

		settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		editor = settings.edit();

		Gson gson = new Gson();
		String jsonFavorites = gson.toJson(favorites);

		editor.putString(FAVORITES, jsonFavorites);

		editor.commit();
	}

	public void addFavorite(Context context, VideoDevice product) {
        ArrayList<VideoDevice> newFavorites = new ArrayList<VideoDevice>();
        for (VideoDevice device : getFavorites(context)) {
            if (!device.name.equalsIgnoreCase(product.name)) {
                newFavorites.add(device);
            }
        }

		newFavorites.add(product);
		saveFavorites(context, newFavorites);
	}

	public void removeFavorite(Context context, VideoDevice product) {
        ArrayList<VideoDevice> newFavorites = new ArrayList<VideoDevice>();
        for (VideoDevice device : getFavorites(context)) {
            if (!device.name.equalsIgnoreCase(product.name)) {
                newFavorites.add(device);
            }
        }
        saveFavorites(context, newFavorites);
	}

	public ArrayList<VideoDevice> getFavorites(Context context) {
		SharedPreferences settings;
		List<VideoDevice> favorites;

		settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

		if (settings.contains(FAVORITES)) {
			String jsonFavorites = settings.getString(FAVORITES, null);
			Gson gson = new Gson();
			VideoDevice[] favoriteItems = gson.fromJson(jsonFavorites, VideoDevice[].class);

			favorites = Arrays.asList(favoriteItems);
			favorites = new ArrayList<VideoDevice>(favorites);
		}
        else {
            return new ArrayList<VideoDevice>();
		}

		return (ArrayList<VideoDevice>) favorites;
	}

    public void addDeviceFromUriString(Context context, String urlString) {
        Log.d(this.getClass().getSimpleName(), "Populating device from URI: " + urlString);

        Uri uri = Uri.parse(urlString);
        String title = uri.getQueryParameter("title");
        String name = uri.getQueryParameter("name");
        String port = uri.getQueryParameter("port");
        String url = uri.getQueryParameter("url");
        String type = uri.getQueryParameter("type");
        String category = uri.getQueryParameter("category");
        String host = uri.getQueryParameter("host");

        int portInt = 0;
        if (port != null && !port.equals("")) {
            portInt = Integer.parseInt(port);
        }

        int typeInt = 0;
        if (type != null && !type.equals("")) {
            typeInt = Integer.parseInt(type);
        }

        int categoryInt = 0;
        if (category != null && !category.equals("")) {
            categoryInt = Integer.parseInt(category);
        }

        if (host == null) {
            host = "127.0.0.1";
        }

        VideoDevice device = new VideoDevice(title, name, url, portInt, typeInt, categoryInt, host);
        addFavorite(context, device);
    }
}
