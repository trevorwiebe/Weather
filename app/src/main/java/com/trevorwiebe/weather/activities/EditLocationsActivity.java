package com.trevorwiebe.weather.activities;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.trevorwiebe.weather.R;
import com.trevorwiebe.weather.adapters.EditLocationsRecyclerViewAdapter;
import com.trevorwiebe.weather.database.WeatherContract;
import com.trevorwiebe.weather.database.WeatherSQLHelper;
import com.trevorwiebe.weather.utils.ItemClickListener;

public class EditLocationsActivity extends AppCompatActivity {

    private static final String TAG = "EditLocationsActivity";

    private SQLiteDatabase mDatabase;
    private String mSelectedId;

    private TextView mNoLocationsTv;
    private RecyclerView mLocationsRv;
    private EditLocationsRecyclerViewAdapter mEditLocationsRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_locations);

        mDatabase = new WeatherSQLHelper(this).getReadableDatabase();

        mNoLocationsTv = findViewById(R.id.no_locations_tv);

        mLocationsRv = findViewById(R.id.edit_locations_rv);
        mLocationsRv.setLayoutManager(new LinearLayoutManager(this));
        mEditLocationsRecyclerViewAdapter = new EditLocationsRecyclerViewAdapter(this, null);
        mLocationsRv.setAdapter(mEditLocationsRecyclerViewAdapter);
        mLocationsRv.addOnItemTouchListener(new ItemClickListener(this, mLocationsRv, new ItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                AlertDialog.Builder editLocation = new AlertDialog.Builder(EditLocationsActivity.this);
                editLocation.setTitle(getResources().getString(R.string.edit_location));
                View editLocationView = LayoutInflater.from(EditLocationsActivity.this).inflate(R.layout.dialog_edit_text, null);
                editLocation.setView(editLocationView);
                final EditText editLocationEditText = editLocationView.findViewById(R.id.add_location_edit_text);
                TextView selectedLocation = (TextView) view;
                mSelectedId = selectedLocation.getTag().toString();
                String textToEdit = getSelectedText(mSelectedId);
                editLocationEditText.setText(textToEdit);
                editLocationEditText.setSelection(textToEdit.length());
                editLocation.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                editLocation.setPositiveButton(getResources().getString(R.string.update), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String updatedLocation = editLocationEditText.getText().toString();
                        updateLocation(updatedLocation, mSelectedId);
                        refreshView();
                    }
                });
                editLocation.setNeutralButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSelectedLocation(mSelectedId);
                        refreshView();
                    }
                });
                editLocation.show();
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        refreshView();

    }

    @Override
    protected void onDestroy() {
        mDatabase.close();
        super.onDestroy();
    }

    private Cursor getLocations() {
        return mDatabase.query(
                WeatherContract.LocationEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String getSelectedText(String id) {
        Cursor cursor = mDatabase.query(WeatherContract.LocationEntry.TABLE_NAME,
                new String[]{WeatherContract.LocationEntry.DISPLAY_LOCATION},
                WeatherContract.LocationEntry._ID + " = " + id,
                null,
                null,
                null,
                null);

        cursor.moveToFirst();
        return cursor.getString(cursor.getColumnIndex(WeatherContract.LocationEntry.DISPLAY_LOCATION));
    }

    private void updateLocation(String updatedLocation, String id) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String currentSelection = sharedPreferences.getString(getResources().getString(R.string.location_pref_key), getResources().getString(R.string.location_current_location_label));
        String locationInDb = getSelectedText(id);

        if (locationInDb.equals(currentSelection)) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(getString(R.string.location_pref_key), updatedLocation);
            editor.apply();
        }
        ContentValues cv = new ContentValues();
        cv.put(WeatherContract.LocationEntry.DISPLAY_LOCATION, updatedLocation);
        int numsUpdated = mDatabase.update(WeatherContract.LocationEntry.TABLE_NAME, cv, WeatherContract.LocationEntry._ID + " = " + id, null);
        if (numsUpdated > 0) {
            Toast.makeText(this, getResources().getString(R.string.updated_successfully), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getResources().getString(R.string.not_updated_successfully), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSelectedLocation(String id) {

        String locationDeleted = getSelectedText(id);

        int numDeleted = mDatabase.delete(WeatherContract.LocationEntry.TABLE_NAME, WeatherContract.LocationEntry._ID + " = " + id, null);
        if (numDeleted > 0) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String currentSelection = sharedPreferences.getString(getResources().getString(R.string.location_pref_key), getResources().getString(R.string.location_current_location_label));

            if (locationDeleted.equals(currentSelection)) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(getString(R.string.location_pref_key), getString(R.string.location_current_location_label));
                editor.apply();
            }
            Toast.makeText(this, getResources().getString(R.string.location_deleted_successfully), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getResources().getString(R.string.location_not_deleted), Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshView() {
        if (getLocations().getCount() == 0) {
            mLocationsRv.setVisibility(View.GONE);
            mNoLocationsTv.setVisibility(View.VISIBLE);
        } else {
            mLocationsRv.setVisibility(View.VISIBLE);
            mNoLocationsTv.setVisibility(View.GONE);
            mEditLocationsRecyclerViewAdapter.swapData(getLocations());
        }
    }

}
