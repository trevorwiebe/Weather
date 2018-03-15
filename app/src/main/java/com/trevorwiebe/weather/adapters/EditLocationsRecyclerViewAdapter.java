package com.trevorwiebe.weather.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.trevorwiebe.weather.R;
import com.trevorwiebe.weather.database.WeatherContract;

/**
 * Created by thisi on 3/14/2018.
 */

public class EditLocationsRecyclerViewAdapter extends RecyclerView.Adapter<EditLocationsRecyclerViewAdapter.EditLocationsViewHolder> {

    private Cursor mLocationsCursor;
    private Context mContext;

    public EditLocationsRecyclerViewAdapter(Context context, Cursor locationsCursor) {
        mContext = context;
        mLocationsCursor = locationsCursor;
    }

    @Override
    public int getItemCount() {
        if (mLocationsCursor == null) return 0;
        return mLocationsCursor.getCount();
    }

    @NonNull
    @Override
    public EditLocationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_edit_locations, parent, false);
        return new EditLocationsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EditLocationsViewHolder holder, int position) {
        mLocationsCursor.moveToPosition(position);

        String location = mLocationsCursor.getString(mLocationsCursor.getColumnIndex(WeatherContract.LocationEntry.DISPLAY_LOCATION));
        int id = mLocationsCursor.getInt(mLocationsCursor.getColumnIndex(WeatherContract.LocationEntry._ID));
        holder.mLocationTv.setText(location);
        holder.mLocationTv.setTag(id);

    }

    public void swapData(Cursor newLocationCursor) {
        mLocationsCursor = newLocationCursor;
        if (mLocationsCursor != null) {
            notifyDataSetChanged();
        }
    }

    public class EditLocationsViewHolder extends RecyclerView.ViewHolder {

        private TextView mLocationTv;

        public EditLocationsViewHolder(View view) {
            super(view);
            mLocationTv = view.findViewById(R.id.edit_location_tv);
        }
    }
}
