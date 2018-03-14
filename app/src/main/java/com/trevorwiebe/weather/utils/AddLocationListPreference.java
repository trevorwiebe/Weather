package com.trevorwiebe.weather.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.trevorwiebe.weather.R;

/**
 * Created by thisi on 3/11/2018.
 */

public class AddLocationListPreference extends ListPreference {

    private static final String TAG = "AddLocationListPreferen";

    private MaterialDialog.Builder mEditLocationDialog;

    public AddLocationListPreference(Context context) {
        super(context);
    }

    public AddLocationListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void showDialog(Bundle state) {
        mEditLocationDialog = new MaterialDialog.Builder(getContext());
        mEditLocationDialog.title(getDialogTitle());

        mEditLocationDialog.neutralText("Edit Locations");
        mEditLocationDialog.onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

            }
        });

        mEditLocationDialog.negativeText("New Location");
        mEditLocationDialog.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                final AlertDialog.Builder addNewLocationBuilder = new AlertDialog.Builder(getContext());
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
                    }
                });
                View addLocationView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_location_edit_text, null);
                addNewLocationBuilder.setView(addLocationView);
                EditText addLocationEditText = addLocationView.findViewById(R.id.add_location_edit_text);
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

        mEditLocationDialog.items(getEntries());
        mEditLocationDialog.itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();

                if (which >= 0 && getEntryValues() != null) {
                    String value = getEntryValues()[which].toString();
                    if (callChangeListener(value))
                        setValue(value);
                }
                return true;
            }
        });
        mEditLocationDialog.show();
    }
}
