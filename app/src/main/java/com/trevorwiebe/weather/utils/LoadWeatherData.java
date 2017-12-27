package com.trevorwiebe.weather.utils;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by thisi on 12/26/2017.
 */

public class LoadWeatherData extends AsyncTask<String, Void, String> {

    private OnWeatherLoadFinished mOnWeatherLoadFinished;

    public interface OnWeatherLoadFinished {
        void loadFinished(String rawData);
    }

    public LoadWeatherData(OnWeatherLoadFinished onWeatherLoadFinished) {
        mOnWeatherLoadFinished = onWeatherLoadFinished;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            String url = strings[0];
            URL url_obj = new URL(url);

            HttpURLConnection connection = (HttpURLConnection) url_obj.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = reader.readLine()) != null) {
                    response.append(inputLine);
                }
                reader.close();

                return response.toString();
            } else {
                // Error
                return null;
            }
        } catch (Exception e) {
            // Error
            return null;
        }
    }

    @Override
    protected void onPostExecute(String rawWeatherData) {
        mOnWeatherLoadFinished.loadFinished(rawWeatherData);
    }
}
