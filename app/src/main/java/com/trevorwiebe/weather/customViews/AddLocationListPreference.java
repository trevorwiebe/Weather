package com.trevorwiebe.weather.customViews;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.trevorwiebe.weather.R;
import com.trevorwiebe.weather.activities.EditLocationsActivity;
import com.trevorwiebe.weather.database.WeatherContract;
import com.trevorwiebe.weather.database.WeatherSQLHelper;

import java.util.ArrayList;

/**
 * Created by thisi on 3/11/2018.
 */

public class AddLocationListPreference extends ListPreference {

    private static final String TAG = "AddLocationListPreferen";

    private WeatherSQLHelper mWeatherHelper = new WeatherSQLHelper(getContext());
    private ArrayList<String> mLocationArr = new ArrayList<>();

    public AddLocationListPreference(Context context) {
        super(context);
    }

    public AddLocationListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void showDialog(Bundle state) {
        MaterialDialog.Builder editLocationDialog = new MaterialDialog.Builder(getContext());
        editLocationDialog.title(getDialogTitle());

        editLocationDialog.neutralText("Edit Locations");
        editLocationDialog.onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                Intent editLocationIntent = new Intent(getContext(), EditLocationsActivity.class);
                getContext().startActivity(editLocationIntent);
            }
        });

        editLocationDialog.negativeText("New Location");
        editLocationDialog.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                final AlertDialog.Builder addNewLocationBuilder = new AlertDialog.Builder(getContext());
                View addLocationView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_text, null);
                final EditText addLocationEditText = addLocationView.findViewById(R.id.add_location_edit_text);
                addNewLocationBuilder.setTitle(getContext().getResources().getString(R.string.add_new_location_title));
                addNewLocationBuilder.setMessage(getContext().getResources().getString(R.string.add_new_location_summary));
                addNewLocationBuilder.setNegativeButton(getContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                addNewLocationBuilder.setPositiveButton(getContext().getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String location = addLocationEditText.getText().toString();
                        setValue(location);
                        saveLocationData(location);
                    }
                });
                addNewLocationBuilder.setView(addLocationView);
                final AlertDialog editBtnsDialog = addNewLocationBuilder.create();
                addLocationEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        int charCount = s.length();
                        if (charCount > 3) {
                            Button positiveBtn = editBtnsDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            if (positiveBtn != null) {
                                positiveBtn.setEnabled(true);
                            }
                        } else {
                            Button positiveBtn = editBtnsDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            if (positiveBtn != null) {
                                positiveBtn.setEnabled(false);
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                editBtnsDialog.show();
                Button positiveBtn = editBtnsDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (positiveBtn != null) {
                    positiveBtn.setEnabled(false);
                }
            }
        });

        editLocationDialog.items(getLocationEntries());
        editLocationDialog.itemsCallbackSingleChoice(getIndexOfSetLocation(), new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();

                if (which >= 0) {
                    String value = getLocationEntries()[which].toString();
                    if (callChangeListener(value))
                        setValue(value);
                }
                return true;
            }
        });
        editLocationDialog.show();
    }


    private void saveLocationData(String location) {

        String loadableLocation = location.replace(" ", ",");

        SQLiteDatabase database = mWeatherHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(WeatherContract.LocationEntry.DISPLAY_LOCATION, location);
        cv.put(WeatherContract.LocationEntry.LOAD_LOCATION, loadableLocation);

        database.insert(WeatherContract.LocationEntry.TABLE_NAME, null, cv);

    }

    private CharSequence[] getLocationEntries() {

        if (mLocationArr.size() == 0) {
            mLocationArr.add("Current Location");

            SQLiteDatabase database = mWeatherHelper.getReadableDatabase();
            Cursor locationCursor = database.query(WeatherContract.LocationEntry.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            locationCursor.moveToFirst();
            while (locationCursor.moveToNext()) {
                String location = locationCursor.getString(locationCursor.getColumnIndex(WeatherContract.LocationEntry.DISPLAY_LOCATION));
                mLocationArr.add(location);
            }
            locationCursor.close();
        }
        return mLocationArr.toArray(new CharSequence[mLocationArr.size()]);
    }

    private int getIndexOfSetLocation() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String currentSelection = sharedPreferences.getString(getContext().getString(R.string.location_pref_key), getContext().getResources().getString(R.string.location_current_location_value));

        getLocationEntries();

        return mLocationArr.indexOf(currentSelection);
    }
}
