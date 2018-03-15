package com.trevorwiebe.weather.database;

import android.provider.BaseColumns;

/**
 * Created by thisi on 3/14/2018.
 */

public class WeatherContract {

    public static class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "location_entries";
        public static final String DISPLAY_LOCATION = "display_location";
        public static final String LOAD_LOCATION = "load_location";
    }
}
