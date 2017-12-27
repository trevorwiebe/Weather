package com.trevorwiebe.weather.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.trevorwiebe.weather.R;
import com.trevorwiebe.weather.utils.LoadWeatherData;
import com.trevorwiebe.weather.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements LoadWeatherData.OnWeatherLoadFinished {

    private static final String TAG = "MainActivity";

    // Widgets
    private FrameLayout mBaseLayout;
    private TextView mCurrentTemp;
    private TextView mCurrentCondition;
    private TextView mWind;
    private TextView mLastUpdated;
    private ProgressBar mLoadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBaseLayout = findViewById(R.id.main_layout);
        mCurrentTemp = findViewById(R.id.current_temp);
        mCurrentCondition = findViewById(R.id.current_condition);
        mWind = findViewById(R.id.wind_conditions);
        mLastUpdated = findViewById(R.id.last_updated);
        mLoadingIndicator = findViewById(R.id.loading_indicator);

        new LoadWeatherData(MainActivity.this).execute(Utility.BASE_URL);

        setBackgroundColors(getResources().getColor(R.color.loading_color));
        setTextColor(getResources().getColor(R.color.loading_color));

    }

    @Override
    public void loadFinished(String rawData) {
        HashMap<String, String> weatherHashMap = parseData(rawData);
        mCurrentTemp.setText(weatherHashMap.get("temp_f") + (char) 0x00B0 + "F");
        mCurrentCondition.append(" " + weatherHashMap.get("weather"));
        mWind.append(" " + weatherHashMap.get("wind_mph") + " mph from the " + weatherHashMap.get("wind_dir"));
        mLastUpdated.setText(weatherHashMap.get("last_updated"));

        float temp = Float.parseFloat(weatherHashMap.get("temp_f"));
        int backgroundColor = evaluateBackgroundColor(temp);
        setBackgroundColors(backgroundColor);

        // set the text color
        int textColor = getResources().getColor(R.color.white);
        setTextColor(textColor);

        // hide the loading indicator
        mLoadingIndicator.setVisibility(View.INVISIBLE);
    }

    private HashMap<String, String> parseData(String rawData) {
        HashMap<String, String> weather_map = new HashMap<>();
        try {
            JSONObject baseJsonObject = new JSONObject(rawData);
            JSONObject currentObservationObject = baseJsonObject.getJSONObject("current_observation");
            weather_map.put("weather", currentObservationObject.getString("weather"));
            weather_map.put("temp_f", currentObservationObject.getString("temp_f"));
            weather_map.put("wind_dir", currentObservationObject.getString("wind_dir"));
            weather_map.put("wind_mph", currentObservationObject.getString("wind_mph"));
            weather_map.put("last_updated", currentObservationObject.getString("observation_time"));
            return weather_map;
        } catch (JSONException e) {
            Log.d(TAG, "parseData: returning null " + e);
            return null;
        }
    }

    private int evaluateBackgroundColor(float temp) {
        int blue = getResources().getColor(R.color.blue);
        int red = getResources().getColor(R.color.red);
        float colorRatio;
        if (temp < 0) {
            colorRatio = 1f;
        } else if (temp < 10) {
            colorRatio = 0.9f;
        } else if (temp < 20) {
            colorRatio = 0.8f;
        } else if (temp < 30) {
            colorRatio = 0.7f;
        } else if (temp < 40) {
            colorRatio = 0.6f;
        } else if (temp < 50) {
            colorRatio = 0.5f;
        } else if (temp < 60) {
            colorRatio = 0.4f;
        } else if (temp < 70) {
            colorRatio = 0.3f;
        } else if (temp < 80) {
            colorRatio = 0.2f;
        } else if (temp < 90) {
            colorRatio = 0.1f;
        } else if (temp < 100) {
            colorRatio = 0.0f;
        } else {
            colorRatio = 0.5f;
        }
        return ColorUtils.blendARGB(red, blue, colorRatio);
    }

    private void setBackgroundColors(int color) {
        ObjectAnimator colorFade = ObjectAnimator.ofObject(mBaseLayout, "backgroundColor", new ArgbEvaluator(), getResources().getColor(R.color.loading_color), color);
        colorFade.setDuration(1500);
        colorFade.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
            ObjectAnimator notiFade = ObjectAnimator.ofObject(window, "StatusBarColor", new ArgbEvaluator(), getResources().getColor(R.color.loading_color), color);
            notiFade.setDuration(1500);
            notiFade.start();
        }
    }

    private void setTextColor(int color) {
        mCurrentTemp.setTextColor(color);
        mCurrentCondition.setTextColor(color);
        mWind.setTextColor(color);
        mLastUpdated.setTextColor(color);
    }

}
