package com.trevorwiebe.weather.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;


public class Utility {

    // Base Url
    public static final String BASE_URL = "http://api.wunderground.com/api/d2ca143a09cfc813/conditions/q/";

    // All possible errors that can happen when fetching data
    public static final int NO_DATA_RETURNED = 0;
    public static final int NEED_LOCATION_PERMISSION = 1;
    public static final int JSON_PARSING_FAILED = 2;
    public static final int NO_INTERNET_CONNECTION = 3;
    public static final int LOCATIONS_NOT_TURNED_ON = 4;
    public static final int FAILED_TO_GET_LOCATION = 5;
    public static final int LOCATION_NOT_RECOGNIZED = 6;

    // Unit settings strings
    public static final String AUTOMATIC = "Automatic";
    public static final String IMPERIAL = "Imperial";
    public static final String CELSIUS = "Metric";

    // Unit types that need to be converted
    public static final int TEMPERATURE = 7;
    public static final int DISTANCE = 8;
    public static final int LENGTH = 9;

    // Check if is connected to the internet
    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    // Check if locations is enabled
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

}
