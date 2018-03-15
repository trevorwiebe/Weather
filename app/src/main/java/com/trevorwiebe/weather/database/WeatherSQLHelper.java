package com.trevorwiebe.weather.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by thisi on 3/14/2018.
 */

public class WeatherSQLHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "simple_weather.db";

    private static final String CREATE_TABLE_LOCATION =
            "CREATE TABLE " + WeatherContract.LocationEntry.TABLE_NAME + " (" +
                    WeatherContract.LocationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    WeatherContract.LocationEntry.DISPLAY_LOCATION + " TEXT," +
                    WeatherContract.LocationEntry.LOAD_LOCATION + " TEXT)";

    public WeatherSQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_LOCATION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + WeatherContract.LocationEntry.TABLE_NAME);
    }
}
